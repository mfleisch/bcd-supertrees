package flipcut.clo;

import epos.model.tree.Tree;
import flipcut.AbstractFlipCut;
import flipcut.costComputer.FlipCutWeights;
import flipcut.flipCutGraph.CutGraphCutter;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.ExplicitBooleanOptionHandler;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.kohsuke.args4j.ExampleMode.ALL;
import static org.kohsuke.args4j.OptionHandlerFilter.PUBLIC;


/**
 * Created by fleisch on 19.06.14.
 */
public abstract class FlipCutCLO <A extends AbstractFlipCut>{
    //should return the name of the algorithm, respectively the command to run the method
    protected final String NAME  =  initName();
    protected abstract String initName();

    protected final A algorithm = initAlgorithm();
    protected abstract A initAlgorithm();
    public A getAlgorithm() {
        return algorithm;
    }

    // receives other command line parameters than options
    @Argument
    public List<String> arguments = new ArrayList<>();

    protected static enum Algorithm {BCD,FC}
    protected static enum SuppportedWeights {UNIT_WEIGHT, BRANCH_LENGTH, BOOTSTRAP_VALUES, LEVEL, BRANCH_AND_LEVEL, BOOTSTRAP_AND_LEVEL}

    protected final EnumMap<SuppportedWeights, FlipCutWeights.Weights> weightMapping = initWheightMapping();
    protected abstract EnumMap<SuppportedWeights,FlipCutWeights.Weights> initWheightMapping();

    //##### flip cut parameter #####
    //cut graph type
    @Option(name="-c", aliases = "--cutGraphImplementation", usage = "Choose the graph structure used for the mincut calculation", hidden = true)
    public abstract void setGraphType(CutGraphCutter.CutGraphTypes graphType);

    //FlipCut or BCD
    @Option(name="-a", aliases = "--algorithm", usage="The algorithm that is used to estimate the Supertree.\n BCD = BadCharacterDeletion supertrees\n FC = FlipCut supertrees.", hidden = true)
    public void setAlgorithm(Algorithm algo) {
        if (algo == Algorithm.FC)
            setGraphType(CutGraphCutter.CutGraphTypes.MAXFLOW_TARJAN_GOLDBERG);
        else
            setGraphType(CutGraphCutter.CutGraphTypes.HYPERGRAPH_MINCUT_VIA_TARJAN_MAXFLOW);
    }//default value is true

    //Weighting
    @Option(name="-w", aliases = "--graphWeighting", usage="Weighting strategy",forbids = "-W")
    public void setWeights(SuppportedWeights weighting){
        if (weighting != null){
            FlipCutWeights.Weights tmp = weightMapping.get(weighting);
            if (tmp != null){
                setWeights(tmp);
            }
        }
    }

    @Option(name="-W", aliases = "--internalWeighting", usage="Weighting with internal code including nonofficial weightings", forbids = "-w",hidden = true)
    public void setWeights(FlipCutWeights.Weights weights){
        algorithm.setWeights(weights);
    }//default value is UW

    @Option(name="-b", aliases = "--bootstrapThreshold", usage="Minimal bootstrap value of a tree-node to be considered during the supertree calculation")
    public void setBootstrapThreshold(int bootstrapThreshold){
        algorithm.setBootstrapThreshold(bootstrapThreshold);
    }//default bootstrap Threshold value


    public void setInputTrees(List<Tree> inputTrees){
        setInputTrees(inputTrees, null);
    }

    public void setInputTrees(List<Tree> inputTrees, Tree guideTree){
        algorithm.setInputTrees(inputTrees,guideTree);
    }
    
    public Tree getSupertree(){
        return getSupertrees().get(0);
    }

    private List<Tree> getSupertrees() {
        return algorithm.getSupertrees();
    }

    //##### command line parameter #####
    @Option(name="-s", aliases = "--useSCMTree", handler = ExplicitBooleanOptionHandler.class, usage="Use SCM-tree as guide tree")
    public boolean useSCM = true;  //default value true

    @Option(name="-o", aliases = "--outputPath", usage="Output file" )
    public File output = null;  //default value null

    //pre and post procesing stuff
    @Option(name="-r", aliases = "--ucr", usage="Run unsupported clade reduction post-processing", hidden = true)
    public boolean unsupportedCladeReduction = false;

    @Option(name="-u", aliases = "--uds", usage="Run undisputed sibling pre-processing", hidden = true)
    public boolean removeUndisputedSiblings = false;

    @Option(name = "-v", aliases = "--verbose", usage = "Minimal console output")
    public boolean verbose = false;

    @Option(name = "-i", aliases = "--insufficient", usage = "Skip if input trees have insufficient taxa overlap", hidden = true)
    public boolean skipInsufficientOverlapInstances = false; //todo warn if taxa hav insufficient taxa overlap taxa overlap check



    //help option
    @Option(name = "-h", aliases = "--help", usage = "usage message", help = true, forbids = "-H")
    public boolean help = false;

    //help option
    @Option(name = "-H", aliases = "--HELP", usage = "Full usage message including nonofficial Options", help = true ,forbids = "-h")
    public boolean fullHelp = false;

    public void printHelp(final CmdLineParser parser) {
        printHelp(parser, System.out);
    } //standard help

    public void printHelp(final CmdLineParser parser, PrintStream stream) {
        printUsage(stream);
        stream.println();
        stream.println();
        // print the list of available options
        printOptions(parser, stream);
        stream.println();
        stream.println();
        // print option sample. This is useful some time
        printExample(parser, stream);
    }

    protected abstract void printUsage(PrintStream stream);


    protected void printOptions(final CmdLineParser parser, PrintStream stream) {
        stream.println("General options:");
        if (fullHelp) {
            parser.printUsage(new OutputStreamWriter(stream), null, ALL);
        } else {
            parser.printUsage(new OutputStreamWriter(stream), null, PUBLIC);
        }
    }

    protected void printExample(final CmdLineParser parser, PrintStream stream) {
        stream.println("Example:");
        if (fullHelp) {
            stream.println(NAME + " " + parser.printExample(ALL));
        } else {
            stream.println(NAME + " " + parser.printExample(PUBLIC));
        }
    }


}
