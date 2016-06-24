package org.aksw.jena_sparql_api.concept_cache.combinatorics;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.isomorphism.Problem;
import org.aksw.jena_sparql_api.utils.CnfUtils;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;

public class ProblemFactoryOpMapping
//    implements Problem<Map<Var, Var>>
{

    public static Problem<Map<Var, Var>> create(Op a, Op b) {
        if(a != null && b != null) {
            Class<?> ac = a.getClass();
            Class<?> bc = b.getClass();


        }
        return null;
    }

    public static Problem<Map<Var, Var>> create(OpQuadPattern a, OpQuadPattern b, Map<Var, Var> baseSolution) {
        List<Quad> aqp = a.getPattern().getList();
        List<Quad> bqp = b.getPattern().getList();
        Problem<Map<Var, Var>> result = new ProblemVarMappingQuad(aqp, bqp, baseSolution);
        return result;
    }

//    public static Problem<Map<Var, Var>> create(OpFilter a, OpFilter b, Map<Var, Var> baseSolution) {
//        Set<Set<Expr>> acnf = CnfUtils.toSetCnf(a.getExprs());
//        Set<Set<Expr>> bcnf = CnfUtils.toSetCnf(b.getExprs());
//        //List<Quad> aqp = a.getPattern().getList();
//        //List<Quad> bqp = b.getPattern().getList();
//        Problem<Map<Var, Var>> result = new ProblemVarMappingExpr(acnf, bcnf, baseSolution);
//        return result;
//    }
//





//    public static Stream<Map<Var, Var>> match(OpProject a, OpProject b) {
//
//    }


}
