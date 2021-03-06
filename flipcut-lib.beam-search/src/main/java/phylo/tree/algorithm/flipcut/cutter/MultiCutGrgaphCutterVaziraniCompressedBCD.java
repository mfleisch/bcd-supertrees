package phylo.tree.algorithm.flipcut.cutter;

import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import mincut.cutGraphAPI.bipartition.CompressedBCDMultiCut;
import mincut.cutGraphAPI.bipartition.MultiCut;
import mincut.cutGraphAPI.bipartition.VaziraniCut;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.CutGraphImpl;
import mincut.cutGraphImpl.maxFlowGoldbergTarjan.Node;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDMultiCutGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphTypes;

import java.util.*;
import java.util.concurrent.ExecutorService;

public class MultiCutGrgaphCutterVaziraniCompressedBCD extends AbstractMultiCutGraphCutterVazirani<RoaringBitmap, CompressedBCDMultiCutGraph> {

    int[] taxa;
    RoaringBitmap taxaAsMap;


    public MultiCutGrgaphCutterVaziraniCompressedBCD(CompressedBCDMultiCutGraph graphToCut) {
        super(graphToCut);
    }

    //creates a cutset, that contains taxa an characters to delete
    //todo, this is ugly, do something nice
    private RoaringBitmap buildCutSet(CutGraphImpl hipri, CompressedBCDGraph source, TIntObjectMap<TIntList> charMapping, RoaringBitmap sourceSetTaxa, RoaringBitmap sinkSetTaxa) {
        RoaringBitmap sourceChars = new RoaringBitmap();

        for (Node node : hipri.getNodes()) {
            int nodeIndex = ((Node.IntNode) node).getIntName();
            if (hipri.isInSourceSet(node)) {
                if (!source.isTaxon(nodeIndex)) {
                    sourceChars.add(nodeIndex);
                } else {
                    sourceSetTaxa.add(nodeIndex);
                }
            }
        }


        RoaringBitmap sourceSet = collectCharsToRemove(sourceChars, source, charMapping);
        sourceSetTaxa.and(taxaAsMap);
        sourceSet.or(sourceSetTaxa);

        return sourceSet;
    }

    private RoaringBitmap collectCharsToRemove(RoaringBitmap cutSetChars, CompressedBCDGraph source, TIntObjectMap<TIntList> charMapping) {
        RoaringBitmap cutSet = new RoaringBitmap();
        cutSetChars.forEach((IntConsumer) value -> {
            if (source.isCharacter(value)) {
                if (!cutSetChars.contains(source.getCloneIndex(value))) {
                    TIntList map = charMapping.get(value);
                    if (map != null)
                        cutSet.add(map.toArray());
                    else
                        cutSet.add(value);
                }
            } else if (source.isCharacterClone(value)) {
                int charIndex = source.getCharIndex(value);
                if (!cutSetChars.contains(charIndex)) {
                    TIntList map = charMapping.get(charIndex);
                    if (map != null)
                        cutSet.add(map.toArray());
                    else
                        cutSet.add(charIndex);
                }
            }
        });

        return cutSet;
    }

    private List<RoaringBitmap> mergeGuideEdgesWithTaxonSet(final List<RoaringBitmap> guideEdges, final RoaringBitmap taxonSet) {
        //merge taxa with giude edge if they do overlap
        Iterator<RoaringBitmap> it = guideEdges.iterator();
        while (it.hasNext()) {
            RoaringBitmap guideEdge = it.next();
            if (RoaringBitmap.intersects(taxonSet, guideEdge)) {
                taxonSet.or(guideEdge);
                it.remove();
            }
        }
        guideEdges.add(taxonSet); //add (merged) taxa to set og guide edges
        return guideEdges;
    }

