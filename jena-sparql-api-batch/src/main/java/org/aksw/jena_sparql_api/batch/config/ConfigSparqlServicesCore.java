package org.aksw.jena_sparql_api.batch.config;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.aksw.jena_sparql_api.core.DatasetListener;
import org.aksw.jena_sparql_api.core.SparqlService;
import org.aksw.jena_sparql_api.core.SparqlServiceFactory;
import org.aksw.jena_sparql_api.sparql.ext.http.HttpInterceptorRdfLogging;
import org.aksw.jena_sparql_api.update.DatasetListenerTrack;
import org.aksw.jena_sparql_api.update.SinkModelWriter;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class ConfigSparqlServicesCore {

//    @Bean
//    @Qualifier("tracking")
//    public SparqlService defaultTrackerStore(@Qualifier("config") SparqlServiceFactory ssf) {
//        //SparqlServiceFactory ssf = new SparqlServiceFactoryHttp();
//        SparqlService result = ssf.createSparqlService("http://localhost:8890/sparql", DatasetDescriptionUtils.createDefaultGraph("http://jsa.aksw.org/track/"), null);
//        return result;
//    }
//
    private Random random = new Random();

    @Bean
    @Autowired
    @Primary
    //@Qualifier("workflow")
    public SparqlServiceFactory workflowSparqlServiceFactory(@Qualifier("config") SparqlServiceFactory ssf, @Qualifier("tracking") SparqlService trackerService) {

        long jobInstanceId = random.nextLong();

        DatasetListenerTrack listener = new DatasetListenerTrack(trackerService);
        List<DatasetListener> listeners = Collections.<DatasetListener>singletonList(listener);

//        List<DatasetListener> listeners = Collections.<DatasetListener>singletonList(new DatasetListener() {
//            @Override
//            public void onPreModify(Diff<Set<Quad>> diff, UpdateContext updateContext) {
//                System.out.println(updateContext.getSparqlService().getDatasetDescription());
//            }
//        });

//        SparqlServiceFactory result = FluentSparqlServiceFactory.from(ssf).config()
//            .withUpdateListeners(new UpdateStrategyEventSource(), listeners)
//            .end()
//            .create();

        SparqlServiceFactory result = ssf;

//        DatasetListenerSink
//        //Fluent
//        ChangeSetMetadata metadata = new ChangeSetMetadata("claus", "testing");
//        SparqlServiceFactoryEventSource result = new SparqlServiceFactoryEventSource(ssf);
//        SinkChangeSetWriter sink = new SinkChangeSetWriter(metadata, ssfChangeSet);
//        result.getListeners().add(new DatasetListenerSink(sink));


        return result;
    }

//    @Bean
//    @Qualifier("logging")
//    public SparqlService defaultLoggerStore(SparqlServiceFactory ssf) {
//        SparqlService result = ssf.createSparqlService("http://localhost:8890/sparql", DatasetDescriptionUtils.createDefaultGraph("http://jsa.aksw.org/log/"), null);
//        return result;
//    }

    @Bean
    @Autowired
    public Supplier<HttpClient> httpClientSupplier(@Qualifier("logging") SparqlService sparqlService) {
        SinkModelWriter sink = new SinkModelWriter(sparqlService);

        HttpInterceptorRdfLogging logger = new HttpInterceptorRdfLogging(sink);


        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(100);
        connManager.setDefaultMaxPerRoute(100);

        HttpClient httpClient = HttpClientBuilder.create()
        	.setConnectionManager(connManager)
        	.addInterceptorLast((HttpRequestInterceptor)logger)
        	.addInterceptorLast((HttpResponseInterceptor)logger)
        	.build();

        // TODO This sets the httpClient globally, which is actually not desired
        //HttpOp.setDefaultHttpClient(httpClient);
        //HttpOp.setUseDefaultClientWithAuthentication(true);

        Supplier<HttpClient> result = () -> httpClient;

        return result;
    }


}
