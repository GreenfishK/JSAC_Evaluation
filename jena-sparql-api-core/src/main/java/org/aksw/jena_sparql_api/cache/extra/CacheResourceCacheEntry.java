package org.aksw.jena_sparql_api.cache.extra;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;

import org.aksw.commons.util.StreamUtils;
import org.aksw.jena_sparql_api.core.ResultSetCloseable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.iterator.WrappedIterator;


/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 7/26/11
 *         Time: 3:46 PM
 */
public class CacheResourceCacheEntry
    implements CacheResource
{
    private static Logger logger = LoggerFactory.getLogger(CacheResourceCacheEntry.class);

    private CacheEntry cacheEntry;

    public CacheResourceCacheEntry(CacheEntry cacheEntry) {
        this.cacheEntry = cacheEntry;
    }


    /*
    @Override
    public InputStream open()  {
        try {
            return clob.getAsciiStream();
        } catch (SQLException e) {
            return null;
        }
    }*/

    /**
     * This class streams the result set.
     * Take care to close it. It auto-closes on consumption.
     *
     * @return
     */
    @Override
    public ResultSet asResultSet() {
        try {
            return _asResultSet();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public ResultSet _asResultSet()
            throws SQLException
    {
        final InputStream in = cacheEntry.getInputStream();
        ResultSet resultSet = ResultSetFactory.fromXML(in);
        ResultSetCloseable result = new ResultSetCloseable(resultSet, in);
        return result;
    }

    @Override
    public boolean isOutdated() {
        return System.currentTimeMillis() - cacheEntry.getTimestamp() > cacheEntry.getLifespan();
    }

    @Override
    public Model asModel(Model result) {
        try {
            return _asModel(result);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Model _asModel(Model result) throws SQLException {
        InputStream in = cacheEntry.getInputStream();

        /*
        ByteArrayInputStream tmp;
        try {
            String str = StreamUtils.toStringSafe(in);
            System.out.println("Content: ");
            System.out.println(str);
            tmp = new ByteArrayInputStream(str.getBytes());

        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        in = tmp;
        */

        result.read(in, null, "N-TRIPLES");
        close();
//        try {
//            in.close();
//        } catch (Exception e) {
//            logger.warn("Error", e);
//        }
        //cacheEntry.getInputStream().close();
        //close();

        return result;
    }

    @Override
    public boolean asBoolean() {
        try {
            return _asBoolean();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean _asBoolean() throws SQLException, IOException {
        // NOTE: This function closes the stream
        String str = StreamUtils.toString(cacheEntry.getInputStream());

        boolean result = Boolean.parseBoolean(str);

        //cacheEntry.getInputStream().close();
        //close();

        return result;
    }

    @Override
    public void close() {
        try {
            cacheEntry.getInputStream().close();
        } catch (IOException e) {
            //throw new RuntimeException(e);
            logger.warn("Error", e);
        }
    }


    @Override
    public Iterator<Triple> asIteratorTriples() {
        InputStream in = cacheEntry.getInputStream();

        // The iterator returned by jena closes the input stream when done
        // Also, it implements ClosableIterator
        Iterator<Triple> result = RDFDataMgr.createIteratorTriples(in, Lang.NTRIPLES, null);
        return result;
    }

}
