package org.aksw.jena_sparql_api_sparql_path2;

import org.jgrapht.EdgeFactory;

public class EdgeFactoryLabeledEdge<V, T>
    implements EdgeFactory<V, LabeledEdge<V, T>>
{
    @Override
    public LabeledEdgeImpl<V, T> createEdge(V sourceVertex, V targetVertex) {
        LabeledEdgeImpl<V, T> result = new LabeledEdgeImpl<V, T>(sourceVertex, targetVertex, null);
        return result;
    }
}