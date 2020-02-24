package org.aksw.jena_sparql_api.rx;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.utils.DatasetGraphUtils;
import org.aksw.jena_sparql_api.utils.DatasetUtils;
import org.aksw.jena_sparql_api.utils.IteratorClosable;
import org.aksw.jena_sparql_api.utils.QuadPatternUtils;
import org.aksw.jena_sparql_api.utils.QuadUtils;
import org.apache.jena.atlas.iterator.IteratorResourceClosing;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.ext.com.google.common.collect.Iterators;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.lang.PipedQuadsStream;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.RiotParsers;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.util.iterator.ClosableIterator;

import com.github.davidmoten.rx2.flowable.Transformers;
import com.google.common.collect.Lists;

import io.reactivex.Flowable;

/**
 * Reactive extensions of RDFDataMgr
 * 
 * @author Claus Stadler, Nov 12, 2018
 *
 */
public class RDFDataMgrRx {

    /**
     * TODO Adapt for createIteratorTriples
     * 
     * Adaption from RDFDataMgr.createIteratorQuads that waits for data on the input stream
     * indefinitely
     * 
     * Creates an iterator over parsing of quads
     * @param input Input Stream
     * @param lang Language
     * @param baseIRI Base IRI
     * @return Iterator over the quads
     */
    public static Iterator<Quad> createIteratorQuads(
    		InputStream input,
    		Lang lang,
    		String baseIRI,
    		int bufferSize, boolean fair, int pollTimeout, int maxPolls) {
        // Special case N-Quads, because the RIOT reader has a pull interface
        if ( RDFLanguages.sameLang(RDFLanguages.NQUADS, lang) ) {
            return new IteratorResourceClosing<>(
                RiotParsers.createIteratorNQuads(input, null, RiotLib.dftProfile()),
                input);
        }
        // Otherwise, we have to spin up a thread to deal with it
        final PipedRDFIterator<Quad> it = new PipedRDFIterator<>(bufferSize, fair, pollTimeout, maxPolls);
        final PipedQuadsStream out = new PipedQuadsStream(it);

        Thread t = new Thread(()-> parseFromInputStream(out, input, baseIRI, lang, null)) ;
        t.start();
        return it;
    }
	
    
    public static void parseFromInputStream(StreamRDF destination, InputStream in, String baseUri, Lang lang, Context context) {
        RDFParser.create()
            .source(in)
            .base(baseUri)
            .lang(lang)
            .context(context)
            .parse(destination);
    }

	public static Flowable<Quad> createFlowableQuads(Callable<InputStream> inSupplier, Lang lang, String baseIRI) {
		return createFlowableFromInputStream(inSupplier, in -> RDFDataMgr.createIteratorQuads(in, lang, baseIRI));
	}

	public static Flowable<Triple> createFlowableTriples(Callable<InputStream> inSupplier, Lang lang, String baseIRI) {
		return createFlowableFromInputStream(inSupplier, in -> RDFDataMgr.createIteratorTriples(in, lang, baseIRI));
	}

	
	/**
	 * Creates resources by grouping consecutive quads with the same graph into a Model,
	 * and then returning a resource for that graph IRI.
	 * 
	 * {@code
	 * GRAPH :s {
	 *   :s :p :o .
	 *   :o :x :y
	 * }
	 * }
	 * 
	 * 
	 * @param inSupplier
	 * @param lang
	 * @param baseIRI
	 * @return
	 */
	public static Flowable<Resource> createFlowableResources(Callable<InputStream> inSupplier, Lang lang, String baseIRI) {
		Flowable<Resource> result = createFlowableQuads(inSupplier, lang, baseIRI)	
			.compose(Transformers.<Quad>toListWhile(
		            (list, t) -> list.isEmpty() 
		                         || list.get(0).getGraph().equals(t.getGraph())))
			.map(list -> list.stream().map(RDFDataMgrRx::decodeDistinguished).collect(Collectors.toList()))
			.map(QuadPatternUtils::createResourceFromQuads);

		return result;
	}
	
	public static final String DISTINGUISHED_PREFIX = "x-distinguished:";
	public static final int DISTINGUISHED_PREFIX_LENGTH = DISTINGUISHED_PREFIX.length();
	
	public static Node encodeDistinguished(Node g) {
		Node result = g;
		if(g.isURI()) {
			String str = DISTINGUISHED_PREFIX + g.getURI();
			result = NodeFactory.createURI(str);
		}
		return result;
	}

	public static Quad encodeDistinguished(Quad quad) {
		Node g = quad.getGraph();
		Node encoded = encodeDistinguished(g);
		Quad result = encoded == g ? quad : new Quad(encoded, quad.asTriple());

		return result;
	}

	public static Node decodeDistinguished(Node g) {
		Node result = g;
		if(g.isURI()) {
			String str = g.getURI();
			if(str.startsWith(DISTINGUISHED_PREFIX)) {
				result = NodeFactory.createURI(str.substring(DISTINGUISHED_PREFIX_LENGTH));
			}
		}
		return result;
	}
	
