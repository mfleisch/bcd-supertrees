package phylo.tree.algorithm.flipcut.model;


import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 18.01.13
 * Time: 18:10
 */
public class Cut implements Comparable<Cut> {
    private LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes;
    private List<List<FlipCutNodeSimpleWeight>> comp;
    private List<FlipCutGraphMultiSimpleWeight> splittedGraphs;
    final long minCutValue;
    final FlipCutGraphMultiSimpleWeight sourceGraph;

    public Cut(LinkedHashSet<FlipCutNodeSimpleWeight> sinkNodes, long minCutValue, FlipCutGraphMultiSimpleWeight sourceGraph) {
        //splittedGraphs = (List<FlipCutGraphMultiSimpleWeight>) sourceGraph.split(sinkNodes);
        this.sinkNodes = sinkNodes;
        this.minCutValue = minCutValue;
        this.sourceGraph = sourceGraph;
        comp = null;
    }

    public Cut(List<List<FlipCutNodeSimpleWeight>> comp, FlipCutGraphMultiSimpleWeight graph) {
        sinkNodes = null;
        minCutValue = 0;
        sourceGraph = graph;
        this.comp = comp;
        //splittedGraphs = sourceGraph.buildComponentGraphs(comp);
    }

    public long getMinCutValue() {
        return minCutValue;
    }

    public int compareTo(Cut o) {
        return (minCutValue < o.minCutValue) ? -1 : ((minCutValue == o.minCutValue) ? 0 : 1);
    }

    public List<FlipCutGraphMultiSimpleWeight> getSplittedGraphs() {

        if (splittedGraphs == null) {
            if (comp != null) {
                splittedGraphs = sourceGraph.buildComponentGraphs(comp);
                comp = null;

            } else {
                splittedGraphs = (List<FlipCutGraphMultiSimpleWeight>) sourceGraph.split(sinkNodes);
                sinkNodes = null;
            }
        }
        return splittedGraphs;
    }
}