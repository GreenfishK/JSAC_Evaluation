package org.aksw.jena_sparql_api.concepts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.aksw.commons.collections.SetUtils;
import org.aksw.jena_sparql_api.stmt.SparqlPrologueParser;
import org.aksw.jena_sparql_api.stmt.SparqlPrologueParserImpl;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.utils.ElementUtils;
import org.aksw.jena_sparql_api.utils.GeneratorBlacklist;
import org.aksw.jena_sparql_api.utils.VarUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.Syntax;
import org.apache.jena.sdb.core.Gensym;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.graph.NodeTransform;
import org.apache.jena.sparql.lang.ParserSPARQL11;
import org.apache.jena.sparql.lang.SPARQLParser;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.PatternVars;

import com.google.common.collect.Sets;

/**
 * A concept combines a SPARQL graph pattern (element) with a variable.
 *
 * NOTE: For concept parsing, rather use SparqlConceptParser than the static methods on this class.
 *
 * @author raven
 *
 */
public class Concept
	implements UnaryRelation
{
    private Element element;//List<Element> elements;
    private Var var;

    /**
     * There are several ways how top/bottom could be represented.
     * Clients attempting to indicate top/bottom should attempt to ensure
     * reference equality with these constants
     */
    public static final Concept TOP = Concept.create("", "s");

    /**
     * There are several ways how top/bottom could be represented.
     * Clients attempting to indicate top/bottom should attempt to ensure
     * reference equality with these constants
     */
    public static final Concept BOTTOM = Concept.create("", "s");

    /**
     * Util method to parse strings that use a pipe as a separator between variable and sparql string
     * ?s | ?s a ex:Airport
     *
     * @param str
     * @return
     */
    public static Concept parse(String str) {
        Concept result = parse(str, null);
        return result;
    }

    public static Concept parse(String str, PrefixMapping pm) {
        String[] splits = str.split("\\|", 2);
        if(splits.length != 2) {
            throw new RuntimeException("Invalid string: " + str);

        }

        // Remove leading ? of the varName
        String varName = splits[0].trim();
        if(varName.charAt(0) != '?') {
            throw new RuntimeException("var name must start with '?'");
        }
        varName = varName.substring(1);

        Concept result = create(splits[1], varName, pm);
        return result;
    }


    public static Concept create(String prologueStr, String varName, String elementStr) {
        SparqlQueryParser queryParser = SparqlQueryParserImpl.create(Syntax.syntaxSPARQL_10);

        Concept result = create(prologueStr, varName, elementStr, queryParser);
        return result;
    }

    public static Concept create(String prologueStr, String varName, String elementStr, Function<String, Query> queryParser) {
        //SparqlElementParser elementParser = new SparqlElementParserImpl(queryParser);
        SparqlPrologueParser prologueParser = new SparqlPrologueParserImpl(queryParser);

        Prologue prologue = prologueParser.apply(prologueStr);
        PrefixMapping prefixMapping = prologue.getPrefixMapping();

        Concept result = create(elementStr, varName, prefixMapping);

        return result;
    }


    // TODO Var first
    public static Concept create(String elementStr, String varName) {
        Concept result = create(elementStr, varName, (PrefixMapping)null);
        return result;
    }

    // TODO Var first
    public static Concept create(String elementStr, String varName, PrefixMapping prefixMapping) {
        Var var = Var.alloc(varName);

        Element element = parseElement(elementStr, prefixMapping);
        //Element element = ParserSPARQL10.parseElement(tmp);

        //Element element = ParserSPARQL11.parseElement(tmp);

        // TODO Find a generic flatten routine
        if(element instanceof ElementGroup) {
            ElementGroup group = (ElementGroup)element;
            List<Element> elements = group.getElements();
            if(elements.size() == 1) {
                element = elements.get(0);
            }
        }

        Concept result = new Concept(element, var);

        return result;
    }
    
    public static Element parseElement(String elementStr, PrefixMapping prefixMapping) {
        String tmp = elementStr.trim();
        boolean isEnclosed = tmp.startsWith("{") && tmp.endsWith("}");
        if(!isEnclosed) {
            tmp = "{" + tmp + "}";
        }

        //ParserSparql10 p;
        tmp = "SELECT * " + tmp;

        Query query = new Query();
        query.setPrefixMapping(prefixMapping);
        // TODO Make parser configurable
        SPARQLParser parser = new ParserSPARQL11();
        parser.parse(query, tmp);
        Element result = query.getQueryPattern();

        return result;
    }

    public Concept applyNodeTransform(NodeTransform nodeTransform) {
        Var tmpVar = (Var)nodeTransform.apply(var);

        Element e = ElementUtils.applyNodeTransform(element, nodeTransform);
        Var v = tmpVar == null ? var : tmpVar;

        Concept result = new Concept(e, v);
        return result;
    }

    public Set<Var> getVarsMentioned() {
        Set<Var> result = SetUtils.asSet(PatternVars.vars(element));
        result.add(var); // Var should always be part of element - but better add it here explicitly
        return result;
    }

    public Concept(Element element, Var var) {
        super();
        this.element = element;
        this.var = var;
    }

    public Concept(List<Element> elements, Var var) {
        ElementGroup group = new ElementGroup();

        for(Element item : elements) {
            if(item instanceof ElementTriplesBlock) {
                ElementTriplesBlock tmp = (ElementTriplesBlock)item;
                for(Triple t : tmp.getPattern()) {
                    group.addTriplePattern(t);
                }
            } else {
                group.addElement(item);
            }
        }

        this.element = group;
        this.var = var;
    }


    public Element getElement() {
        return element;
    }

    public List<Element> getElements() {
        return ElementUtils.toElementList(element);
    }

    public Var getVar() {
        return var;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((element == null) ? 0 : element.hashCode());
        result = prime * result + ((var == null) ? 0 : var.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Concept other = (Concept) obj;
        if (element == null) {
            if (other.element != null)
                return false;
        } else if (!element.equals(other.element))
            return false;
        if (var == null) {
            if (other.var != null)
                return false;
        } else if (!var.equals(other.var))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Concept [element=" + element + ", var=" + var + "]";
    }
}
