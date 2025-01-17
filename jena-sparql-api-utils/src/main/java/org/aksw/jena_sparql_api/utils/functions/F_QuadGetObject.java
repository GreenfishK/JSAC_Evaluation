package org.aksw.jena_sparql_api.utils.functions;

import com.google.common.base.Function;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;

public class F_QuadGetObject
    implements Function<Quad, Node>
{
    @Override
    public Node apply(Quad quad) {
        Node result = quad.getObject();
        return result;
    }

    public static final F_QuadGetObject fn = new F_QuadGetObject();
}
