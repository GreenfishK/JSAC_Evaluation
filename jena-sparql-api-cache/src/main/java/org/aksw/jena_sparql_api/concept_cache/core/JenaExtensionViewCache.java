package org.aksw.jena_sparql_api.concept_cache.core;

import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.engine.main.QC;

public class JenaExtensionViewCache {
    public static void register() {
        QC.setFactory(ARQ.getContext(), OpExecutorFactoryViewCache.get());
    }

}
