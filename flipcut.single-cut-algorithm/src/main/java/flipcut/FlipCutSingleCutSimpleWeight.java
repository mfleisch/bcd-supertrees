package flipcut;


import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import flipcut.costComputer.CostComputer;
import flipcut.costComputer.FlipCutWeights;
import flipcut.costComputer.UnitCostComputer;
import flipcut.costComputer.WeightCostComputer;
import flipcut.flipCutGraph.CutGraphCutter;
import flipcut.flipCutGraph.FlipCutGraphSimpleWeight;
import flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import flipcut.flipCutGraph.SingleCutGraphCutter;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 15.01.13
 * Time: 18:13
 */
public class FlipCutSingleCutSimpleWeight extends AbstractFlipCutSingleCut<FlipCutNodeSimpleWeight,FlipCutGraphSimpleWeight,SingleCutGraphCutter> {

    public FlipCutSingleCutSimpleWeight() {
        super();
    }

    public FlipCutSingleCutSimpleWeight(CutGraphCutter.CutGraphTypes type) {
        super(type);
    }

    public FlipCutSingleCutSimpleWeight(Logger log, CutGraphCutter.CutGraphTypes type) {
        super(log, type);
    }

    public FlipCutSingleCutSimpleWeight(Logger log, ExecutorService executorService1, CutGraphCutter.CutGraphTypes type) {
        super(log, executorService1, type);
    }

    @Override
    protected FlipCutGraphSimpleWeight createGraph(List<FlipCutNodeSimpleWeight> component, TreeNode treeNode, final boolean checkEdges) {
        return new FlipCutGraphSimpleWeight(component,treeNode,checkEdges);
    }

    @Override
    protected SingleCutGraphCutter createCutter() {
        if (executorService == null) {
            return new SingleCutGraphCutter(type);
        }else{
            if (numberOfThreads > 0) {
                return new SingleCutGraphCutter(type,executorService,numberOfThreads);
            } else {
                return new SingleCutGraphCutter(type,executorService,CORES_AVAILABLE);
            }
        }
    }

    @Override
    protected FlipCutGraphSimpleWeight createInitGraph(CostComputer costsComputer) {
        return new FlipCutGraphSimpleWeight(costsComputer, bootstrapThreshold);
    }

    //this method contains only simple weightings
    @Override
    protected CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree) {

        if (UnitCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            logger.info("Using Unit Costs");
            return new UnitCostComputer(inputTrees,scaffoldTree);
        } else if (WeightCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            logger.info("Using " + weights);
            return new WeightCostComputer(inputTrees, weights, scaffoldTree);
        }

        logger.warn("No supported weight option found. Setting to standard: "+ FlipCutWeights.Weights.UNIT_COST);
        setWeights(FlipCutWeights.Weights.UNIT_COST);
        return new UnitCostComputer(inputTrees,scaffoldTree);

    }
}
