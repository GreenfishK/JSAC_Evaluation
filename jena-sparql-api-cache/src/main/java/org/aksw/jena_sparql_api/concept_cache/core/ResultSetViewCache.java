package org.aksw.jena_sparql_api.concept_cache.core;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.jena_sparql_api.concept_cache.dirty.SparqlViewCache;
import org.aksw.jena_sparql_api.concept_cache.dirty.IteratorResultSetBinding;
import org.aksw.jena_sparql_api.concept_cache.domain.ProjectedQuadFilterPattern;
import org.aksw.jena_sparql_api.concept_cache.domain.QuadFilterPattern;
import org.aksw.jena_sparql_api.utils.ResultSetUtils;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;

import com.google.common.collect.Iterators;

public class ResultSetViewCache {


    /**
     * Reads the first threshold bindings from the given result set, and attempts to cache them,
     * unless the result set turns out to be too large.
     *
     * Returns a new result together with a flag of whether (true) or not (false) caching was performed.
     *
     *
     * @param physicalRs
     * @param indexVars
     * @param indexResultSetSizeThreshold
     * @param conceptMap
     * @param pqfp
     * @return
     */
    public static Entry<ResultSet, Boolean> cacheResultSet(ResultSet physicalRs, Set<Var> indexVars, long indexResultSetSizeThreshold, SparqlViewCache conceptMap, ProjectedQuadFilterPattern pqfp) {

        ResultSet resultRs;
        //ResultSet physicalRs = decoratee.execSelect();
        List<String> varNames = physicalRs.getResultVars();

        List<Binding> bindings = new ArrayList<Binding>();

        int i;
        for(i = 0; i < indexResultSetSizeThreshold && physicalRs.hasNext(); ++i) {
            Binding binding = physicalRs.nextBinding();
            bindings.add(binding);
        }

        //boolean exceededThreshold = i >= indexResultSetSizeThreshold;
        boolean isCacheable = i <= indexResultSetSizeThreshold;

        if(isCacheable) {
            //it = bindings.iterator();
            ResultSet tmp = new ResultSetStream(varNames, null, bindings.iterator());

            resultRs = ResultSetFactory.copyResults(tmp);

            QuadFilterPattern qfp = pqfp.getQuadFilterPattern();
            ResultSet cacheRs = ResultSetUtils.project(resultRs, indexVars, true);
            conceptMap.index(qfp, cacheRs);
        } else {
            // TODO Resource leak if the physicalRs is not consumed - fix that somehow!
            Iterator<Binding> it = Iterators.concat(bindings.iterator(), new IteratorResultSetBinding(physicalRs));
            resultRs = new ResultSetStream(varNames, null, it);
        }

        Entry<ResultSet, Boolean> result = new SimpleEntry<>(resultRs, isCacheable);

        return result;
    }

}
