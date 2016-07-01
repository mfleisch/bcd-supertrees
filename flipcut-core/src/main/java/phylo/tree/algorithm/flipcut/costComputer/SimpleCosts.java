package phylo.tree.algorithm.flipcut.costComputer;




import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 14.01.13
 * Time: 17:47
 */
public abstract class SimpleCosts extends CostComputer {

    public SimpleCosts(List<Tree> trees, FlipCutWeights.Weights weights) {
        super(trees, weights);
    }

    public SimpleCosts(List<Tree> trees, FlipCutWeights.Weights weights, Tree scaffoldTree) {
        super(trees, weights, scaffoldTree);
    }


    public abstract long getEdgeWeight(TreeNode node);
}
