package mincut.cutGraphImpl.minCutKargerStein;

import com.google.common.util.concurrent.AtomicDouble;
import gnu.trove.TIntCollection;
import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import mincut.cutGraphAPI.bipartition.HashableCut;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.bcdGraph.CompressedBCDGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.Hyperedge;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class CompressedKargerGraph implements KargerGraph<CompressedKargerGraph, RoaringBitmap> {

    private int numberOfvertices;
    private final TIntObjectMap<RoaringBitmap> mergedTaxa;
    private RoaringBitmap[] hyperEdges = null;
    private double[] weights = null;
    private double[] cumulativeWeights = null;
    private int hashCache = 0;


    //this is more for testing than anything else
    public CompressedKargerGraph(List<? extends TIntCollection> hyperedges, List<? extends Number> weights) {
        final RoaringBitmap allTaxa = new RoaringBitmap();

        if (hyperedges == null || weights == null || hyperedges.size() != weights.size())
            throw new IllegalArgumentException("Input must not be null and there has to be a weight for every edge.");

        //add weights
        cumulativeWeights = new double[weights.size()];
        this.weights = new double[weights.size()];

        int i = 0;
        double current = 0d;
        for (Number weight : weights) {
            current += weight.doubleValue();
            this.weights[i] = weight.doubleValue();
            cumulativeWeights[i++] = current;
        }

        //add hyperedges
        hyperEdges = new RoaringBitmap[hyperedges.size()];
        i = 0;
        for (TIntCollection hyperedge : hyperedges) {
            hyperEdges[i] = RoaringBitmap.bitmapOf(hyperedge.toArray());
            allTaxa.or(hyperEdges[i++]);
        }

        numberOfvertices = allTaxa.getCardinality();
        mergedTaxa = createMergedTaxaMap(allTaxa, numberOfvertices);

    }

    public CompressedKargerGraph(CompressedBCDGraph sourceGraph) {
        this(sourceGraph, true);
    }

    public CompressedKargerGraph(CompressedBCDGraph sourceGraph, boolean preMergeInfinityChars) {
        final RoaringBitmap allTaxa = new RoaringBitmap();

        if (preMergeInfinityChars && sourceGraph.hasGuideEdges()) {
            TObjectDoubleMap<RoaringBitmap> charCandidates = new TObjectDoubleHashMap<>(sourceGraph.numCharacter(), Constants.DEFAULT_LOAD_FACTOR, Double.NaN);


            for (Hyperedge hyperedge : sourceGraph.hyperEdges()) {
                //check for normal edge
                if (!hyperedge.isInfinite()) {
                    RoaringBitmap taxa = hyperedge.ones().clone();
                    for (Hyperedge guide : sourceGraph.guideHyperEdges()) {
                        RoaringBitmap intersection = RoaringBitmap.and(taxa, guide.ones());
                        if (!intersection.isEmpty()) {
                            taxa.xor(intersection);
                            taxa.add(guide.ones().first());
                        }
                    }
                    //add only if there are edges left after merging
                    if (taxa.getCardinality() > 0) {
                        allTaxa.or(taxa);
                        if (taxa.getCardinality() > 1) //add characters and merge identical ones
                            charCandidates.adjustOrPutValue(taxa, hyperedge.getWeight(), hyperedge.getWeight());
                    }

                }
            }

            refreshCharacters(charCandidates);

            numberOfvertices = allTaxa.getCardinality();
            mergedTaxa = createMergedTaxaMap(allTaxa, numberOfvertices);
            //add pre merged taxa
            for (Hyperedge hyperedge : sourceGraph.guideHyperEdges()) {
                mergedTaxa.get(hyperedge.ones().first()).or(hyperedge.ones());
            }
        } else {
            TObjectDoubleMap<RoaringBitmap> charCandidates = new TObjectDoubleHashMap<>(sourceGraph.numCharacter(), Constants.DEFAULT_LOAD_FACTOR, Double.NaN);
            for (Hyperedge hyperedge : sourceGraph.hyperEdges()) {
                //check for normal edge
                if (!charCandidates.adjustValue(hyperedge.ones(), hyperedge.getWeight())) {
                    charCandidates.put(hyperedge.ones().clone(), hyperedge.getWeight());
                    allTaxa.or(hyperedge.ones());
                }
            }

            refreshCharacters(charCandidates);

            numberOfvertices = allTaxa.getCardinality();
            mergedTaxa = createMergedTaxaMap(allTaxa, numberOfvertices);
        }
    }


    //clone constructor
    private CompressedKargerGraph(RoaringBitmap[] hyperEdges, double[] weights, double[] cumulativeWeights, TIntObjectMap<RoaringBitmap> mergedTaxa, int numVertices) {
        this.mergedTaxa = mergedTaxa;
        this.hyperEdges = hyperEdges;
        this.weights = weights;
        this.cumulativeWeights = cumulativeWeights;
        this.numberOfvertices = numVertices;
    }

    private TIntObjectMap<RoaringBitmap> createMergedTaxaMap(final RoaringBitmap allTaxa, final int numOfTaxa) {
        final TIntObjectMap<RoaringBitmap> mergedTaxa = new TIntObjectHashMap<>(numOfTaxa);
        allTaxa.forEach((IntConsumer) value -> {
            mergedTaxa.put(value, RoaringBitmap.bitmapOf(value));
        });
        return mergedTaxa;
    }

    public void contract() {
        contract(ThreadLocalRandom.current());
    }

    @Override
    public CompressedKargerGraph contractAndKeep() {
        final CompressedKargerGraph keep = new CompressedKargerGraph(cloneChars(hyperEdges), weights, cumulativeWeights, cloneTaxa(mergedTaxa), numberOfvertices);
        contract();
        return keep;
    }

    public void contract(final Random random) {
        final int selectedIndex = drawCharacter(random);
        // select the character from wich we want do merge (probs correspond to weight)
        final RoaringBitmap selectedCharacter = hyperEdges[selectedIndex];
        final int numberOfTaxaInCharacter = selectedCharacter.getCardinality();

        // select an edge to merge (equally distributed)
        //select random pair of taxa -> clique so every edge exists
        final int firstDrawn;
        int secondDrawn;
        if (selectedCharacter.getCardinality() == 2) {
            firstDrawn = selectedCharacter.first();
            secondDrawn = selectedCharacter.last();
        } else {
            firstDrawn = selectedCharacter.select(random.nextInt(numberOfTaxaInCharacter));
            int randSecond = random.nextInt(numberOfTaxaInCharacter - 1) + 1; //draw from all but the first one
            secondDrawn = selectedCharacter.select(randSecond);
            if (secondDrawn == firstDrawn) //if firstDrawn equals secondDrawn use the first one which was not part of the random selections
                secondDrawn = selectedCharacter.first();
        }

        assert firstDrawn != secondDrawn;

        // merge taxa
        mergedTaxa.get(firstDrawn).or(mergedTaxa.remove(secondDrawn));

        numberOfvertices--;

        // refresh hyperEdges and cumulative weights
        refreshCharactersAndCumulativeWeights(firstDrawn, secondDrawn);
    }

    private void refreshCharactersAndCumulativeWeights(int firstDrawn, int secondDrawn) {

        TObjectDoubleMap<RoaringBitmap> charCandidates = new TObjectDoubleHashMap<>(hyperEdges.length);
        for (int i = 0; i < hyperEdges.length; i++) {
            if (!mergeEdgeIfExistsAndCheckIsEmpty(hyperEdges[i], firstDrawn, secondDrawn)) {
                charCandidates.adjustOrPutValue(hyperEdges[i], weights[i], weights[i]);
            }
        }

        refreshCharacters(charCandidates);
    }

    private void refreshCharacters(TObjectDoubleMap<RoaringBitmap> charCandidates) {
        hyperEdges = new RoaringBitmap[charCandidates.size()];
        weights = new double[charCandidates.size()];
        cumulativeWeights = new double[charCandidates.size()];

        AtomicInteger index = new AtomicInteger(0);
        AtomicDouble tmp = new AtomicDouble(0d);
        charCandidates.forEachEntry((key, value) -> {
            final int i = index.getAndIncrement();
            hyperEdges[i] = key;
            weights[i] = value;
            cumulativeWeights[i] = tmp.addAndGet(value);
            return true;
        });
    }


    public boolean isCutted() {
        return mergedTaxa.size() == 2 && numberOfvertices == 2;
    }

    @Override
    public double getSumOfWeights() {
        return cumulativeWeights[cumulativeWeights.length - 1];
    }

    @Override
    public int getNumberOfVertices() {
        return numberOfvertices;
    }

    private boolean mergeEdgeIfExistsAndCheckIsEmpty(final RoaringBitmap connectedTaxa, final int firstDrawn, final int secondDrawn) {
        if (connectedTaxa.contains(secondDrawn)) {
            connectedTaxa.flip(secondDrawn);
            if (!connectedTaxa.contains(firstDrawn))
                connectedTaxa.flip(firstDrawn);
        }

        return connectedTaxa.getCardinality() < 2;
    }

    private int drawCharacter(final Random random) {
        double max = getSumOfWeights();
        double r = max * random.nextDouble();
        int i = Arrays.binarySearch(cumulativeWeights, r);
        return i < 0 ? Math.abs(i) - 1 : i;
    }


    private static RoaringBitmap[] cloneChars(final RoaringBitmap[] hyperEdges) {
        final RoaringBitmap[] chars = new RoaringBitmap[hyperEdges.length];
        for (int i = 0; i < hyperEdges.length; i++) {
            chars[i] = hyperEdges[i].clone();
        }
        return chars;
    }

    private static TIntObjectMap<RoaringBitmap> cloneTaxa(final TIntObjectMap<RoaringBitmap> mergedTaxa) {
        final TIntObjectMap<RoaringBitmap> nuMergedTaxa = new TIntObjectHashMap<>(mergedTaxa.size());
        mergedTaxa.forEachEntry((a, b) -> {
            nuMergedTaxa.put(a, b.clone());
            return true;
        });

        return nuMergedTaxa;
    }

    @Override
    public CompressedKargerGraph clone() {
        CompressedKargerGraph clone = new CompressedKargerGraph(
                cloneChars(hyperEdges),
                weights.clone(),
                cumulativeWeights.clone(),
                cloneTaxa(mergedTaxa),
                numberOfvertices
        );
        return clone;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompressedKargerGraph)) return false;

        CompressedKargerGraph graph = (CompressedKargerGraph) o;

        if (Double.compare(graph.getSumOfWeights(), getSumOfWeights()) != 0) return false;
        if (isCutted() != graph.isCutted()) return false;
        boolean r = mergedTaxa.valueCollection().equals(graph.mergedTaxa.valueCollection());
        if (r && hashCode() != graph.hashCode())
            throw new RuntimeException("Hash exception!!!!!!!");
        return r;

    }

    @Override
    public int hashCode() {
        if (!isCutted() || hashCache == 0) {
            int result;
            long temp;
            temp = Double.doubleToLongBits(getSumOfWeights());
            result = (int) (temp ^ (temp >>> 32));
            result = 31 * result + mergedTaxa.valueCollection().hashCode();
            result = 31 * result + (isCutted() ? 1 : 0);
            hashCache = result;
        }
        return hashCache;
    }

    public HashableCut<RoaringBitmap> asCut() {
        if (!isCutted())
            throw new IllegalStateException("Graph has to be cutted to get Cut representation");
        TIntObjectIterator<RoaringBitmap> it = mergedTaxa.iterator();
        it.advance();
        RoaringBitmap s = it.value();
        it.advance();
        RoaringBitmap t = it.value();
        return new HashableCut<>(s, t, getSumOfWeights());
    }
}
