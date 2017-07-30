package org.aksw.jena_sparql_api.iso.index;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;

public abstract class SetOpsJGraphTBase<V, E, G extends Graph<V, E>>
    implements SetOps<G, V>
{
    protected abstract E transformEdge(E edge, Function<V, V> nodeTransform);

    @Override
    public G transformItems(G a, Function<V, V> nodeTransform) {
        G result = createNew();
        SetOpsJGraphTBase.transformItems(result, a, nodeTransform, this::transformEdge);
        return result;
    }

//	@Override
//	public DirectedGraph<Node, Triple> applyIso(DirectedGraph<Node, Triple> a, BiMap<Node, Node> itemTransform) {
//		G result = transformItems(graph, iso::get);
//		return result;
//	}


// JGraphT comes with an intersect view of graphs
//    @Override
//    public G intersect(G a, G b) {
//    	SetOpsJGraphTBase.intersection(a, b);
//    }

    @Override
    public G difference(G baseGraph, G removalGraph) {
        G result = createNew();
        SetOpsJGraphTBase.difference(result, baseGraph, removalGraph);
        return result;
    }

    @Override
    public int size(G g) {
        int result = g.edgeSet().size();
        return result;
    }


    public static <V, E, T extends Graph<V, E>> T difference(T result, Graph<V, E> baseSet, Graph<V, E> removalSet) {
        Graphs.addGraph(result, baseSet);

        //Graphs.unio
        result.removeAllEdges(removalSet.edgeSet());
        baseSet.vertexSet().forEach(v -> {
            if(baseSet.edgesOf(v).isEmpty()) {
                result.removeVertex(v);
            }
        });

        return result;
    }

//    public static <V, E> DirectedGraph<V, E> intersection(DirectedGraph<V, E> baseSet, DirectedGraph<V, E> removalSet) {
//        DirectedGraph<V, E> result = new DirectedSubgraph<>(baseSet, removalSet.vertexSet(), removalSet.edgeSet());
//        return result;
//    }


    public static <V, E, G extends Graph<V, E>> G transformItems(G result, G set, Function<V, V> nodeTransform, BiFunction<E, Function<V, V>, E> edgeTransform) {
        set.vertexSet().stream().map(nodeTransform).forEach(result::addVertex);

        set.edgeSet().stream().forEach(e -> {
            E isoEdge = edgeTransform.apply(e, nodeTransform);
            V isoSrc = nodeTransform.apply(set.getEdgeSource(e));
            V isoTgt = nodeTransform.apply(set.getEdgeTarget(e));

            result.addEdge(isoSrc, isoTgt, isoEdge);
        });

        return result;
    }


}