    @Override
    protected void initialCut() {
        // get the mincut, fix s iterate over t
        VaziraniCut<RoaringBitmap> currentNode;
        VaziraniCut<RoaringBitmap> lightestCut;

        // inti data structures
        TIntObjectMap<TIntList> charMapping = new TIntObjectHashMap<>();
        TIntObjectMap<Node.IntNode> cutgraphTaxa = new TIntObjectHashMap<>(source.numTaxa());

        List<RoaringBitmap> guideEdges = CompressedSingleCutter.createGuideEdges(source.getSource());
        CutGraphImpl hipri = CompressedSingleCutter.createHipri(
                source.getSource(), guideEdges, charMapping, cutgraphTaxa
        );

        taxa = cutgraphTaxa.keys();
        Arrays.sort(taxa);
        taxaAsMap = RoaringBitmap.bitmapOf(taxa);
        initCuts = new VaziraniCut[taxa.length - 1];
        queueAscHEAP = new PriorityQueue<>();


        RoaringBitmap sSet = RoaringBitmap.bitmapOf(taxa[0]);
        RoaringBitmap tSet = RoaringBitmap.bitmapOf(taxa[1]);

        //j=0
        hipri.setSource(cutgraphTaxa.get(sSet.first()));
        hipri.setSink(cutgraphTaxa.get(tSet.first()));
        hipri.calculateMaxFlow(false);

        long cutValue = hipri.getValue();

        //createCutset
        RoaringBitmap cutSet = buildCutSet(hipri, source.getSource(), charMapping, sSet, tSet);

        //here the cutset is the set of characters to delete
        lightestCut = new VaziraniCut<>(cutSet, cutValue, 1);
        initCuts[0] = lightestCut;

        //ATTENTION this is  the undirected graph version as tweak for symmetric bcd graph
        for (int j = 1; j < taxa.length - 1; j++) {
            //build cutgraph from patial cut
            //merge taxa together for a partial cut
            sSet = new RoaringBitmap();
            tSet = RoaringBitmap.bitmapOf(taxa[j + 1]);
            for (int i = 0; i <= j; i++) {
                sSet.add(taxa[i]);
            }

            //add merged taxa as a guide tree
            guideEdges = mergeGuideEdgesWithTaxonSet(
                    CompressedSingleCutter.createGuideEdges(source.getSource()), sSet
            );

            //create graph: add characters and remaining taxa to cutgraph and merge everything that has to
            charMapping.clear();
            cutgraphTaxa.clear();
            hipri = CompressedSingleCutter.createHipri(source.getSource(), guideEdges, charMapping, cutgraphTaxa);

            //calculate partial cut
            hipri.setSource(cutgraphTaxa.get(sSet.first())); // taxa 0 to j are in sset and the lowest taxon ist returned after merging. so we use sset.first instead
            hipri.setSink(cutgraphTaxa.get(tSet.first()));
            hipri.calculateMaxFlow(false);

            cutValue = hipri.getValue();
            cutSet = buildCutSet(hipri, source.getSource(), charMapping, sSet, tSet);

            currentNode = new VaziraniCut<>(cutSet, cutValue, 1);
            initCuts[j] = currentNode;
            //save lightest cut for HEAP init
            if (currentNode.minCutValue() < lightestCut.minCutValue()) lightestCut = currentNode;
        }
        //initialize heap
        VaziraniCut<RoaringBitmap> initialToHeap = new VaziraniCut<>(lightestCut.getCutSet(), lightestCut.minCutValue(), lightestCut.k()); //todo why new node?
        queueAscHEAP.add(initialToHeap);
    }