	public static Quad decodeDistinguished(Quad quad) {
		Quad result = quad;
		Node g = quad.getGraph();
		if(g.isURI()) {
			String str = g.getURI();
			if(str.startsWith(DISTINGUISHED_PREFIX)) {
				result = new Quad(NodeFactory.createURI(str.substring(DISTINGUISHED_PREFIX_LENGTH)), quad.asTriple());
			}
		}
		return result;
	}
	
	/**
	 * Groups consecutive quads with the same graph yeld by createFlowableQuads into datasets 
	 * 
	 * @param inSupplier
	 * @param lang
	 * @param baseIRI
	 * @return
	 */
	public static Flowable<Dataset> createFlowableDatasets(Callable<InputStream> inSupplier, Lang lang, String baseIRI) {
		Flowable<Dataset> result = createFlowableQuads(inSupplier, lang, baseIRI)		
			.compose(Transformers.<Quad>toListWhile(
		            (list, t) -> list.isEmpty() 
		                         || list.get(0).getGraph().equals(t.getGraph())))
			.map(DatasetGraphQuadsImpl::create)
			.map(DatasetFactory::wrap);

		return result;
	}
	
	public static Flowable<Dataset> createFlowableDatasets(Callable<TypedInputStream> inSupplier) {
		
		Flowable<Dataset> result = createFlowableFromInputStream(inSupplier,
				in -> createIteratorQuads(
						in,
						RDFLanguages.contentTypeToLang(in.getContentType()),
						in.getBaseURI(),
						PipedRDFIterator.DEFAULT_BUFFER_SIZE,
						false,
						PipedRDFIterator.DEFAULT_POLL_TIMEOUT,
						Integer.MAX_VALUE
						))
		.compose(Transformers.<Quad>toListWhile(
	            (list, t) -> list.isEmpty() 
	                         || list.get(0).getGraph().equals(t.getGraph())))
		.map(DatasetGraphQuadsImpl::create)
		.map(DatasetFactory::wrap);

		return result;
	}
	
	public static <T, I extends InputStream> Flowable<T> createFlowableFromInputStream(Callable<I> inSupplier, Function<? super I, ? extends Iterator<T>> fn) {
		Flowable<T> result = Flowable.generate(
				() -> {
					I in = inSupplier.call();
					Iterator<T> it = fn.apply(in);
					return new IteratorClosable<>(it, () -> {
						// Try to close the iterator 'it'
						// Otherwise, forcefully close the stream
						// (may cause a (usually/hopefully) harmless exception)
						try {
							if(it instanceof Closeable) {
					            ((Closeable)it).close();
							} else if (it instanceof org.apache.jena.atlas.lib.Closeable) {
					            ((org.apache.jena.atlas.lib.Closeable)it).close();								
							} else {
								try {
									in.close();
								} finally {
									// Consume any remaining items in the iterator to prevent blocking issues
									// For example, Jena's producer thread can get blocked
									// when parsed items are not consumed
									Iterators.size(it);
								}
							}
						} finally {
							// Close the backing input stream in any case
							in.close();
						}
					});
				},
				(reader, emitter) -> {
					if(reader.hasNext()) {
						T item = reader.next();
						emitter.onNext(item);
					} else {
						emitter.onComplete();
					}
				},
				ClosableIterator::close);
		return result;
	}

	public static void writeResources(Flowable<? extends Resource> flowable, Path file, RDFFormat format) throws Exception {
		writeDatasets(flowable.map(DatasetUtils::createFromResource), file, format);
	}

	public static void writeResources(Flowable<? extends Resource> flowable, OutputStream out, RDFFormat format) throws Exception {
		writeDatasets(flowable.map(DatasetUtils::createFromResource), out, format);
	}
	
	
//	static class State {
//		Path targetPath;
//		Path tmpPath;
//		OutputStream out;
//	}
//	interface Sink {
//		
//	}
//	
//    public static <T> FlowableTransformer<T, T> cache(Path file, BiConsumer<T, OutputStream> serializer) {
//    	
//    	State[] state = {null};
//    	return f -> f
//    			.doOnSubscribe(s -> state[0] = new State())
//    			.doOnNext(item -> serializer.accept(state[0].out, item))
//    			.doOnCancel(onCancel)
//    		;
//    	
//    	
////    	return f ->
////          f.doOnSubscrible // open the stream
////    		f.doOnNext() // write the item
////    	    f.onCancel() // delete the file
////          f.onError // delete the file
////    	    f.onComplete // close the stream, copy the file to the final location
//    		
////            final BiPredicate<? super List<T>, ? super T> condition, boolean emitRemainder) {
////        return collectWhile(ListFactoryHolder.<T>factory(), ListFactoryHolder.<T>add(), condition, emitRemainder);
//    }
	
	
	/**
	 * An encoder that renames a graph if it is encountered on successive encoding requests.
	 * 
	 * @author raven
	 *
	 */
	public static class QuadEncoderDistinguish {
		protected Set<Node> priorGraphs = Collections.emptySet();

