package org.aksw.jena_sparql_api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCache;
import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.Cache;
import org.aksw.jena_sparql_api.cache.extra.CacheBackend;
import org.aksw.jena_sparql_api.cache.extra.CacheCore;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontendImpl;
import org.aksw.jena_sparql_api.cache.extra.CacheImpl;
import org.aksw.jena_sparql_api.cache.h2.CacheCoreH2;
import org.aksw.jena_sparql_api.cache.staging.CacheBackendDao;
import org.aksw.jena_sparql_api.cache.staging.CacheBackendDaoPostgres;
import org.aksw.jena_sparql_api.cache.staging.CacheBackendDataSource;
import org.aksw.jena_sparql_api.compare.QueryExecutionCompare;
import org.aksw.jena_sparql_api.compare.QueryExecutionFactoryCompare;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.jena_sparql_api.retry.core.QueryExecutionFactoryRetry;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class QueryCallable
    implements Callable<Integer>
{
    private static final Logger logger = LoggerFactory
            .getLogger(QueryCallable.class);

    private int nLoops;
    private int nResources;
    private Random rand;
    private QueryExecutionFactoryCompare qef;

    public QueryCallable(int nLoops, Random rand, int nResources, QueryExecutionFactoryCompare qef) {
        this.nLoops = nLoops;
        this.rand = rand;
        this.nResources = nResources;
        this.qef = qef;
    }

    @Override
    public Integer call() {
        logger.debug("Starting query runner");
        for(int i = 0; i < nLoops; ++i) {
            int id = rand.nextInt(nResources);
            String queryStr = SparqlTest.createTestQueryString(id);
            QueryExecutionCompare qe = qef.createQueryExecution(queryStr);
            ResultSet rs = qe.execSelect();
            if(qe.isDifference()) {
                throw new RuntimeException("Dammit - difference in output");
            }
            ResultSetFormatter.consume(rs);
        }
        logger.debug("Stopping query runner");

        return 0;
    }
}

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 7/27/11
 *         Time: 12:27 AM
 */
public class SparqlTest {


    private static final Logger logger = LoggerFactory
            .getLogger(SparqlTest.class);

//    @BeforeClass
//    public static void setUp() {
//        PropertyConfigurator.configure("log4j.properties");
//    }

    public static final String prefix = "http://example.org/resource/item";

    public static Model createTestModel(int n) {
        Model result = ModelFactory.createDefaultModel();

        for(int i = 0; i < n; ++i) {
            Resource s = result.createResource(prefix + i);
            result.add(s, RDF.type, OWL.Thing);
        }

        return result;
    }


    public static String createTestQueryString(int i) {
        String result = "SELECT * { <" + prefix + i + "> ?p ?o }"; //a <http://www.w3.org/2002/07/owl#Thing> }";
        return result;
    }



    @Test
    public void testMultiThreaded() throws InterruptedException, ClassNotFoundException, SQLException, IOException {
        int nThreads = 4;
        int nResources = 50;
        int nLoops = 100;


        Model model = createTestModel(nResources);
        QueryExecutionFactory qefBase = new QueryExecutionFactoryModel(model);

        QueryExecutionFactory qef = qefBase;

//        QueryExecutionFactory qef2 = new QueryExecutionFactoryModel(model);
//        QueryExecutionFactory qef3 = new QueryExecutionFactoryModel(model);


        qef = new QueryExecutionFactoryRetry(qef, 5, 1);

        // Add delay in order to be nice to the remote server (delay in milli seconds)
        //qef = new QueryExecutionFactoryDelay(qef, 1);

        // Set up a cache
        // Cache entries are valid for 1 day
        long timeToLive = 24l * 60l * 60l * 1000l;

        // This creates a 'cache' folder, with a database file named 'sparql.db'
        // Technical note: the cacheBackend's purpose is to only deal with streams,
        // whereas the frontend interfaces with higher level classes - i.e. ResultSet and Model

        Class.forName("org.h2.Driver");

//        JdbcDataSource dataSource = new JdbcDataSource();
//        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_ON_EXIT=FALSE");
//        dataSource.setUser("sa");
//        dataSource.setPassword("sa");
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        String schemaResourceName = "/org/aksw/jena_sparql_api/cache/cache-schema-pgsql.sql";
        InputStream in = SparqlTest.class.getResourceAsStream(schemaResourceName);

        if(in == null) {
            throw new RuntimeException("Failed to load resource: " + schemaResourceName);
        }

        InputStreamReader reader = new InputStreamReader(in);
        Connection conn = dataSource.getConnection();
        try {
            RunScript.execute(conn, reader);
        } finally {
            conn.close();
        }


        CacheBackendDao dao = new CacheBackendDaoPostgres();
        CacheBackend cacheBackend = new CacheBackendDataSource(dataSource, dao);
        CacheFrontend cacheFrontend = new CacheFrontendImpl(cacheBackend);
        qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);

//
//        // Add pagination
        qef = new QueryExecutionFactoryPaginated(qef, 900);


