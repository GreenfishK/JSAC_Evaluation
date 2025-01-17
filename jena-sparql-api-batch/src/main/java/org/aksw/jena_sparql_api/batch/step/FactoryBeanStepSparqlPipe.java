package org.aksw.jena_sparql_api.batch.step;


import org.aksw.jena_sparql_api.batch.reader.ItemReaderQuad;
import org.aksw.jena_sparql_api.batch.reader.PredicateQuadExpr;
import org.aksw.jena_sparql_api.batch.writer.ItemWriterQuad;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.UpdateExecutionFactory;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.batch.item.validator.ValidatingItemProcessor;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.base.Predicate;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.expr.Expr;


public class FactoryBeanStepSparqlPipe
    //extends AbstractFactoryBean<Step>
    extends FactoryBeanStepBase
{
//    protected StepBuilderFactory stepBuilders;


    //protected String name;
    protected int chunkSize = 1000;
    protected int readSize = 1000;
    protected Query query;
    protected boolean isDelete;

    protected QueryExecutionFactory source;
    protected UpdateExecutionFactory target;


    // A filter expression over the read triples
    protected Expr filter;


    public FactoryBeanStepSparqlPipe() {
        setSingleton(false);
    }

//    public StepBuilderFactory getStepBuilders() {
//        return stepBuilders;
//    }

//    @Autowired
//    public void setStepBuilders(StepBuilderFactory stepBuilders) {
//        this.stepBuilders = stepBuilders;
//    }

//    public String getName() {
//        return name;
//    }

    @Override
    public FactoryBeanStepSparqlPipe setName(String name) {
        super.setName(name);
        return this;
    }

    public FactoryBeanStepSparqlPipe setChunk(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    public int getReadSize() {
        return readSize;
    }

    public void setReadSize(int readSize) {
        this.readSize = readSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }


    public Query getQuery() {
        return query;
    }


    public void setQuery(Query query) {
        this.query = query;
    }


    public QueryExecutionFactory getSource() {
        return source;
    }


    public void setSource(QueryExecutionFactory source) {
        this.source = source;
    }


    public UpdateExecutionFactory getTarget() {
        return target;
    }


    public void setTarget(UpdateExecutionFactory target) {
        this.target = target;
    }

    public Expr getFilter() {
        return filter;
    }

    public void setFilter(Expr filter) {
        this.filter = filter;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public void setDelete(boolean isDelete) {
        this.isDelete = isDelete;
    }

//    @Override
//    public Step createInstance() throws Exception {
//
//        final Predicate<Quad> predicate = filter == null ? null : new PredicateQuadExpr(filter);
//
//        ItemReaderQuad reader = new ItemReaderQuad(source, query);
//        reader.setPageSize(readSize);
//
//        ItemProcessor<? super Quad, ? extends Quad> processor;
//        if(predicate != null) {
//            ValidatingItemProcessor<Quad> validatingProcessor = new ValidatingItemProcessor<Quad>();
//            validatingProcessor.setValidator(new Validator<Quad>() {
//                @Override
//                public void validate(Quad quad) throws ValidationException {
//                    boolean isValid = predicate.apply(quad);
//                    if(!isValid) {
//                        throw new ValidationException("A quad failed validation: " + quad);
//                    }
//                }
//            });
//            validatingProcessor.setFilter(true);
//            validatingProcessor.afterPropertiesSet();
//
//            processor = validatingProcessor;
//        } else {
//            processor = new PassThroughItemProcessor<Quad>();
//        }
//
//        //ItemProcessor<? super Quad, ? extends Quad> processor = new PassThroughItemProcessor<Quad>();
//        ItemWriterQuad writer = new ItemWriterQuad(target, isDelete);
//
//        reader.setPageSize(chunkSize);
//
//        //StepBuilderFactory stepBuilders = batchConfig.stepBuilders();
//        StepBuilder stepBuilder = stepBuilders.get(name);
//
//        AbstractTaskletStepBuilder<?> base = stepBuilder
//                .<Quad, Quad>chunk(chunkSize)
//                .reader(reader)
//                .processor(processor)
//                .writer(writer);
//
//        if(taskExecutor != null) {
//            base = base.taskExecutor(taskExecutor);
//
//            if(throttle != null) {
//                base = base.throttleLimit(throttle);
//            }
//        }
//
//        Step result = base.build();
//
//
////                .taskExecutor(taskExecutor)
////                .throttleLimit(throttleLimit)
////                .build();
//
//        return result;
//    }

//    @Override
//    public Class<?> getObjectType() {
//        return Step.class;
//    }

//    @StepScope
//    @Autowired


    @Override
    protected Step configureStep(StepBuilder stepBuilder) {

        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)((ConfigurableApplicationContext)ctx).getBeanFactory();
        RootBeanDefinition itemReaderBd = new RootBeanDefinition(ItemReaderQuad.class);
        itemReaderBd.setScope("step");
        itemReaderBd.getPropertyValues()
            .add("currentItemCount", "#{ stepExecutionContext[minValue] }")
            .add("maxItemCount", "#{ stepExecutionContext[maxValue] }")
            .add("pageSize", readSize)
            .add("qef", source)
            .add("query", query)
            ;

        String itemReaderName = BeanDefinitionReaderUtils.registerWithGeneratedName(itemReaderBd, beanFactory);

        BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(new BeanDefinitionHolder(itemReaderBd, itemReaderName), beanFactory, true);
        BeanDefinitionReaderUtils.registerBeanDefinition(proxyHolder, beanFactory);

        String proxyName = proxyHolder.getBeanName();
        @SuppressWarnings("unchecked")
        ItemReader<Quad> reader = (ItemReader<Quad>)beanFactory.getBean(proxyName);

        //BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(

//        ItemReaderQuad tmp = new ItemReaderQuad(source, query);
//        tmp.setQef(source);
//        tmp.setQuery(query);
//        tmp.setPageSize(readSize);
//
//        BeanDefinitionProxyUtils.createScopedProxy(beanFactory, bean, scopeName, propertyToSpel)
//        BeanDefinitionBuilder.genericBeanDefinition().addPropertyValue(name, value)


//        tmp.setCurrentItemCount(count);
//        tmp.getExecutionContextKey(key)

//        String proxyName = itemReaderName + "-proxy";
//        RootBeanDefinition proxyBd = new RootBeanDefinition(ScopedProxyFactoryBean.class);
//        proxyBd.getPropertyValues()
//            .add("targetBeanName", itemReaderName);
//        beanFactory.registerBeanDefinition(proxyName, proxyBd);

//        SimpleStepFactoryBean x;
//        RootBeanDefinition stepBd = new RootBeanDefinition(SimpleStepFactoryBean.class);
//        stepBd.getPropertyValues()
//            .add("commitInterval", chunkSize)
//            .add("itemReader", )
//            .add("itemProcessor", processor)
//            .add("writer", writer)
//            ;


        //SimpleStepFactoryBean<T, S>


//        ScopedProxyUtils.createScopedProxy(definition, registry, proxyTargetClass)
//        beanFactory.registerBeanDefinition(name + "itemReader", itemReaderBd);

        //Object bean = beanFactory.getBean("mytest");
        //Object bean = context.getBean("mytest");


        final Predicate<Quad> predicate = filter == null ? null : new PredicateQuadExpr(filter);

        //ItemReaderQuad reader = new ItemReaderQuad(source, query);
        //reader.setPageSize(readSize);

        ItemProcessor<? super Quad, ? extends Quad> processor;
        if(predicate != null) {
            ValidatingItemProcessor<Quad> validatingProcessor = new ValidatingItemProcessor<Quad>();
            validatingProcessor.setValidator(new ValidatorQuadByPredicate(predicate));
            validatingProcessor.setFilter(true);
            try {
                validatingProcessor.afterPropertiesSet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            processor = validatingProcessor;
        } else {
            processor = new PassThroughItemProcessor<Quad>();
        }

        //ItemProcessor<? super Quad, ? extends Quad> processor = new PassThroughItemProcessor<Quad>();
        ItemWriterQuad writer = new ItemWriterQuad(target, isDelete);

        //reader.setPageSize(chunkSize);


        //SimplePartitioner x

        Step result = stepBuilder
                .<Quad, Quad>chunk(chunkSize)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                //.throttleLimit(throttle)
                .build()
                ;




        if(throttle != null) {
            Step slaveStep = result;

            Partitioner partitioner = new PartitionerSparqlSlice(source, query);
            result = stepBuilder
                .partitioner(slaveStep)
                .partitioner(name, partitioner)
                .taskExecutor(taskExecutor)
                .gridSize(throttle)
                .build()
                ;


            //.partitioner(name + "-partitioner", partitioner).
        }

        return result;
    }
}
