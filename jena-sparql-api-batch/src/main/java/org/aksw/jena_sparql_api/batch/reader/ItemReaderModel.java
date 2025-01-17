package org.aksw.jena_sparql_api.batch.reader;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.jena_sparql_api.concepts.Concept;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.lookup.MapService;
import org.aksw.jena_sparql_api.lookup.MapServiceUtils;
import org.aksw.jena_sparql_api.mapper.MappedConcept;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;


/**
 * Item reader that reads a SPARQL SELECT query using pagination
 *
 * @author raven
 *
 * @param <T>
 */
public class ItemReaderModel
    extends AbstractPaginatedDataItemReader<Entry<Resource, Model>>
{
    private Concept concept;
    private MapService<Concept, Resource, Model> listService;

    public ItemReaderModel(MapService<Concept, Resource, Model> listService, Concept concept) {
        setName(this.getClass().getName());
        this.listService = listService;
        this.concept = concept;
    }

    @Override
    protected Iterator<Entry<Resource, Model>> doPageRead() {
        long limit = (long)this.pageSize;
        long offset = this.page * this.pageSize;

        Map<Resource, Model> map = listService.fetchData(concept, limit, offset);
        Iterator<Entry<Resource, Model>> result = map.entrySet().iterator();
        return result;
    }
}
