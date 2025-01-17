package org.aksw.jena_sparql_api.batch.step;

import java.util.List;
import java.util.Map.Entry;

import org.aksw.commons.collections.diff.Diff;
import org.aksw.jena_sparql_api.batch.processor.ItemProcessorModifierDatasetGraphDiff;
import org.aksw.jena_sparql_api.batch.reader.ItemReaderDatasetGraph;
import org.aksw.jena_sparql_api.batch.writer.ItemWriterSparqlDiff;
import org.aksw.jena_sparql_api.concepts.Concept;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.SparqlService;
import org.aksw.jena_sparql_api.core.UpdateExecutionFactory;
import org.aksw.jena_sparql_api.core.connection.SparqlQueryConnectionJsa;
import org.aksw.jena_sparql_api.hop.Hop;
import org.aksw.jena_sparql_api.hop.MapServiceHop;
import org.aksw.jena_sparql_api.lookup.MapService;
import org.aksw.jena_sparql_api.modifier.Modifier;
import org.aksw.jena_sparql_api.modifier.ModifierList;
import org.aksw.jena_sparql_api.shape.ResourceShape;
import org.aksw.jena_sparql_api.shape.ResourceShapeParser;
import org.aksw.jena_sparql_api.shape.lookup.MapServiceResourceShapeDataset;
import org.aksw.jena_sparql_api.stmt.SparqlUpdateParser;
import org.apache.jena.graph.Node;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.sparql.core.DatasetGraph;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;


public class FactoryBeanStepSparqlDiff
    extends AbstractFactoryBean<Step>
{
    //protected AbstractBatchConfiguration batchConfig;
    protected StepBuilderFactory stepBuilders;



//	@Autowired
//	protected StepBuilder stepBuilder;

//	public FactoryBeanStepSparqlDiff(StepBuilder stepBuilder) {
//		this.stepBuilder = stepBuilder;
//	}

    protected ResourceShape shape;
//
    protected Hop hop;

//    protected Object shape;

    protected String name;
    //protected TODO How to represent the shape?
    protected int chunkSize;
    protected SparqlQueryConnection sourceQef;
    protected Concept concept;
    //protected ListService<>

    protected SparqlUpdateParser updateParser;

    @Autowired
    protected ResourceShapeParser shapeParser;

    protected List<Modifier<? super DatasetGraph>> modifiers;
    //protected List<?> modifiers;


    protected UpdateExecutionFactory targetUef;

    public FactoryBeanStepSparqlDiff() {
        setSingleton(false);
    }

//	@Autowired
//	public FactoryBeanStepSparqlDiff(StepBuilderFactory stepBuilders) {
//		setSingleton(false);
//		this.stepBuilders = stepBuilders;
//	}


    @Autowired
    public void setStepBuilders(StepBuilderFactory stepBuilders) {
        this.stepBuilders = stepBuilders;
    }


    @Autowired
    public void setSparqlUpdateParser(SparqlUpdateParser updateParser) {
        this.updateParser = updateParser;
    }

//	@Autowired
//	public void setBatchConfig(AbstractBatchConfiguration batchConfig) {
//		try {
//			this.stepBuilders = batchConfig.stepBuilders();
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}


    public FactoryBeanStepSparqlDiff setChunk(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

//    public FactoryBeanStepSparqlDiff setShape(Object shape) {
//        this.shape = shape;
//        return this;
//    }

    public FactoryBeanStepSparqlDiff setShape(ResourceShape shape) {
        this.shape = shape;
        return this;
    }
//
    public FactoryBeanStepSparqlDiff setHop(Hop hop) {
        this.hop = hop;
        return this;
    }


//    public FactoryBeanStepSparqlDiff setModifier(Modifier<? super DatasetGraph> modifier) {
//        this.modifier = modifier;
//        return this;
//    }

    public FactoryBeanStepSparqlDiff setModifiers(List<Modifier<? super DatasetGraph>> modifiers) {
        this.modifiers = modifiers;
        return this;
    }

//    public FactoryBeanStepSparqlDiff setModifiers(List<?> modifiers) {
//        this.modifiers = modifiers;
//        return this;
//    }

//	public FactoryBeanStepSparqlDiff setModifier(Concept concept) {
//		this.concept = concept;
//		return this;
//	}

    public FactoryBeanStepSparqlDiff setName(String name) {
        this.name = name;
        return this;
    }

    public FactoryBeanStepSparqlDiff setSource(QueryExecutionFactory sourceQef) {
        setSource(new SparqlQueryConnectionJsa(sourceQef));
        return this;
    }

    public FactoryBeanStepSparqlDiff setSource(SparqlQueryConnection sourceQef) {
        this.sourceQef = sourceQef;
        return this;
    }

    public FactoryBeanStepSparqlDiff setConcept(Concept concept) {
        this.concept = concept;
        return this;
    }


    public FactoryBeanStepSparqlDiff setTarget(UpdateExecutionFactory targetUef) {
        this.targetUef = targetUef;
        return this;
    }


    public FactoryBeanStepSparqlDiff setService(SparqlService sparqlService) {
        setSource(sparqlService.getQueryExecutionFactory());
        setTarget(sparqlService.getUpdateExecutionFactory());
        return this;
    }


//	public StepBuilder getStepBuilder() {
//		return stepBuilder;
//	}

//    public ResourceShape getShape() {
//        return shape;
//    }

    public String getName() {
        return name;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public SparqlQueryConnection getSource() {
        return sourceQef;
    }

    public Concept getConcept() {
        return concept;
    }

    public List<Modifier<? super DatasetGraph>> getModifiers() {
        return modifiers;
    }

    public UpdateExecutionFactory getTarget() {
        return targetUef;
    }

    public MapService<Concept, Node, DatasetGraph> createListService() {

        //Hop hop = (shape instanceof Hop) ? (Hop)shape : null;
        //ResourceShape sh = (shape instanceof ResourceShape) ? (ResourceShape)shape : null;
        ResourceShape sh = shape;

        MapService<Concept, Node, DatasetGraph> result;
        if(hop != null) {
             result = new MapServiceHop(sourceQef, hop, 30);
        } else if(sh != null) {
            result = new MapServiceResourceShapeDataset(sourceQef, sh, true);
        } else {
            throw new RuntimeException("No shape provided");
        }

        return result;
    }

    @Override
    public Step createInstance() throws Exception {
        Modifier<DatasetGraph> modifier = ModifierList.<DatasetGraph>create(modifiers);


        MapService<Concept, Node, DatasetGraph> listService = createListService();
        //ItemReader<Entry<Node, DatasetGraph>> itemReader = new ItemReaderDatasetGraph(listService, concept);
        ItemReaderDatasetGraph itemReader = new ItemReaderDatasetGraph(listService, concept);
        ItemProcessor<Entry<? extends Node, ? extends DatasetGraph>, Entry<Node, Diff<DatasetGraph>>> itemProcessor = new ItemProcessorModifierDatasetGraphDiff(modifier);
        ItemWriter<Entry<? extends Node, ? extends Diff<? extends DatasetGraph>>> itemWriter = new ItemWriterSparqlDiff(targetUef);

        itemReader.setPageSize(chunkSize);

        //StepBuilderFactory stepBuilders = batchConfig.stepBuilders();
        StepBuilder stepBuilder = stepBuilders.get(name);

        Step result = stepBuilder
                .<Entry<Node, DatasetGraph>, Entry<Node, Diff<DatasetGraph>>>chunk(chunkSize)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();

        return result;
    }

    @Override
    public Class<?> getObjectType() {
        return Step.class;
    }
}