		public synchronized Dataset encode(Dataset dataset) {
			Set<Node> now = Sets.newHashSet(dataset.asDatasetGraph().listGraphNodes());
			List<Quad> quads = Lists.newArrayList(dataset.asDatasetGraph().find());
			
			Set<Node> conflicts = Sets.intersection(priorGraphs, now);
			List<Quad> newQuads = quads.stream()
					.map(q -> conflicts.contains(q.getGraph()) ? encodeDistinguished(q) : q)
					.collect(Collectors.toList());
			
			priorGraphs = now.stream()
				.map(n -> conflicts.contains(n) ? encodeDistinguished(n) : n)
				.collect(Collectors.toSet());
			
			Dataset result = DatasetFactory.wrap(DatasetGraphQuadsImpl.create(newQuads));
			return result;
			// Rename all graphs in the intersection
		}
	}

	public static class QuadEncoderMerge {
		protected Map<Node, Set<Quad>> pending = new LinkedHashMap<>();

		public synchronized Dataset accept(Dataset dataset) {
			Supplier<Set<Quad>> setSupplier = LinkedHashSet::new; 
			
			Iterator<Quad> it = dataset.asDatasetGraph().find();
			Map<Node, Set<Quad>> index = QuadUtils.partitionByGraph(
					it,
					new LinkedHashMap<Node, Set<Quad>>(),
					setSupplier);
			
			Set<Node> before = pending.keySet();
			Set<Node> now = index.keySet();
			
			Set<Node> overlap = Sets.intersection(now, before);
			Set<Node> newGraphs = Sets.difference(now, before);
//					readyGraphs,
//					Sets.difference(now, before));

			for(Node appending : overlap) {
				Set<Quad> tgt = pending.get(appending);
				Set<Quad> src = index.get(appending);
				tgt.addAll(src);
			}
			
			for(Node newGraph : newGraphs) {
				Set<Quad> src = index.get(newGraph);
				pending.put(newGraph, src);
			}

			// Emit the ready graphs
			Dataset result = DatasetFactory.create();
			Set<Node> readyGraphs = new HashSet<>(Sets.difference(before, now));

			for(Node ready : readyGraphs) {
				Set<Quad> quads = pending.get(ready);
				
				DatasetGraphUtils.addAll(result.asDatasetGraph(), quads);
				
				pending.remove(ready);
			}
			
			//System.err.println("Pending size " + pending..size());
			
			return result;
		}
		
		public Dataset getPendingDataset() {
			Dataset result = DatasetFactory.create();
			for(Collection<Quad> quads : pending.values()) {
				DatasetGraphUtils.addAll(result.asDatasetGraph(), quads);
			}

			return result;
		}
	}	
	public static class QuadEncoderMergeOld {
		protected Dataset pending = DatasetFactory.create();

		public synchronized Dataset accept(Dataset dataset) {
			Set<Node> before = Sets.newHashSet(pending.asDatasetGraph().listGraphNodes());
			Set<Node> now = Sets.newHashSet(dataset.asDatasetGraph().listGraphNodes());
			
			Set<Node> readyGraphs = Sets.difference(before, now);
			Set<Node> appendings = Sets.union(
					Sets.intersection(before, now),
					Sets.difference(now, before));

			for(Node appending : appendings) {
				Graph tgt = pending.asDatasetGraph().getGraph(appending);
				Graph src = dataset.asDatasetGraph().getGraph(appending);
				
				GraphUtil.addInto(tgt, src);
			}

			// Emit the ready graphs
			Dataset result = DatasetFactory.create();
			for(Node ready : readyGraphs) {
				Graph src = pending.asDatasetGraph().getGraph(ready);
				DatasetGraphUtils.addAll(result.asDatasetGraph(), ready, src);
				
				pending.asDatasetGraph().removeGraph(ready);
			}
			
			System.err.println("Pending size " + pending.asDatasetGraph().size());
			
			return result;
		}
		
		public Dataset getPendingDataset() {
			return pending;
		}
	}
	// A better approach would be to transform a flowable to write to a file as a side effect
	// Upon flowable completion, copy the file to its final location
	public static void writeDatasets(Flowable<? extends Dataset> flowable, Path file, RDFFormat format) throws Exception {
		try(OutputStream out = new FileOutputStream(file.toFile())) {
			writeDatasets(flowable, out, format);
		}
	}

	// Does not close the stream
	public static void writeDatasets(Flowable<? extends Dataset> flowable, OutputStream out, RDFFormat format) throws Exception {
		try {
			QuadEncoderDistinguish encoder = new QuadEncoderDistinguish();
			flowable
			// Flush every 1000 graphs
			.buffer(1000)
			.blockingForEach(items -> {
				for(Dataset item : items) {
					Dataset encoded = encoder.encode(item);
					RDFDataMgr.write(out, encoded, format);
				}
				out.flush();
			});
		} finally {
			out.flush();
		}
	}

}
