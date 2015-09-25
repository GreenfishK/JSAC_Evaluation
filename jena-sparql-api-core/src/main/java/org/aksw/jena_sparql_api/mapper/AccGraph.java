package org.aksw.jena_sparql_api.mapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.hp.hpl.jena.sparql.syntax.Template;

public class AccGraph
    implements Acc<Graph>
{
    private Graph graph;
    private Template template;

    public AccGraph(Template template) {
       this(GraphFactory.createDefaultGraph(), template);
    }

    public AccGraph(Graph graph, Template template) {
        super();
        this.graph = graph;
        this.template = template;
    }

    @Override
    public void accumulate(Binding binding) {
        Set<Triple> triples = new HashSet<Triple>();
        Map<Node, Node> bNodeMap = new HashMap<Node, Node>();
        template.subst(triples, bNodeMap, binding);

        for(Triple triple : triples) {
            graph.add(triple);
        }
    }

    @Override
    public Graph getValue() {
        return graph;
    }

}