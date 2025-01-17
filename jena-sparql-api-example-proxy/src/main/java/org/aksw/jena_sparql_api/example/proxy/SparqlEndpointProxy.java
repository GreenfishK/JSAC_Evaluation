package org.aksw.jena_sparql_api.example.proxy;

import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUpdate;
import org.aksw.jena_sparql_api.utils.UriUtils;
import org.aksw.jena_sparql_api.web.servlets.SparqlEndpointBase;

import com.google.common.collect.Multimap;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.update.UpdateProcessor;



@Path("/sparql")
public class SparqlEndpointProxy
    extends SparqlEndpointBase
{
    private String defaultServiceUri;
    private boolean allowOverrideServiceUri = false;

    public SparqlEndpointProxy(@Context ServletContext context) {

        this.defaultServiceUri = (String)context.getAttribute("defaultServiceUri");

        Boolean tmp = (Boolean)context.getAttribute("allowOverrideServiceUri");
        this.allowOverrideServiceUri = tmp == null ? true : tmp;

        if(!allowOverrideServiceUri && (defaultServiceUri == null || defaultServiceUri.isEmpty()) ) {
            throw new RuntimeException("Overriding of service URI disabled, but no default URI set.");
        }
    }

    @Override
    public QueryExecution createQueryExecution(final Query query, @Context HttpServletRequest req) {

        Multimap<String, String> qs = UriUtils.parseQueryString(req.getQueryString());

        Collection<String> serviceUris = qs.get("service-uri");
        String serviceUri;
        if(serviceUris == null || serviceUris.isEmpty()) {
            serviceUri = defaultServiceUri;
        } else {
            serviceUri = serviceUris.iterator().next();

            // If overriding is disabled, a given uri must match the default one
            if(!allowOverrideServiceUri && !defaultServiceUri.equals(serviceUri)) {
                throw new RuntimeException("Access to any service other than " + defaultServiceUri + " is blocked.");
            }
        }

        if(serviceUri == null) {
            throw new RuntimeException("No SPARQL service URI sent with the request and no default one is configured");
        }


        QueryExecutionFactory qef = new QueryExecutionFactoryHttp(serviceUri);
        QueryExecution result = qef.createQueryExecution(query);

        return result;
    }

    @Override
    public UpdateProcessor createUpdateProcessor(SparqlStmtUpdate stmt) {
        throw new RuntimeException("Updates not supported");
    }
}

