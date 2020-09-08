package org.aksw.jena_sparql_api.stmt;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.jena.shared.PrefixMapping;

public interface SparqlStmtParser
    extends Function<String, SparqlStmt>
{
    public static SparqlStmtParser wrapWithOptimizePrefixes(Function<String, SparqlStmt> delegate) {
        return str -> {
            SparqlStmt r = delegate.apply(str);
            SparqlStmtUtils.optimizePrefixes(r);
            return r;
        };
    }

    public static SparqlStmtParser wrapWithPostProcessor(Function<String, SparqlStmt> delegate, Consumer<SparqlStmt> postProcessor) {
        return str -> {
            SparqlStmt r = delegate.apply(str);
            postProcessor.accept(r);
            return r;
        };
    }

    public static SparqlStmtParser wrapWithTransform(Function<String, SparqlStmt> delegate, Function<? super SparqlStmt, ? extends SparqlStmt> transform) {
        return str -> {
            SparqlStmt before = delegate.apply(str);
            SparqlStmt after = transform.apply(before);
            return after;
        };
    }


    public static SparqlStmtParser wrapWithNamespaceTracking(PrefixMapping pm, Function<String, SparqlStmt> delegate) {
        return s -> {
            SparqlStmt r = delegate.apply(s);
            if(r.isParsed()) {
                PrefixMapping pm2 = null;
                if(r.isQuery()) {
                    pm2 = r.getAsQueryStmt().getQuery().getPrefixMapping();
                } else if(r.isUpdateRequest()) {
                    pm2 = r.getAsUpdateStmt().getUpdateRequest().getPrefixMapping();
                }

                if(pm2 != null) {
                    pm.setNsPrefixes(pm2);
                }
            }
            return r;
        };
    };

}
