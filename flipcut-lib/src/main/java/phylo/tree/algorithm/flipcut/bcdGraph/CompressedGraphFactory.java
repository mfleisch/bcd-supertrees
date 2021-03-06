package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.impl.Constants;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.Hyperedge;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.MergedHyperedge;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.SimpleHyperedge;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;
import phylo.tree.model.TreeUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CompressedGraphFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompressedGraphFactory.class);

    public static CompressedBCDSourceGraph createSourceGraph(CostComputer costComputer, double bootstrapTheshold, boolean mergedEdges) {
        LOGGER.info("Creating graph representation of input trees...");
        LOGGER.info("Merge graph data structrure = " + mergedEdges);
        final Tree scaffold = costComputer.getScaffoldTree();
        final List<Tree> trees = costComputer.getTrees();

        TIntObjectMap<RoaringBitmap> scaffoldMapping = new TIntObjectHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
        TObjectIntMap<String> leafs = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);

        List<Hyperedge> edges = new LinkedList<>();
        Map<Set<String>, MergedHyperedge> duplicateEdges = new LinkedHashMap<>();
        Map<Set<String>, RoaringBitmap> duplicateBits = new HashMap<>();

        int leafIndex = 0;
        int numOfChars = 0;

        LOGGER.info("Reading trees...");
        RoaringBitmap activeScaffoldCharacters = null;
        if (scaffold != null) {
            TreeNode scaffoldRoot = scaffold.getRoot();
            Set<String> treeTaxa = new HashSet<>();

            for (TreeNode scaffoldNode : scaffold.vertices()) {
                if (scaffoldNode.isLeaf()) {
                    leafs.put(scaffoldNode.getLabel(), leafIndex++);
                    treeTaxa.add(scaffoldNode.getLabel());
                } else {
                    numOfChars++;
                }
            }
            AtomicInteger characterIndex = new AtomicInteger(leafIndex);
            activeScaffoldCharacters = addScaffoldCharacterRecursive(scaffoldRoot.getChildren(), characterIndex, costComputer, treeTaxa, leafs, duplicateBits, edges, duplicateEdges, scaffoldMapping, mergedEdges);
        }
        if (activeScaffoldCharacters == null) activeScaffoldCharacters = new RoaringBitmap();

        //do character stuff
        for (Tree tree : trees) {
            if (tree != scaffold) {
                TreeNode root = tree.getRoot();
                Set<String> treeTaxa = new HashSet<>(tree.vertexCount());
                List<TreeNode> inner = new LinkedList<>();

                for (TreeNode node : root.depthFirstIterator()) {
                    if (node.isLeaf()) {
                        if (scaffold == null && leafs.putIfAbsent(node.getLabel(), leafIndex) == leafs.getNoEntryValue())
                            leafIndex++;
                        treeTaxa.add((node.getLabel()));
                    } else if (!node.equals(root)) {
                        inner.add(node);
                    }
                }
                numOfChars += inner.size();

                for (TreeNode node : inner) {
                    //collect edges and zero edges
                    addEdge(node, treeTaxa, leafs, duplicateBits, edges, duplicateEdges, costComputer, mergedEdges);
                }
            }
        }


        assert leafs.size() == leafIndex;


        //create taxa;
        final String[] taxa = new String[leafs.size()];
        LOGGER.info("Add " + taxa.length + " Taxa to graph...");
        leafs.forEachEntry((a, b) -> {
            taxa[b] = a;
            return true;
        });

        //create chars
        int charIndex = taxa.length;
        final TIntObjectMap<Hyperedge> hyperedges = new TIntObjectHashMap<>(duplicateEdges.size());
        LOGGER.info("Add " + duplicateEdges.size() + " merged Characters to graph...");
        int allMergedChars = 0;
        for (Hyperedge hyperedge : edges) {
            hyperedges.put(charIndex++, hyperedge);
            if (hyperedge instanceof MergedHyperedge)
                allMergedChars += ((MergedHyperedge) hyperedge).umergedNumber();
            else
                allMergedChars++;
        }
        LOGGER.info(numOfChars + " where merged to " + allMergedChars + " and can be further reduced to " + duplicateEdges.size() + " during mincut");


        return new CompressedBCDSourceGraph(taxa, hyperedges, activeScaffoldCharacters, scaffoldMapping);
    }

    private static TIntSet createBits(TreeNode node, final TObjectIntMap<String> leafs) {
        TIntSet oneBits = new TIntHashSet();
        for (TreeNode treeNode : node.depthFirstIterator()) {
            if (treeNode.isLeaf()) {
                oneBits.add(leafs.get(treeNode.getLabel()));
            }
        }
        return oneBits;
    }

    private static RoaringBitmap getCompressedBits(final Map<Set<String>, RoaringBitmap> zs, Set<String> bits, final TObjectIntMap<String> leafs) {
        RoaringBitmap cbits = zs.get(bits);
        if (cbits == null) {
            cbits = new RoaringBitmap();
            for (String s : bits) {
                cbits.add(leafs.get(s));
                zs.put(bits, cbits);
            }
        }
        return cbits;
    }

    private static RoaringBitmap getCompressedBits(final Map<TIntSet, RoaringBitmap> zs, TIntSet bits) {
        RoaringBitmap edge = zs.get(bits);
        if (edge == null) {
            edge = RoaringBitmap.bitmapOf(bits.toArray());
            zs.put(bits, edge);
        }
        return edge;
    }


    private static Hyperedge addEdge(final TreeNode node, final Set<String> treeTaxa, final TObjectIntMap<String> leafs, final Map<Set<String>, RoaringBitmap> zs, final List<Hyperedge> edges, final Map<Set<String>, MergedHyperedge> duplicateEdges, final CostComputer costComputer, boolean mergedEdge) {
        if (mergedEdge) {
            return addMergedEdge(node, treeTaxa, leafs, zs, edges, duplicateEdges, costComputer);
        } else {
            return addSimpleEdge(node, treeTaxa, leafs, zs, edges, costComputer);
        }
    }

    private static SimpleHyperedge addSimpleEdge(final TreeNode node, final Set<String> treeTaxa, final TObjectIntMap<String> leafs, final Map<Set<String>, RoaringBitmap> zs, final List<Hyperedge> edges, final CostComputer costComputer) {
        Set<String> oneBits = TreeUtils.getLeafLabels(node);
        RoaringBitmap edgeOnes = getCompressedBits(zs, oneBits, leafs);
        Set<String> zeroBits = new HashSet<>(treeTaxa);
        zeroBits.removeAll(oneBits);
        RoaringBitmap edgeZeroes = getCompressedBits(zs, zeroBits, leafs);
        SimpleHyperedge hyperedge = new SimpleHyperedge(edgeOnes, edgeZeroes, costComputer.getEdgeWeight(node));

        edges.add(hyperedge);

        return hyperedge;
    }

    private static MergedHyperedge addMergedEdge(final TreeNode node, final Set<String> treeTaxa, final TObjectIntMap<String> leafs, final Map<Set<String>, RoaringBitmap> zs, final List<Hyperedge> edges, final Map<Set<String>, MergedHyperedge> duplicateEdges, final CostComputer costComputer) {
        Set<String> oneBits = TreeUtils.getLeafLabels(node);

        MergedHyperedge hyperedge = duplicateEdges.get(oneBits);
        if (hyperedge == null) {
            RoaringBitmap edge = getCompressedBits(zs, oneBits, leafs);
            hyperedge = new MergedHyperedge(edge);
            duplicateEdges.put(oneBits, hyperedge);
            edges.add(hyperedge);
        }
        Set<String> zeroBits = new HashSet<>(treeTaxa);
        zeroBits.removeAll(oneBits);

        hyperedge.addZero(
                getCompressedBits(zs, zeroBits, leafs),
                costComputer.getEdgeWeight(node)
        );

        return hyperedge;
    }


    private static RoaringBitmap addScaffoldCharacterRecursive(final List<TreeNode> children, final AtomicInteger charIndex,
                                                               final CostComputer costComputer,
                                                               final Set<String> treeTaxa,
                                                               TObjectIntMap<String> leafs,
                                                               final Map<Set<String>, RoaringBitmap> zs,
                                                               final List<Hyperedge> edges,
                                                               final Map<Set<String>, MergedHyperedge> duplicateEdges,
                                                               final TIntObjectMap<RoaringBitmap> scaffoldMapping,
                                                               boolean mergedEdges) {

        RoaringBitmap childrenI = new RoaringBitmap();

        for (TreeNode scaffoldNode : children) {
            if (scaffoldNode.isInnerNode()) {
                //create hyperedge
                Hyperedge hyperedge = addEdge(scaffoldNode, treeTaxa, leafs, zs, edges, duplicateEdges, costComputer, mergedEdges);

                assert hyperedge.getWeight() == CutGraphCutter.getInfinity();

                final int currentIndex = charIndex.getAndIncrement();
                childrenI.add(currentIndex);

                RoaringBitmap set = addScaffoldCharacterRecursive(scaffoldNode.getChildren(), charIndex, costComputer, treeTaxa, leafs, zs, edges, duplicateEdges, scaffoldMapping, mergedEdges);
                if (!set.isEmpty())
                    scaffoldMapping.put(currentIndex, set);
            }
        }

        return childrenI;
    }
}