        QueryExecutionFactoryCompare qefCompare = new QueryExecutionFactoryCompare(qef, qefBase);



        ExecutorService executors = Executors.newFixedThreadPool(nThreads);

        Random rand = new Random();

        Collection<Callable<Integer>> callables = new ArrayList<>();
        for(int i = 0; i < nThreads; ++i) {
            Callable<Integer> callable = new QueryCallable(nLoops, rand, nResources, qefCompare);
            callables.add(callable);
        }

        List<Future<Integer>> futures = executors.invokeAll(callables);
        executors.shutdown();
        executors.awaitTermination(20, TimeUnit.SECONDS);

        for(Future<Integer> future : futures) {
            try {
                future.get();
            } catch(Exception e) {
                logger.error("Test case failed: ", e);
            }
        }
    }


    public QueryExecutionFactory createService() {
        String service = "http://dbpedia.org/sparql";
        List<String> defaultGraphNames = Arrays.asList("http://dbpedia.org");
        QueryExecutionFactory f = new QueryExecutionFactoryHttp(service, defaultGraphNames);

        return f;
    }

    //@Test
    public void testHttp() {
        String service = "http://dbpedia.org/sparql";
        List<String> defaultGraphNames = Arrays.asList("http://dbpedia.org");
        QueryExecutionFactory f = new QueryExecutionFactoryHttp(service, defaultGraphNames);

        assertEquals("http://dbpedia.org", f.getState());
        assertEquals("http://dbpedia.org/sparql", f.getId());

        QueryExecution qe = f.createQueryExecution("Select * {?s ?p ?o .} limit 3");
        ResultSet rs = qe.execSelect();
        //System.out.println(ResultSetFormatter.asText(rs));
    }

    //@Test
    public void testHttpDelay() {
        QueryExecutionFactory f = createService();

        long delay = 5000;
        f = new QueryExecutionFactoryDelay(f, delay);

        long start = System.currentTimeMillis();

        ResultSetFormatter.consume(f.createQueryExecution("Select * {?s ?p ?o .} limit 3").execSelect());
        ResultSetFormatter.consume(f.createQueryExecution("Select * {?s ?p ?o .} limit 3").execSelect());

        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed > 0.9f * delay);

    }

    //@Test
    public void testPagination() {
        //System.out.println("Starting testPagination");

        Model model = ModelFactory.createDefaultModel();
        model.add(RDF.type, RDF.type, RDF.type);
        model.add(RDF.List, RDF.type, RDF.List);

        QueryExecutionFactory f = new QueryExecutionFactoryModel(model);


        //QueryExecutionFactory f = createService();
        f = new QueryExecutionFactoryDelay(f, 5000);


        f = new QueryExecutionFactoryPaginated(f, 1);

        QueryExecution q = f.createQueryExecution("Select * {?s ?p ?o}");
        ResultSet rs = q.execSelect();
        ResultSetFormatter.consume(rs);
//        while(rs.hasNext()) {
            //System.out.println("Here");
            //System.out.println(rs.next());
//        }

    }


    @Test
    public void testPaginationSelectConstruct() {

        //System.out.println("Starting testPagination");

        Model model = ModelFactory.createDefaultModel();
        model.add(RDF.type, RDF.type, RDF.type);
        model.add(RDF.List, RDF.type, RDF.List);
        model.add(RDF.Seq, RDF.type, RDF.Seq);

        QueryExecutionFactory f = new QueryExecutionFactoryModel(model);


        //QueryExecutionFactory f = createService();
        //f = new QueryExecutionFactoryDelay(f, 5000);


        f = new QueryExecutionFactoryPaginated(f, 1);

        String queryString = "Construct { ?s a ?o } { ?s a ?o }";

        Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL_11);

        QueryExecution q = f.createQueryExecution(queryString);
        Model result = q.execConstruct();

