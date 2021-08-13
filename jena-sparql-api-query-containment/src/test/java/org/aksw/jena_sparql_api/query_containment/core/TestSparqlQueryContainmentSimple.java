package org.aksw.jena_sparql_api.query_containment.core;

import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.aksw.jena_sparql_api.query_containment.index.ResidualMatching;
import org.aksw.jena_sparql_api.query_containment.index.SparqlQueryContainmentIndex;
import org.aksw.jena_sparql_api.query_containment.index.SparqlQueryContainmentIndexImpl;
import org.aksw.jena_sparql_api.query_containment.index.SparqlTreeMapping;
import org.apache.derby.tools.sysinfo;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.junit.Test;

public class TestSparqlQueryContainmentSimple {

    /*
    Notes:
    Order by clause seems not to be supported.
    Aliases in select clause not supported.
    Aliases in bind not supported.
    Sequence paths are not supported.

    */

    private final String url_test_dir = System.getProperty("user.dir") + "/src/test/java/org/aksw/jena_sparql_api/query_containment/core";

    @Test
    public void testOptionalWhereClause() throws IOException {
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__optional_where_clause_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__optional_where_clause_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    @Test
    public void testRDFTypePredicate() throws IOException {
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__rdf_type_predicate_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__rdf_type_predicate_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    @Test
    public void testLeaveOutSubjectInTripleStatements() throws IOException {
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__leave_out_subject_in_triple_statements_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__leave_out_subject_in_triple_statements_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    @Test
    public void testOrderOfTripleStatements() throws IOException {
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__order_of_triple_statements_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__order_of_triple_statements_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    @Test
    public void testAliasViaBind() throws IOException {
        // failed with original query
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__alias_via_bind_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__alias_via_bind_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    @Test
    public void testVariableNames() throws IOException {
        // failed with original query
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__variable_names_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__variable_names_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    @Test
    public void testVariablesNotBound() throws IOException {
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__variables_not_bound_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__variables_not_bound_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    @Test
    public void testInvertedPaths() throws IOException {
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__inverted_paths_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__inverted_paths_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    @Test
    public void testSequencePaths() throws IOException {
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__sequence_paths_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__sequence_paths_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    @Test
    public void testPrefixAlias() throws IOException {
        String vStr = Files.readString(Path.of(url_test_dir + "/test_normalization__prefix_alias_alt1.txt"));
        String qStr = Files.readString(Path.of(url_test_dir + "/test_normalization__prefix_alias_alt2.txt"));
        printOutQueryContainments(vStr, qStr);
        printOutQueryContainments(qStr, vStr);
    }

    public static void printOutQueryContainments(String vStr, String qStr) {
        System.out.println("Lookup with " + qStr);
        Query v = QueryFactory.create(vStr, Syntax.syntaxSPARQL_10);
        Query q = QueryFactory.create(qStr, Syntax.syntaxSPARQL_10);

        Op vOp = Algebra.compile(v);
        Op qOp = Algebra.compile(q);

        // Insert the query in the '(v)iew' role into the index and perform a
        // lookup with the one in the '(q)uery/request/prototype' role.
        SparqlQueryContainmentIndex<String, ResidualMatching> index = SparqlQueryContainmentIndexImpl.create();
        index.put("v", vOp);

        Stream<Entry<String, SparqlTreeMapping<ResidualMatching>>> candidates = index.match(qOp);

        System.out.println("Start of containment mappings");
        candidates.forEach(e -> {
            String key = e.getKey();
            SparqlTreeMapping<ResidualMatching> mapping = e.getValue();

            System.out.println("Obtained the following mappings for index entry with key " + key + ": ");
            System.out.println(mapping);

            System.out.println("Normalized index entry algebra expression:");
            System.out.println(index.get(key));
            ResidualMatching rm = mapping.getNodeMappings().get(
                    mapping.getaTree().getRoot(), mapping.getbTree());
            System.out.println(rm);
        });
        System.out.println("End of containment mappings");

        boolean isContained = SparqlQueryContainmentUtils.tryMatch(v, q);
        System.out.println("Is contained: " + isContained);
    }



}



