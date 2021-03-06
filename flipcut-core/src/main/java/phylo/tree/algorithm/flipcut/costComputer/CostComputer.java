package phylo.tree.algorithm.flipcut.costComputer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phylo.tree.algorithm.flipcut.flipCutGraph.AbstractFlipCutNode;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User: Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * 09.07.12 17:55
 */

/**
 * node weight styles
 */
public abstract class CostComputer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CostComputer.class);

    /**
     * The accuracy used to go from double to long
     */
    public final static long ACCURACY = 1000000000L; //100000000;
    public final static double MIN_VALUE = 1d / ACCURACY;
    public final static double ZERO = Double.MIN_VALUE;

    public final static Set<FlipCutWeights.Weights> SUPPORTED_COST_TYPES = Collections.emptySet();

    protected final List<Tree> trees;
    protected final Tree scaffoldTree;
    protected FlipCutWeights.Weights weights;

    //just for normalization for easy combination of different weighting types
    protected double longestBranch;
    protected int maxLevel;
    protected double maxBSValue = -1d;

    public CostComputer(List<Tree> trees, FlipCutWeights.Weights weights) {
        this(trees, weights, null);
    }


    public CostComputer(List<Tree> inputTrees, FlipCutWeights.Weights weights, Tree scaffoldTree) {
        LOGGER.info("Initializing CostComputer...");
        this.scaffoldTree = scaffoldTree;
        trees = new ArrayList<>(inputTrees);

        //find longest branch... //todo check if this is still needed
        longestBranch = 0;
        for (Tree tree : trees) {
            //finde longest branch and max BSValue
            for (TreeNode node : tree.vertices()) {
                if (node.getParent() != null && node.isInnerNode()) {
                    //branch stuff
                    double branchLength = node.getDistanceToParent();
                    if (branchLength > longestBranch)
                        longestBranch = branchLength;

                    if (node.isLeaf()) {
                        //level stuff
                        int level = node.getLevel();
                        if (level > maxLevel)
                            maxLevel = level;
                    } else {
                        //BS stuff
                        double currentBS = parseBSValueFromLabel(node);
                        if (currentBS > maxBSValue) {
                            maxBSValue = currentBS;
                        }
                    }
                }
            }
        }

        if (scaffoldTree != null && !trees.contains(scaffoldTree)) {
//            System.out.println("adding scaffold tree to treelist");
            trees.add(scaffoldTree);
            //scaffoldTree.getRoot().setLabel(Double.toString(getScaffoldWeight()));
        }
        if (maxBSValue < 0d) {
            maxBSValue = 100d;
        }
        this.weights = weights;
        LOGGER.info("...Done!");
    }

    protected double parseBSValueFromLabel(TreeNode node) {
        if (node.getLabel() != null) {
            try {
                return Double.valueOf(node.getLabel());
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;

    }

    //returns 1 if no weight is set
    protected double parseTreeWeightFromLabel(Tree tree) {
        String rootLabel = tree.getRoot().getLabel();
        if (rootLabel != null) {
            try {
                return Double.valueOf(rootLabel);
            } catch (NumberFormatException e) {
                return 1d;
            }
        }
        return 1d;
    }

    public abstract long getEdgeWeight(TreeNode node, List<? extends AbstractFlipCutNode> leafes, AbstractFlipCutNode leaf);

    public abstract long getEdgeWeight(TreeNode node, List<TreeNode> leafes, TreeNode leaf);

    public long getEdgeWeight(TreeNode node) {
        return getEdgeWeight(node, null, (TreeNode) null);
    }


    public List<Tree> getTrees() {
        return trees;
    }

    public Tree getScaffoldTree() {
        return scaffoldTree;
    }
}