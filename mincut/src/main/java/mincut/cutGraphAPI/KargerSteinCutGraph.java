package mincut.cutGraphAPI;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import mincut.EdgeColor;
import mincut.cutGraphAPI.bipartition.AbstractBipartition;
import mincut.cutGraphAPI.bipartition.CutFactory;
import mincut.cutGraphAPI.bipartition.SimpleHashableCut;
import mincut.cutGraphImpl.minCutKargerStein.KargerStein;
import mincut.cutGraphImpl.minCutKargerStein.SimpleGraph;
import mincut.cutGraphImpl.minCutKargerStein.Vertex;

import java.util.*;

/**
 * Created by fleisch on 15.04.15.
 */
public class KargerSteinCutGraph<V, C extends CutFactory<LinkedHashSet<V>, ? extends AbstractBipartition<V>>> implements MultiCutGraph<V>, Cutting<V>, EdgeColorableUndirectedGraph<V> {
    private static final boolean RESCURSIVE_KARGER = true;
    private TIntObjectMap<V> vertexMap = new TIntObjectHashMap<>();
    private Map<V, Vertex> vertexMapBack = new HashMap<>();
    private BiMap<V, EdgeColor> charactermap = HashBiMap.create();

    private SimpleGraph g;
    private int vertexIndex = 0;

    private final C cutFactory;

    public KargerSteinCutGraph(C cutFactory) {
        this.cutFactory = cutFactory;
        clear();
    }

    @Override
    public List<AbstractBipartition<V>> calculateMinCuts() {
        KargerStein cutter = new KargerStein();
        List<SimpleHashableCut> cuts = cutter.getMinCuts(g, RESCURSIVE_KARGER);

        ArrayList<AbstractBipartition<V>> basicCuts = new ArrayList<>(cuts.size());
        for (SimpleHashableCut cut : cuts) {
            basicCuts.add(buildCut(cut));
        }
//        Collections.sort(basicCuts);
        return basicCuts;
    }

    private AbstractBipartition<V> buildCut(SimpleHashableCut c) {

        //get source taxa set
        LinkedHashSet<V> sSet = new LinkedHashSet<>();
        c.getSset().forEach(v -> {
            sSet.add(vertexMap.get(v));
            return true;
        });

        //get targe taxa set
        LinkedHashSet<V> tSet = new LinkedHashSet<>();
        c.getTset().forEach(v -> {
            tSet.add(vertexMap.get(v));
            return true;
        });

        //get edges
        LinkedHashSet<V> cutEdges = new LinkedHashSet<>();
        //get cutsocre from edges
        for (EdgeColor color : c.getEdgeColors()) {
            cutEdges.add(charactermap.inverse().get(color));
        }


        return cutFactory.newCutInstance(sSet, tSet, cutEdges, (long) c.minCutValue());
    }


    @Override
    public List<AbstractBipartition<V>> calculateMinCuts(int numberOfCuts) {
        if (numberOfCuts <= 0) return null;

        if (numberOfCuts == 1) {
            return Arrays.asList(calculateMinCut());
        } else {
            List<AbstractBipartition<V>> cuts = calculateMinCuts();
            if (cuts.size() > numberOfCuts) {
//                System.out.println("leght before: " + cuts.size());
                cuts.subList(numberOfCuts + 1, cuts.size()).clear();
//                System.out.println("leght after: " + cuts.size());
            }
            return cuts;
        }
    }

    @Override
    public AbstractBipartition<V> calculateMinCut() {
        return buildCut((SimpleHashableCut) new KargerStein<SimpleGraph, TIntSet>().getMinCut(g, true));
    }

    public AbstractBipartition<V> sampleCut() {
        return buildCut(KargerStein.sampleCut(g).asCut());
    }

    public List<AbstractBipartition<V>> sampleCuts(int numberOfCuts) {
        Set<SimpleGraph> graphs = new HashSet<>(numberOfCuts);

        for (int i = 0; i < numberOfCuts; i++) {
            graphs.add(KargerStein.sampleCut(g));
        }

        Iterator<SimpleGraph> it = graphs.iterator();

        List<AbstractBipartition<V>> cuts = new ArrayList<>(graphs.size());
        while (it.hasNext()) {
            cuts.add(buildCut(it.next().asCut()));
            it.remove();//dont know if i should do that -> doe not double the memory but remove is some overheads
        }
        return cuts;
    }

    @Override
    public void addNode(V vertex) {
        vertexMap.put(vertexIndex, vertex);
        Vertex v = new Vertex(vertexIndex);
        vertexMapBack.put(vertex, v);
        g.addVertex(v);
        vertexIndex++;
    }


    @Override
    public void addEdge(V vertex1, V vertex2, long capacity) {
        addEdge(vertex1, vertex2, capacity, null);
    }

    @Override
    public void addEdge(V vertex1, V vertex2, long capacity, V hyperedge) {
        addEdge(vertex1, vertex2, capacity, hyperedge, false);
    }

    public void addEdge(V vertex1, V vertex2, double capacity, V hyperedge, boolean uncutable) {
        if (!vertexMapBack.containsKey(vertex1))
            addNode(vertex1);
        if (!vertexMapBack.containsKey(vertex2))
            addNode(vertex2);

        EdgeColor color = charactermap.get(hyperedge);
        if (color == null) {
            color = new EdgeColor(capacity, uncutable);
            if (hyperedge != null)
                charactermap.put(hyperedge, color);
        }
        g.addEdge(vertexMapBack.get(vertex1), vertexMapBack.get(vertex2), color);
    }

    @Override
    public void clear() {
        vertexMap.clear();
        g = new SimpleGraph(/*weighter*/);
        vertexIndex = 0;
        charactermap.clear();
    }


}
