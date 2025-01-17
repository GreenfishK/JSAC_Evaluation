package org.aksw.jena_sparql_api.utils;

import java.util.Iterator;

import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.binding.Binding;

/**
 * Wraps a result set as an iterator of bindings
 *
 * @author raven
 *
 */
public class IteratorResultSetBinding
    implements Iterator<Binding>
{
    private ResultSet rs;

    public IteratorResultSetBinding(ResultSet rs) {
        this.rs = rs;
    }

    @Override
    public boolean hasNext() {
        boolean result = rs.hasNext();
        return result;
    }

    @Override
    public Binding next() {
        Binding result = rs.nextBinding();
        return result;
    }

    @Override
    public void remove() {
        throw new RuntimeException("Operation not supported");
    }
    
    
    public static Iterator<Binding> wrap(ResultSet rs) {
    	return new IteratorResultSetBinding(rs);
    }
}