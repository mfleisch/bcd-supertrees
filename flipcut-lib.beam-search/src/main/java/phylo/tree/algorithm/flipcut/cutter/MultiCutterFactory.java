package phylo.tree.algorithm.flipcut.cutter;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 16.02.17.
 */

import phylo.tree.algorithm.flipcut.SourceTreeGraphMultiCut;
import phylo.tree.algorithm.flipcut.cutter.blacklists.GreedyBlackList;
import phylo.tree.algorithm.flipcut.cutter.blacklists.RandomizedBlackList;
import phylo.tree.algorithm.flipcut.cutter.blacklists.RecursiveBlackList;
import phylo.tree.algorithm.flipcut.cutter.undirectedConversion.ChracterScoreModifier;
import phylo.tree.algorithm.flipcut.cutter.undirectedConversion.KargerGraphCreator;
import phylo.tree.algorithm.flipcut.cutter.undirectedConversion.RelativeToNumOfConnection;
import phylo.tree.algorithm.flipcut.cutter.undirectedConversion.StaticKargerGraphCreator;
import phylo.tree.algorithm.flipcut.flipCutGraph.CutGraphTypes;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface MultiCutterFactory<C extends MultiCutter<S, T>, S, T extends SourceTreeGraphMultiCut<S, T>> extends CutterFactory<C, S, T> {
    enum MultiCutterType {
        VAZIRANI,
        GREEDY,
        GREEDY_RAND,
        GREEDY_RECURSIVE,
        MC,
        MC_RECURSIVE,
        MC_STATIC_ABS,
        MC_STATIC_REL,
        CUT_SAMPLING,
        CUT_SAMPLING_NON_RECURSIVE
    }

    static MultiCutterFactory newInstance() {
        return newInstance(MultiCutterType.VAZIRANI, CutGraphTypes.COMPRESSED_BCD_VIA_MAXFLOW_TARJAN_GOLDBERG);
    }

    static MultiCutterFactory newInstance(MultiCutterType multiCutterType) {
        return newInstance(multiCutterType, CutGraphTypes.COMPRESSED_BCD_VIA_MAXFLOW_TARJAN_GOLDBERG);
    }

    static MultiCutterFactory newInstance(CutGraphTypes simpleCutterType) {
        return newInstance(MultiCutterType.VAZIRANI, simpleCutterType);
    }

    static MultiCutterFactory newInstance(MultiCutterType multiCutterType, CutGraphTypes simpleCutterType) {
        switch (multiCutterType) {
            case VAZIRANI:
                return createVazirani(simpleCutterType);
            case CUT_SAMPLING:
                return new MultiCutGraphCutterUndirectedTranfomationCompressed.Factory(true);
            case CUT_SAMPLING_NON_RECURSIVE:
                return new MultiCutGraphCutterUndirectedTranfomationCompressed.Factory(false);
            case GREEDY:
                return new MultiCutGraphCutterGreedy.Factory(simpleCutterType, new GreedyBlackList());
            case GREEDY_RAND:
                return new MultiCutGraphCutterGreedy.Factory(simpleCutterType, new RandomizedBlackList());
            case GREEDY_RECURSIVE:
                return new MultiCutGraphCutterGreedy.Factory(simpleCutterType, new RecursiveBlackList());
            case MC:
                return new MultiCutGraphCutterUndirectedTranfomation.Factory(new ChracterScoreModifier() {
                }, new KargerGraphCreator() {
                }, true);
            case MC_RECURSIVE:
                return new MultiCutGraphCutterUndirectedTranfomation.Factory(new ChracterScoreModifier() {
                }, new KargerGraphCreator() {
                }, false);
            case MC_STATIC_ABS:
                return new MultiCutGraphCutterUndirectedTranfomation.Factory(new ChracterScoreModifier() {
                }, new StaticKargerGraphCreator(), false);
            case MC_STATIC_REL:
                return new MultiCutGraphCutterUndirectedTranfomation.Factory(new RelativeToNumOfConnection(), new StaticKargerGraphCreator(), false);
            default:
                return createVazirani(simpleCutterType);
        }
    }

    static MultiCutterFactory createVazirani(CutGraphTypes simpleCutterType) {
        if (simpleCutterType == CutGraphTypes.HYPERGRAPH_MINCUT_VIA_MAXFLOW_TARJAN_GOLDBERG) {
            return new MultiCutGraphCutterVazirani.Factory();
        } else {
            return new MultiCutGrgaphCutterVaziraniCompressedBCD.Factory();
        }
    }

}
