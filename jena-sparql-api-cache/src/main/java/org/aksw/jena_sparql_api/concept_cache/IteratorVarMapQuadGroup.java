package org.aksw.jena_sparql_api.concept_cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aksw.commons.collections.MapUtils;

import com.google.common.collect.AbstractIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.core.Var;

class IteratorVarMapQuadGroup
    extends AbstractIterator<Map<Var, Var>>
{
    private List<Quad> sourceQuads;
    private List<Quad> targetQuads;

    private Map<Var, Var> baseSolution;

    private CombinatoricsVector combi;

    private List<Map<Var, Var>> partialSolutions;

    private int nextInc;

    public IteratorVarMapQuadGroup(List<Quad> sourceQuads, List<Quad> targetQuads, Map<Var, Var> baseSolution) {

        this.sourceQuads = sourceQuads;
        this.targetQuads = targetQuads;
        this.baseSolution = baseSolution;

        this.combi = new CombinatoricsVector(targetQuads.size(), sourceQuads.size());

        this.partialSolutions = new ArrayList<Map<Var, Var>>(sourceQuads.size());

        update(0);
    }

    // Returns true if a solution was generated
    private boolean update(int i) {
        int[] vector = combi.vector();

        for(; i < vector.length; ++i) {

            Map<Var, Var> priorSolution = i > 0 ? partialSolutions.get(i - 1) : baseSolution;

            Quad sourceQuad = sourceQuads.get(i);
            int targetIndex = vector[i];
            Quad targetQuad = targetQuads.get(targetIndex);

            Map<Var, Var> nextSolution = validate(sourceQuad, targetQuad, priorSolution);
            if(nextSolution == null) {
                break;
            }

            partialSolutions.add(nextSolution);
        }

        nextInc = combi.nextIndex(i - 1);

        boolean result = i == vector.length;
        return result;
    }

    private boolean inc() {
        combi.inc(nextInc);

        boolean result;
        if(nextInc >= 0) {
            //System.out.println(nextInc);
            while(partialSolutions.size() > nextInc) {
                partialSolutions.remove(partialSolutions.size() - 1);
            }

            result = update(nextInc);

        } else {
            result = false;
        }

        return result;
    }

    public static Map<Var, Var> validate(Quad sourceQuad, Quad targetQuad, Map<Var, Var> partialSolution) {
        //boolean result = false;

        Map<Var, Var> result = Utils2.createVarMap(sourceQuad, targetQuad);
        if(result != null) {
            boolean isCompatible = MapUtils.isPartiallyCompatible(result, partialSolution);
            if(isCompatible) {
                result.putAll(partialSolution);
            }
        }

        return result;
    }

    @Override
    protected Map<Var, Var> computeNext() {
        // Check if we are at a solution
        while(combi.vector() != null && partialSolutions.size() != sourceQuads.size()) {
            inc();
        }

        Map<Var, Var> result;
        if(combi.vector() == null) {
            result = this.endOfData();
        } else {

            result = partialSolutions.get(partialSolutions.size() - 1);
            inc();
        }

        return result;
    }


    public static Iterator<Map<Var, Var>> create(QuadGroup quadGroup, Map<Var, Var> baseSolution) {
        Iterator<Map<Var, Var>> result = new IteratorVarMapQuadGroup(new ArrayList<Quad>(quadGroup.getCandQuads()), new ArrayList<Quad>(quadGroup.getQueryQuads()), baseSolution);
        return result;
    }
}