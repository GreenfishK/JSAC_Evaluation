package org.aksw.jena_sparql_api.mapper.proxy;

import org.aksw.jena_sparql_api.mapper.annotation.DefaultValue;
import org.aksw.jena_sparql_api.mapper.annotation.Iri;
import org.aksw.jena_sparql_api.mapper.proxy.function.FunctionBinder;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.util.ExprUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestFunctionGenerator {
	
	@Test
	public void test() throws Exception {
		FunctionBinder binder = new FunctionBinder();		
		binder.register(TestFunctionGenerator.class.getMethod("myFn", String.class, int.class, float.class));
		
		NodeValue nv = ExprUtils.eval(ExprUtils.parse("<urn:test>('world', 2)"));
		String str = nv.asUnquotedString();
		
		Assert.assertEquals("hello world - 2 - 3.14", str);
	}
	
	@Iri("urn:test")
	public static String myFn(String arg, @DefaultValue("1") int intVal, @DefaultValue("3.14") float floatVal) {
		return "hello " + arg + " - " + intVal + " - " + floatVal;
	}
}