//        model.write(System.out, "N-TRIPLES");
//        System.out.println("Blah");
//        result.write(System.out, "N-TRIPLES");
        //assertEquals(model, result);
    }

    //@Test
    public void testPaginationSelectComplex() {
        System.out.println("Starting testPagination");

        Model model = ModelFactory.createDefaultModel();
        model.add(RDF.type, RDF.type, RDF.type);
        model.add(RDF.List, RDF.type, RDF.List);

        QueryExecutionFactory f = new QueryExecutionFactoryModel(model);


        //QueryExecutionFactory f = createService();
        //f = new QueryExecutionFactoryDelay(f, 5000);


        f = new QueryExecutionFactoryPaginated(f, 1);

        String queryString = "SELECT ?p (COUNT(?s) AS ?count) WHERE {?s ?p ?o. {SELECT ?s ?o WHERE {?s a ?o.} } }";

        Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL_11);

        QueryExecution q = f.createQueryExecution(queryString);
        ResultSet rs = q.execSelect();
        while(rs.hasNext()) {
            System.out.println("Here");
            System.out.println(rs.next());
        }




        /*
        String query = String.format(queryTemplate, propertyToDescribe, limit, offset);
Map<ObjectProperty, Integer> result = new HashMap<ObjectProperty, Integer>();
ObjectProperty prop;
Integer oldCnt;
boolean repeat = true;
QueryExecutionFactory f = new QueryExecutionFactoryHttp(ks.getEndpoint().getURL().toString(), ks.getEndpoint().getDefaultGraphURIs());
f = new QueryExecutionFactoryPaginated(f, limit);
QueryExecution exec = f.createQueryExecution(QueryFactory.create(query, Syntax.syntaxARQ));
ResultSet rs = exec.execSelect();
int i = 0;
QuerySolution qs;
while(rs.hasNext() && ++i <= maxFetchedRows){
qs = rs.next();
prop = new ObjectProperty(qs.getResource("p").getURI());
int newCnt = qs.getLiteral("count").getInt();
oldCnt = result.get(prop);
if(oldCnt == null){
oldCnt = Integer.valueOf(newCnt);
}
result.put(prop, oldCnt);
qs.getLiteral("count").getInt();
}
*/
    }


    //@Test
    public void testHttpDelayCache()
        throws Exception
    {
        //PropertyConfigurator.configure("log4j.properties");

        /*
        System.out.println(Integer.toHexString(0));

        if(true) {
            System.exit(666);
        }*/

        Model model = ModelFactory.createDefaultModel();
        model.add(RDF.type, RDF.type, RDF.type);
        model.add(RDF.List, RDF.type, RDF.List);

        QueryExecutionFactory f = new QueryExecutionFactoryModel(model);

        long delay = 50;
        //f = new QueryExecutionFactoryDelay(f, delay);

        CacheCore core = CacheCoreH2.create("unittest-1", 10);
        Cache cache = new CacheImpl(core);
        f = new QueryExecutionFactoryCache(f, cache);


        Thread[] threads = new Thread[] {
                new QueryThread(f, "Select * {?s ?p ?o .}", true),
                new QueryThread(f, "Select * {?s a ?o .}", true)
        };

        for(Thread thread : threads) {
            thread.start();
        }

        Thread.sleep(10000);

        for(Thread thread : threads) {
            thread.interrupt();
        }

        Thread.sleep(1000);
        System.exit(0);


        /*
        ResultSet rs = f.createQueryExecution("Select * {?s ?p ?o .} limit 3").execSelect();
        ResultSetFormatter.outputAsCSV(System.out, rs);

        rs = f.createQueryExecution("Select * {?s ?p ?o .} limit 3").execSelect();
        ResultSetFormatter.outputAsCSV(System.out, rs);

        Model ma = f.createQueryExecution("Construct {?s ?p ?o } {?s ?p ?o .} limit 3").execConstruct();
        ma.write(System.out, "N-TRIPLES");


        Model mb = f.createQueryExecution("Construct {?s ?p ?o } {?s ?p ?o .} limit 3").execConstruct();
        mb.write(System.out, "N-TRIPLES");
*/
    }

    @Test
    public void testHttpDelayCachePagination() {
        // TBD
    }
}



class QueryThread
    extends Thread
{
    private String queryString;
    private QueryExecutionFactory factory;
    private boolean queryType;

    private boolean isCancelled = false;

    public QueryThread(QueryExecutionFactory factory, String queryString, boolean queryType) {
        this.factory = factory;
        this.queryString = queryString;
        this.queryType = queryType;
    }

    @Override
    public void interrupt() {
        this.isCancelled = true;
        super.interrupt();
    }

    @Override
    public void run() {
        while(!isCancelled) {
            QueryExecution qe = factory.createQueryExecution(queryString);

            if(queryType == true) {
                ResultSet rs = qe.execSelect();
                ResultSetFormatter.consume(rs);
            } else {
                Model m = qe.execConstruct();
            }
        }
    }
}
