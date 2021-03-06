package mincut.cutGraphAPI;


import mincut.cutGraphAPI.bipartition.STCut;

import java.util.List;

/**
 * Created by fleisch on 15.04.15.
 */
public interface DirectedCutting<V> {
    /**
     * Returns the minimum ST cut.
     * This does lazy computation, if the cut was not computed, it computes the cut.
     *
     * @return mincut all connected components of the input graph
     */
    STCut<V> calculateMinSTCut(V source, V sink);

    void submitSTCutCalculation(V source, V sink);
    List<STCut<V>>calculateMinSTCuts();
}
