package phylo.tree.algorithm.flipcut.cutter;

import mincut.cutGraphAPI.bipartition.Cut;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphTypes;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 29.11.12
 * Time: 16:37
 */
public class SingleCutGraphCutter extends SimpleCutGraphCutter<FlipCutGraphSimpleWeight> {
    private Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> cachedMinCut = null;


    public SingleCutGraphCutter(CutGraphTypes type) {
        super(type);
    }

    public SingleCutGraphCutter(CutGraphTypes type, ExecutorService executorService, int threads) {
        super(type, executorService, threads);
    }

    @Override
    public Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> cut(SourceTreeGraph<LinkedHashSet<FlipCutNodeSimpleWeight>> source) {
        cachedMinCut = super.cut(source);
        return cachedMinCut;
    }

    @Override
    public Cut<LinkedHashSet<FlipCutNodeSimpleWeight>> getMinCut() {
        return cachedMinCut;
    }

    @Override
    public void clear() {
        super.clear();
        cachedMinCut = null;
    }


    public static class Factory implements MaxFlowCutterFactory<SingleCutGraphCutter, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphSimpleWeight> {
        private final CutGraphTypes type;

        public Factory(CutGraphTypes type) {
            this.type = type;
        }


        public SingleCutGraphCutter newInstance() {
            return new SingleCutGraphCutter(type);
        }

        @Override
        public SingleCutGraphCutter newInstance(FlipCutGraphSimpleWeight graph) {
            return newInstance();
        }

        @Override
        public SingleCutGraphCutter newInstance(FlipCutGraphSimpleWeight graph, ExecutorService executorService, int threads) {
            return new SingleCutGraphCutter(type, executorService, threads);
        }

        @Override
        public CutGraphTypes getType() {
            return type;
        }
    }


}
