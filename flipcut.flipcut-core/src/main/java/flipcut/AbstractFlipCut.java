package flipcut;//import epos.model.tree.Tree;

import epos.model.tree.Tree;
import flipcut.costComputer.CostComputer;
import flipcut.costComputer.FlipCutWeights;
import flipcut.flipCutGraph.AbstractFlipCutGraph;
import flipcut.flipCutGraph.AbstractFlipCutNode;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 14:10
 */
public abstract class AbstractFlipCut<N extends AbstractFlipCutNode<N>, T extends AbstractFlipCutGraph<N>> {
    protected static final boolean DEBUG = false;
    //postprocess that deletes clade of the supertree without support from the input trees
    private ExecutorService GOBAL_EXECUTER;
    protected int numberOfThreads = 0;

    /**
     * Use edge weights
     */
    protected FlipCutWeights.Weights weights = FlipCutWeights.Weights.UNIT_COST;
    /**
     * Logger
     */
    protected Logger log;
    /**
     * Verbose logs
     */
    protected boolean verbose = false;
    /**
     * Set minimum bootstrap value of a clade to be part of the analysis ()
     */
    protected int bootstrapThreshold = 0;
    /**
     * The Graph actual working on
     */
    protected T initialGraph;

    protected AbstractFlipCut() {
        this(Logger.getLogger(AbstractFlipCut.class));
    }

    /**
     * Create new instace with logger
     *
     * @param log the logger
     */
    protected AbstractFlipCut(Logger log) {
        this.log = log;
    }


    /**
     * Provide lazy access to the log
     *
     * @return log the log
     */
    public Logger getLog() {
        if (log == null) {
            log = Logger.getLogger(getClass());
        }
        return log;
    }

    /**
     * Activate verbose log output
     *
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Sets the edge weights function
     *
     * @param weights the weighting scheme
     */
    public void setWeights(FlipCutWeights.Weights weights) {
        this.weights = weights;
    }

    public void setInputTrees(List<Tree> inputTrees) {
        setInputTrees(inputTrees, null);
    }

    public void setInputTrees(List<Tree> inputTrees, Tree scaffoldTree) {
        final CostComputer costs = initCosts(inputTrees, scaffoldTree);
        initialGraph = createInitGraph(costs);
    }

    public void setBootstrapThreshold(int bootstrapThreshold) {
        this.bootstrapThreshold = bootstrapThreshold;
    }

    public int getBootstrapThreshold() {
        return bootstrapThreshold;
    }


    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    //abstract stuff!
    public abstract List<Tree> getSupertrees();

    protected abstract T createInitGraph(CostComputer costsComputer);

    protected abstract CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree);

    protected ExecutorService getExecuter() {
        if (numberOfThreads <= 1) {
            GOBAL_EXECUTER = null;
        } else {
            if (GOBAL_EXECUTER == null)
                GOBAL_EXECUTER = Executors.newWorkStealingPool(numberOfThreads);
        }
        return GOBAL_EXECUTER;
    }

}