    @Override
    protected List<VaziraniCut<RoaringBitmap>> findCutsFromPartialCuts(VaziraniCut<RoaringBitmap> sourceCut, VaziraniCut<RoaringBitmap>[] initCuts) {
        RoaringBitmap cut = sourceCut.getCutSet();
        List<VaziraniCut<RoaringBitmap>> cuts = new ArrayList<>(taxa.length - sourceCut.k());// todo is the size correct?

        // finding all partial mincut
        for (int k = sourceCut.k(); k < taxa.length; k++) {
            RoaringBitmap sSet = new RoaringBitmap();
            RoaringBitmap tSet = new RoaringBitmap();


            int taxon;
            for (int i = 0; i < k; i++) {
                //todo maybe use Bitmap operations here
                taxon = taxa[i];
                if (cut.contains(taxon)) {
                    sSet.add(taxon);
                } else {
                    tSet.add(taxon);
                }
            }

            //change position of taxon number k
            taxon = taxa[k];
            if (!cut.contains(taxon)) {
                sSet.add(taxon);
            } else {
                tSet.add(taxon);
            }

            if (!tSet.isEmpty()) {
                //build guide tree edgeSet and merge it with s and t set if nessecary
                List<RoaringBitmap> guideEdges = CompressedSingleCutter.createGuideEdges(source.getSource());

                guideEdges = mergeGuideEdgesWithTaxonSet(guideEdges, sSet);
                guideEdges = mergeGuideEdgesWithTaxonSet(guideEdges, tSet);

                TIntObjectMap<TIntList> charMapping = new TIntObjectHashMap<>();
                TIntObjectMap<Node.IntNode> cutgraphTaxa = new TIntObjectHashMap<>(source.numTaxa());

                CutGraphImpl hipri = CompressedSingleCutter.createHipri(source.getSource(), guideEdges, charMapping, cutgraphTaxa);

                hipri.setSource(cutgraphTaxa.get(sSet.first()));
                hipri.setSink(cutgraphTaxa.get(tSet.first()));

                hipri.calculateMaxFlow(false);

                long cutValue = hipri.getValue();
                RoaringBitmap cutSet = buildCutSet(hipri, source.getSource(), charMapping, sSet, tSet);

                VaziraniCut<RoaringBitmap> currentCut = new VaziraniCut<>(cutSet, cutValue, k + 1);
                cuts.add(currentCut);

            } else if (sSet.getCardinality() < taxa.length) {
                //find cut for 0^k case
                //tSet empty --> adding new init Graph! 0^k case);
                VaziraniCut<RoaringBitmap> initCut = null;
                //find best
                for (int i = k; i < initCuts.length; i++) {
                    if (initCut == null || initCuts[i].minCutValue() < initCut.minCutValue()) {
                        initCut = initCuts[i];
                    }
                }
                //copy to new object
                initCut = new VaziraniCut<>(initCut.getCutSet(), initCut.minCutValue(), k + 1);
                cuts.add(initCut);
            }
        }
        return cuts;
    }


    @Override
    protected MultiCut<RoaringBitmap, CompressedBCDMultiCutGraph> buildOutputCut(VaziraniCut<RoaringBitmap> currentNode) {
        //remove taxa indeces from cutset
        currentNode.cutSet.and(source.getSource().characters);
        return new CompressedBCDMultiCut(currentNode.cutSet, currentNode.minCutValue, source);
    }

    @Override
    public boolean isBCD() {
        return true;
    }

    public static class Factory implements MultiCutterFactory<MultiCutGrgaphCutterVaziraniCompressedBCD, RoaringBitmap, CompressedBCDMultiCutGraph>, MaxFlowCutterFactory<MultiCutGrgaphCutterVaziraniCompressedBCD, RoaringBitmap, CompressedBCDMultiCutGraph> {


        @Override
        public MultiCutGrgaphCutterVaziraniCompressedBCD newInstance(CompressedBCDMultiCutGraph graph) {
            return new MultiCutGrgaphCutterVaziraniCompressedBCD(graph);
        }

        @Override
        public MultiCutGrgaphCutterVaziraniCompressedBCD newInstance(CompressedBCDMultiCutGraph graph, ExecutorService executorService, int threads) {
            return new MultiCutGrgaphCutterVaziraniCompressedBCD(graph); //todo multi multi threading
        }

        @Override
        public CutGraphTypes getType() {
            return CutGraphTypes.COMPRESSED_BCD_VIA_MAXFLOW_TARJAN_GOLDBERG;
        }

        @Override
        public boolean isBCD() {
            return true;
        }
    }
}
