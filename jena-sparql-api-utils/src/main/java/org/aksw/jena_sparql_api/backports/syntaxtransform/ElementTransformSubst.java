/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aksw.jena_sparql_api.backports.syntaxtransform;

import java.util.Map ;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Variable;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.graph.NodeTransform;
import com.hp.hpl.jena.sparql.graph.NodeTransformLib;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;

/** An {@link ElementTransform} which replaces occurences of a variable with a Node value. 
 * Because a {@link Var} is a subclass of {@link Node_Variable} which is a {@link Node},
 * this includes variable renaming.
 * <p>
 * This is a transformation on the syntax - all occurences of a variable are replaced, even if
 * inside sub-select's and not project (which means it is effectively a different variable).
 */
public class ElementTransformSubst extends ElementTransformCopyBase {
    private final NodeTransform nodeTransform ;

    public ElementTransformSubst(Map<Var, Node> mapping) {
        this(new NodeTransformSubst(mapping)) ;
    }

    public ElementTransformSubst(NodeTransform nodeTransform) {
        this.nodeTransform = nodeTransform ;
    }

    @Override
    public Element transform(ElementFilter el, Expr expr2) {
        Expr expr = NodeTransformLib.transform(nodeTransform, expr2);

        Element result = expr.equals(expr2)
            ? el
            : new ElementFilter(expr) ;
        
        return result;
    }
    

    @Override
    public Element transform(ElementTriplesBlock el) {
        ElementTriplesBlock etb = new ElementTriplesBlock() ;
        boolean changed = false ;
        for (Triple t : el.getPattern()) {
            Triple t2 = transform(t) ;
            changed = changed || t != t2 ;
            etb.addTriple(t2) ;
        }
        if ( changed )
            return etb ;
        return el ;
    }

    @Override
    public Element transform(ElementPathBlock el) {
        ElementPathBlock epb = new ElementPathBlock() ;
        boolean changed = false ;
        for (TriplePath p : el.getPattern()) {
            TriplePath p2 = transform(p) ;
            changed = changed || p != p2 ;
            epb.addTriplePath(p2) ;
        }
        if ( changed )
            return epb ;
        return el ;
    }

    private TriplePath transform(TriplePath path) {
        Node s = path.getSubject() ;
        Node s1 = transform(s) ;
        Node o = path.getObject() ;
        Node o1 = transform(o) ;

        if ( path.isTriple() ) {
            Node p = path.getPredicate() ;
            Node p1 = transform(p) ;
            if ( s == s1 && p == p1 && o == o1 )
                return path ;
            return new TriplePath(Triple.create(s1, p1, o1)) ;
        }
        if ( s == s1 && o == o1 )
            return path ;
        return new TriplePath(s1, path.getPath(), o1) ;
    }

    private Triple transform(Triple triple) {
        Node s = triple.getSubject() ;
        Node s1 = transform(s) ;
        Node p = triple.getPredicate() ;
        Node p1 = transform(p) ;
        Node o = triple.getObject() ;
        Node o1 = transform(o) ;

        if ( s == s1 && p == p1 && o == o1 )
            return triple ;
        return Triple.create(s1, p1, o1) ;
    }

    private Node transform(Node n) {
        return nodeTransform.convert(n) ;
    }
}