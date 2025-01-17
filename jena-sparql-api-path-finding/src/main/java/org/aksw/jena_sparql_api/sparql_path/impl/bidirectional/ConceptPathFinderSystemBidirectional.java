package org.aksw.jena_sparql_api.sparql_path.impl.bidirectional;

import org.aksw.jena_sparql_api.concepts.UnaryRelation;
import org.aksw.jena_sparql_api.sparql_path.api.ConceptPathFinder;
import org.aksw.jena_sparql_api.sparql_path.api.ConceptPathFinderBase;
import org.aksw.jena_sparql_api.sparql_path.api.ConceptPathFinderFactorySummaryBase;
import org.aksw.jena_sparql_api.sparql_path.api.ConceptPathFinderSystem;
import org.aksw.jena_sparql_api.sparql_path.api.PathSearch;
import org.aksw.jena_sparql_api.sparql_path.api.PathSearchSparqlBase;
import org.aksw.jena_sparql_api.sparql_path.core.PathConstraint2;
import org.aksw.jena_sparql_api.util.sparql.syntax.path.SimplePath;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.SparqlQueryConnection;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public class ConceptPathFinderSystemBidirectional
    //extends ConceptPathFinderFactorySummaryBase
    implements ConceptPathFinderSystem
{


    @Override
    public Single<Model> computeDataSummary(SparqlQueryConnection dataConnection) {
        return ConceptPathFinderBidirectionalUtils.createDefaultDataSummary(dataConnection);
    }

    @Override
    public ConceptPathFinderFactoryBidirectional<?> newPathFinderBuilder() {
        return new ConceptPathFinderFactoryBidirectional<>();
    }



    public static class ConceptPathFinderFactoryBidirectional<T extends ConceptPathFinderFactoryBidirectional<T>>
        extends ConceptPathFinderFactorySummaryBase<T>
    {

        // NOTE We could add more specific attributes here if we wanted

        @Override
        public ConceptPathFinder build() {
            return new ConceptPathFinderBase(dataSummary.getGraph(), dataConnection) {

                @Override
                public PathSearch<SimplePath> createSearch(UnaryRelation sourceConcept, UnaryRelation targetConcept) {
                    return new PathSearchSparqlBase(dataConnection, sourceConcept, targetConcept) {
                        @Override
                        public Flowable<SimplePath> execCore() {
                            Long effectiveLength = maxLength == null ? null : 2 * maxLength;

                            return ConceptPathFinderBidirectionalUtils
                                .findPathsCore(
                                        dataConnection,
                                        sourceConcept,
                                        targetConcept,
                                        maxResults,
                                        effectiveLength,
                                        dataSummary,
                                        shortestPathsOnly,
                                        simplePathsOnly,
                                        pathValidators,
                                        new PathConstraint2(),
                                        ConceptPathFinderBidirectionalUtils::convertGraphPathToSparqlPath);
                        }
                    };
                }
            };
        }
    }
}
