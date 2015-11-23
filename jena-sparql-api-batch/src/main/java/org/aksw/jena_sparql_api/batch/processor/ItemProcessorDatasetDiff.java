package org.aksw.jena_sparql_api.batch.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.commons.collections.SetUtils;
import org.aksw.commons.collections.diff.Diff;
import org.aksw.jena_sparql_api.modifier.Modifier;
import org.aksw.jena_sparql_api.utils.SetDatasetGraph;
import org.springframework.batch.item.ItemProcessor;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import com.hp.hpl.jena.sparql.core.Quad;

public class ItemProcessorDatasetDiff
	implements ItemProcessor<Entry<Resource, Dataset>, Diff<Set<Quad>>>
{
	private Modifier<Dataset> modifier;

	public ItemProcessorDatasetDiff(Modifier<Dataset> modifier) {
		this.modifier = modifier;
	}

	@Override
	public Diff<Set<Quad>> process(Entry<Resource, Dataset> entry) throws Exception {
		Dataset base = entry.getValue();
		DatasetGraph baseGraph = base.asDatasetGraph();
		DatasetGraph cloneGraph = DatasetGraphFactory.createMem();
		Iterator<Quad> it = baseGraph.find();
		while(it.hasNext()) {
			Quad q = it.next();
			cloneGraph.add(q);
		}
		Dataset clone = DatasetFactory.create(cloneGraph);

		modifier.apply(clone);

		Set<Quad> baseQuads = SetDatasetGraph.wrap(base.asDatasetGraph());
		Set<Quad> cloneQuads = SetDatasetGraph.wrap(clone.asDatasetGraph());

		//Diff<Set<Quad>>
		Diff<Set<Quad>> result = createDiff(baseQuads, cloneQuads);

		return result;
	}

	public static <T> Diff<Set<T>> createDiff(Collection<T> before, Collection<T> after) {
		Set<T> x = SetUtils.asSet(before);
		Set<T> y = SetUtils.asSet(after);

		Set<T> added = new HashSet<T>(Sets.difference(x, y));
		Set<T> removed = new HashSet<T>(Sets.difference(y, x));
		Diff<Set<T>> result = Diff.create(added, removed);
		return result;
	}
}