package phylo.tree.algorithm.flipcut.cutter;

import mincut.cutGraphAPI.GoldbergTarjanCutGraph;
import mincut.cutGraphAPI.bipartition.*;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphTypes;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 14:05
 */

/**
 * VAZIRANI ALGORITHM
 */

public class MultiCutGraphCutterVazirani extends AbstractMultiCutGraphCutterVazirani<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> {

    private ArrayList<FlipCutNodeSimpleWeight> taxa;
    private VertexMapping<FlipCutGraphMultiSimpleWeight> mapping = new VertexMapping<>();
    private Map<FlipCutNodeSimpleWeight, Set<FlipCutNodeSimpleWeight>> dummyToMerged;

    private LinkedHashSet<FlipCutNodeSimpleWeight> characters = null;

    @Override
    public void clear() {
        super.clear();
        taxa = null;
        mapping = null;
        dummyToMerged = null;
        characters = null;
    }

    public MultiCutGraphCutterVazirani(FlipCutGraphMultiSimpleWeight graphToCut) {
        super(graphToCut);
    }

    @Override
    protected List<VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>>> findCutsFromPartialCuts(VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>> sourceCut, VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>>[] initCuts) {
        Set<FlipCutNodeSimpleWeight> cutTSet = sourceCut.getCutSet();

        List<VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>>> cuts = new ArrayList<>(taxa.size() - sourceCut.k());

        VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>> currentCut;
        GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight> cutGraph;
        Set<FlipCutNodeSimpleWeight> sSet;
        Set<FlipCutNodeSimpleWeight> tSet;


        // finding all partial mincut
        for (int k = sourceCut.k(); k < taxa.size(); k++) {
//            System.out.println("Taxa number: " + taxa.size() + " - k=" + k);
            sSet = new HashSet<>();
            tSet = new HashSet<>();

            for (int i = 0; i < k; i++) {
                if (cutTSet.contains(taxa.get(i))) {
                    tSet.add(taxa.get(i));
                } else {
                    sSet.add(taxa.get(i));
                }
            }

            //change position of taxon number k
            if (!cutTSet.contains(taxa.get(k))) {
                tSet.add(taxa.get(k));
            } else {
                sSet.add(taxa.get(k));
            }

            //build cutgraph from patial cut
            cutGraph = new GoldbergTarjanCutGraph<>();

            // add characters, character clones and edges between them
            SimpleCutGraphCutter.createGoldbergTarjanCharacterWeights(cutGraph, characters);

            //add taxa > k
            for (int i = k + 1; i < taxa.size(); i++) {
                cutGraph.addNode(taxa.get(i));
            }

            FlipCutNodeSimpleWeight randomS = sSet.iterator().next();
            cutGraph.addNode(randomS);

            if (!tSet.isEmpty()) {

                FlipCutNodeSimpleWeight randomT = tSet.iterator().next();
                cutGraph.addNode(randomT);

                // add edges from character to taxa and merge s and t nodes
                for (FlipCutNodeSimpleWeight character : characters) {
                    long sWeight = 0;
                    long tWeight = 0;
                    for (FlipCutNodeSimpleWeight taxon : character.edges) {
                        long weight = CutGraphCutter.getInfinity();

                        if (sSet.contains(taxon)) {
                            sWeight = CutGraphCutter.getInfinity();
                        } else if (tSet.contains(taxon)) {
                            tWeight = CutGraphCutter.getInfinity();
                        } else {
                            cutGraph.addEdge(character, taxon, weight);
                            cutGraph.addEdge(taxon, character.clone, weight);
                        }
                    }
                    if (sWeight > 0) {
                        cutGraph.addEdge(character, randomS, sWeight);
                        cutGraph.addEdge(randomS, character.clone, sWeight);
                    }
                    if (tWeight > 0) {
                        cutGraph.addEdge(character, randomT, tWeight);
                        cutGraph.addEdge(randomT, character.clone, tWeight);
                    }
                }


                // compute mincut an put it to results
                STCut<FlipCutNodeSimpleWeight> tmpCut = cutGraph.calculateMinSTCut(randomS, randomT);
                LinkedHashSet<FlipCutNodeSimpleWeight> nuCutset = new LinkedHashSet<>(tmpCut.gettSet());
                nuCutset.addAll(tSet);
                currentCut = new VaziraniCut<>(nuCutset, tmpCut.minCutValue(), k + 1);
                cuts.add(currentCut);

            } else if (sSet.size() < taxa.size()) {
                //find cut for 0^k case
                //tSet empty --> adding new init Graph! 0^k case);
                VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>> initCut = null;
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
    protected MultiCut<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> buildOutputCut(VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>> currentNode) {
        BasicCut<FlipCutNodeSimpleWeight> unMapped = mapping.undoMapping(currentNode, dummyToMerged);
        return new DefaultMultiCut(unMapped, source);
    }

    @Override
    protected void initialCut() {
        // get the mincut, fix s iterate over t
        FlipCutNodeSimpleWeight s;
        FlipCutNodeSimpleWeight t;
        VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>> currentNode;
        STCut minCut;
        VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>> lightestCut;

        // inti data structures
        taxa = mapping.createMapping(source);
        initCuts = new VaziraniCut[taxa.size() - 1];
        queueAscHEAP = new PriorityQueue<>();

        //j=0
        GoldbergTarjanCutGraph<FlipCutNodeSimpleWeight> cutGraph = new GoldbergTarjanCutGraph<>();
        dummyToMerged = SimpleCutGraphCutter.createTarjanGoldbergHyperGraphTaxaMerged(cutGraph, source, mapping, new ArrayList<>(taxa.size()));

        minCut = cutGraph.calculateMinSTCut(taxa.get(0), taxa.get(1));
        lightestCut = new VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>>(minCut.gettSet(), minCut.minCutValue(), 1);
        initCuts[0] = lightestCut;


        characters = new LinkedHashSet<>();
        for (Object o : cutGraph.getNodes().keySet()) {
            FlipCutNodeSimpleWeight node = (FlipCutNodeSimpleWeight) o;
            if (!node.isClone() && (node.isCharacter() || node.isDummyCharacter())) {
                characters.add(node);
                if (AbstractFlipCutGraph.SCAFF_TAXA_MERGE) {
                    if (node.edgeWeight == CutGraphCutter.getInfinity())
                        System.out.println("SCM node in graph, but should be merged!");
                }
            }
        }

        for (Set<FlipCutNodeSimpleWeight> simpleWeights : mapping.trivialcharacters.values()) {
            if (characters.removeAll(simpleWeights))
                System.out.println("trivials removed");
        }

        //ATTENTION this is  the undirected graph version as tweak for flipCut Graph
        for (int j = 1; j < taxa.size() - 1; j++) {
            s = taxa.get(j);
            t = taxa.get(j + 1);


            //build cutgraph from patial cut
            cutGraph = new GoldbergTarjanCutGraph<>();
            Set<FlipCutNodeSimpleWeight> sSet = new HashSet<FlipCutNodeSimpleWeight>();
            for (int i = 0; i <= j; i++) {
                sSet.add(taxa.get(i));
            }

            // add characters, character clones and edges between them
            SimpleCutGraphCutter.createGoldbergTarjanCharacterWeights(cutGraph, characters);

            //add taxa
            for (int i = j + 1; i < taxa.size(); i++) {
                cutGraph.addNode(taxa.get(i));
                //System.out.println("adding taxa " + node.name);
            }

            // add edges from character to taxa and merge s and t nodes
            for (FlipCutNodeSimpleWeight character : characters) {
                long sWeight = 0;
                for (FlipCutNodeSimpleWeight taxon : character.edges) {
                    long weight = CutGraphCutter.getInfinity();

                    if (!sSet.contains(taxon)) {
                        cutGraph.addEdge(character, taxon, weight);
                        cutGraph.addEdge(taxon, character.clone, weight);
                    } else {
                        sWeight = CutGraphCutter.getInfinity();
                    }
                }
                if (sWeight > 0) {
                    cutGraph.addEdge(character, s, sWeight);
                    cutGraph.addEdge(s, character.clone, sWeight);
                }
            }

            minCut = cutGraph.calculateMinSTCut(s, t);
            currentNode = new VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>>(minCut.gettSet(), minCut.minCutValue(), 1);
            initCuts[j] = currentNode;
            //save lightest cut for HEAP init
            if (currentNode.minCutValue() < lightestCut.minCutValue()) lightestCut = currentNode;
        }
        //initialize heap
        VaziraniCut<LinkedHashSet<FlipCutNodeSimpleWeight>> initialToHeap = new VaziraniCut<>(lightestCut.getCutSet(), lightestCut.minCutValue(), lightestCut.k()); //todo why new node?
        queueAscHEAP.add(initialToHeap);
    }

    static class Factory implements MultiCutterFactory<MultiCutGraphCutterVazirani, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, MaxFlowCutterFactory<MultiCutGraphCutterVazirani, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> {

        @Override
        public MultiCutGraphCutterVazirani newInstance(FlipCutGraphMultiSimpleWeight graph) {
            return new MultiCutGraphCutterVazirani(graph);
        }

        @Override
        public MultiCutGraphCutterVazirani newInstance(FlipCutGraphMultiSimpleWeight graph, ExecutorService executorService, int threads) {
            return newInstance(graph);
        }

        @Override
        public CutGraphTypes getType() {
            return CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG;
        }
    }
}
