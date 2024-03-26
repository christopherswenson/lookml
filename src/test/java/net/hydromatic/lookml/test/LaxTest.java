/*
 * Licensed to the LookML Authors under one or more contributor
 * license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.
 * The LookML Authors license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.  You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package net.hydromatic.lookml.test;

import net.hydromatic.lookml.LaxHandlers;
import net.hydromatic.lookml.LookmlSchema;
import net.hydromatic.lookml.LookmlSchemas;
import net.hydromatic.lookml.MiniLookml;
import net.hydromatic.lookml.ObjectHandler;
import net.hydromatic.lookml.SchemaLookml;
import net.hydromatic.lookml.parse.LookmlParsers;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests for the LookML event-based parser. */
public class LaxTest {

  private static void generateSampleEvents(ObjectHandler h) {
    h.obj("model", "m", h1 ->
            h1.number("n", 1)
                .string("s", "hello")
                .identifier("b", "true")
                .code("code", "VALUES 1")
                .list("list", h2 ->
                    h2.identifier("asc")
                        .number(-2.5)
                        .string("abc")
                        .list(h3 -> h3.string("singleton")))
                .list("emptyList", h2 -> {}))
        .close();
  }

  private static void assertParse(String s, Matcher<List<String>> matcher) {
    final ParseFixture.Parsed f = ParseFixture.of().parse(s);
    assertThat(f.list, matcher);
  }

  private static void assertParseThrows(String s, Matcher<Throwable> matcher) {
    try {
      final ParseFixture.Parsed f = ParseFixture.of().parse(s);
      fail("expected error, got " + f.list);
    } catch (RuntimeException e) {
      assertThat(e, matcher);
    }
  }

  /** Tests the LookML writer
   * {@link LaxHandlers#writer(StringBuilder, int, boolean)}
   * by running a sequence of parse events through the writer and checking
   * the generated LookML string. */
  @Test void testWriter() {
    final StringBuilder b = new StringBuilder();
    generateSampleEvents(LaxHandlers.writer(b, 2, true));
    final String lookml = "model: m {\n"
        + "  n: 1\n"
        + "  s: \"hello\"\n"
        + "  b: true\n"
        + "  code: VALUES 1;;\n"
        + "  list: [\n"
        + "    asc,\n"
        + "    -2.5,\n"
        + "    \"abc\",\n"
        + "    [\n"
        + "      \"singleton\"\n"
        + "    ]\n"
        + "  ]\n"
        + "  emptyList: []\n"
        + "}";
    assertThat(b, hasToString(lookml));

    // Same as previous, in non-pretty mode
    b.setLength(0);
    generateSampleEvents(LaxHandlers.writer(b, 2, false));
    final String lookml2 = "model:m {"
        + "n:1, s:\"hello\", b:true, code:VALUES 1;;, "
        + "list:[asc, -2.5, \"abc\", [\"singleton\"]], "
        + "emptyList:[]"
        + "}";
    assertThat(b, hasToString(lookml2));
  }

  /** Tests the logging handler
   * {@link LaxHandlers#logger}
   * by running a sequence of parser events through it and checking the
   * resulting list of strings. */
  @Test void testLogger() {
    final List<String> list = new ArrayList<>();
    generateSampleEvents(LaxHandlers.logger(list::add));
    assertThat(list,
        hasToString("[objOpen(model, m), "
            + "number(n, 1), "
            + "string(s, hello), "
            + "identifier(b, true), "
            + "code(code, VALUES 1), "
            + "listOpen(list), "
            + "identifier(asc), "
            + "number(-2.5), "
            + "string(abc), "
            + "listOpen(), "
            + "string(singleton), "
            + "listClose(), "
            + "listClose(), "
            + "listOpen(emptyList), "
            + "listClose(), "
            + "objClose()]"));
  }

  /** Tests that the same events come out of a filter handler
   * ({@link LaxHandlers#filter(ObjectHandler)}) as go into it. */
  @Test void testFilterHandler() {
    final List<String> list = new ArrayList<>();
    generateSampleEvents(LaxHandlers.logger(list::add));
    final List<String> list2 = new ArrayList<>();
    generateSampleEvents(LaxHandlers.filter(LaxHandlers.logger(list2::add)));
    assertThat(list2, hasSize(list.size()));
    assertThat(list2, hasToString(list.toString()));
  }

  @Test void testParse() {
    assertParse("model: m {}",
        hasToString("[objOpen(model, m),"
            + " objClose()]"));
    assertParseThrows("# just a comment",
        hasToString("java.lang.RuntimeException: "
            + "net.hydromatic.lookml.parse.ParseException: "
            + "Encountered \"<EOF>\" at line 1, column 16.\n"
            + "Was expecting:\n"
            + "    <IDENTIFIER> ...\n"
            + "    "));
    assertParseThrows("abc",
        hasToString("java.lang.RuntimeException: "
            + "net.hydromatic.lookml.parse.ParseException: "
            + "Encountered \"<EOF>\" at line 1, column 3.\n"
            + "Was expecting:\n"
            + "    \":\" ...\n"
            + "    "));
    assertParse("model: m {}",
        hasToString("[objOpen(model, m),"
            + " objClose()]"));
    assertParse("s: \"a \\\"quoted\\\" string\"",
        hasToString("[string(s, a \\\"quoted\\\" string)]"));
    assertParse("p: []",
        hasToString("[listOpen(p), listClose()]"));
    assertParse("p: [1]",
        hasToString("[listOpen(p), number(1), listClose()]"));
    assertParse("p: [1, true, [2], -3.5]",
        hasToString("[listOpen(p), number(1), identifier(true),"
            + " listOpen(), number(2), listClose(),"
            + " number(-3.5), listClose()]"));
    assertParse("# begin\n"
            + "model: m {\n"
            + "# middle\n"
            + "} # end",
        hasToString("[comment(# begin),"
            + " objOpen(model, m),"
            + " comment(# middle),"
            + " objClose(),"
            + " comment(# end)]"));
    assertParseThrows("",
        hasToString("java.lang.RuntimeException: "
            + "net.hydromatic.lookml.parse.ParseException: "
            + "Encountered \"<EOF>\" at line 0, column 0.\n"
            + "Was expecting:\n"
            + "    <IDENTIFIER> ...\n"
            + "    "));
    assertParse("model: m {\n"
            + "  sql: multi\n"
            + "     line;;\n"
            + "}",
        hasToString("[objOpen(model, m),"
            + " code(sql,  multi\n"
            + "     line),"
            + " objClose()]"));
    assertParse("model: m {\n"
            + "  my_list: [\n"
            + "    # before element 0\n"
            + "    0,\n"
            + "    # between elements 0 and 1\n"
            + "    # another\n"
            + "    1,\n"
            + "    2\n"
            + "    # after element but before comma\n"
            + "    ,\n"
            + "    # after comma\n"
            + "    2\n"
            + "    # after last element\n"
            + "  ]\n"
            + "}",
        hasToString("["
            + "objOpen(model, m),"
            + " listOpen(my_list),"
            + " comment(# before element 0),"
            + " number(0),"
            + " comment(# between elements 0 and 1),"
            + " comment(# another),"
            + " number(1),"
            + " number(2),"
            + " comment(# after element but before comma),"
            + " comment(# after comma),"
            + " number(2),"
            + " comment(# after last element),"
            + " listClose(),"
            + " objClose()"
            + "]"));
  }

  /** Parses the example document that occurs in {@code README.md}. */
  @Test void testParseReadmeExample() {
    String code = "# Description of the Beatles in Syntactic LookML.\n"
        + "band: beatles {\n"
        + "  founded: 1962\n"
        + "  origin: \"Liverpool\"\n"
        + "  member: paul {\n"
        + "    instruments: [\"bass\", \"guitar\", \"vocal\"]\n"
        + "  }\n"
        + "  member: john {\n"
        + "    instruments: [\"guitar\", \"vocal\", \"harmonica\"]\n"
        + "    lyric: Living is easy with eyes closed\n"
        + "      Misunderstanding all you see\n"
        + "      It's getting hard to be someone, but it all works out\n"
        + "      It doesn't matter much to me ;;\n"
        + "  }\n"
        + "  member: george {\n"
        + "    instruments: [\"guitar\", \"vocal\"]\n"
        + "  }\n"
        + "  member: ringo {\n"
        + "    instruments: [\"drums\", \"vocal\"]\n"
        + "  }\n"
        + "}\n";
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);
    ObjectHandler h =
        new ObjectHandler() {
          @Override public ObjectHandler code(String propertyName,
              String value) {
            if (propertyName.equals("lyric")) {
              out.println(value);
            }
            return this;
          }
        };
    LookmlParsers.parse(h, code,
        LookmlParsers.config()
            .withCodePropertyNames(Collections.singleton("lyric")));
    assertThat(sw,
        hasToString(" Living is easy with eyes closed\n"
            + "      Misunderstanding all you see\n"
            + "      It's getting hard to be someone, but it all works out\n"
            + "      It doesn't matter much to me \n"));
  }

  /** Tests building a simple schema with one enum type. */
  @Test void testSchemaBuilder() {
    LookmlSchema s =
        LookmlSchemas.schemaBuilder()
            .setName("simple")
            .addEnum("boolean", "true", "false")
            .build();
    assertThat(s.name(), is("simple"));
    assertThat(s.objectTypes(), anEmptyMap());
    assertThat(s.enumTypes(), aMapWithSize(1));
    assertThat(s.enumTypes().get("boolean").allowedValues(),
        hasToString("[false, true]"));
  }

  /** Tests building a schema with two enum types and one root object type. */
  @Test void testSchemaBuilder2() {
    LookmlSchema s =
        LookmlSchemas.schemaBuilder()
            .setName("example")
            .addEnum("boolean", "true", "false")
            .addEnum("join_type", "inner", "cross_join", "left_outer")
            .addNamedObjectProperty("empty_object",
                LookmlSchemas.ObjectTypeBuilder::build)
            .addNamedObjectProperty("model",
                b -> b.addNumberProperty("x")
                    .addStringProperty("y")
                    .addEnumProperty("z", "boolean")
                    .addObjectProperty("empty_object")
                    .build())
            .build();
    assertThat(s.name(), is("example"));
    assertThat(s.enumTypes(), aMapWithSize(2));
    assertThat(s.enumTypes().get("boolean").allowedValues(),
        hasToString("[false, true]"));
    assertThat(s.enumTypes().get("join_type").allowedValues(),
        hasToString("[cross_join, inner, left_outer]"));
    assertThat(s.objectTypes(), aMapWithSize(2));
    assertThat(s.objectTypes().get("baz"), nullValue());
    assertThat(s.objectTypes().get("model"), notNullValue());
    assertThat(s.objectTypes().get("model").properties().keySet(),
        hasToString("[empty_object, x, y, z]"));
  }

  /** Tests building a schema where the same property name ("sql") is used for
   * both code and non-code properties. */
  @Test void testSchemaBuilderFailsWithMixedCodeProperties() {
    try {
      LookmlSchema s =
          LookmlSchemas.schemaBuilder()
              .addObjectType("view",
                  b -> b.addNumberProperty("x")
                      .addCodeProperty("sql")
                      .build())
              .addObjectType("dimension",
                  b -> b.addNumberProperty("y")
                      .addStringProperty("sql")
                      .build())
              .build();
      fail("expected error, got " + s);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
          is("property 'sql' has both code and non-code uses"));
    }
  }

  /** Tests building the Mini-LookML schema. */
  @Test void testSchemaBuilder3() {
    final LookmlSchema schema = MiniLookml.schema();
    assertThat(schema.objectTypes(), aMapWithSize(7));
    assertThat(schema.enumTypes(), aMapWithSize(5));
    assertThat(schema.rootProperties(), aMapWithSize(1));
  }

  /** Tests that the schema for Schema-LookML obtained by parsing
   * {@code schema-schema.lkml} is equivalent to the one created by the
   * {@link SchemaLookml#schema()} method.
   *
   * <p>Also lets the schema-schema validate itself. */
  @Test void testCompareSchemaSchema() {
    final URL url = SchemaLookml.getSchemaUrl();
    final LookmlSchema schema = LookmlSchemas.load(url, null);
    final LookmlSchema schemaSchema = SchemaLookml.schema();
    assertThat(LookmlSchemas.compare(schema, schemaSchema), empty());
    assertThat(LookmlSchemas.equal(schema, schemaSchema), is(true));

    // Use the schema to validate itself.
    final LookmlSchema schema2 = LookmlSchemas.load(url, schema);
    assertThat(LookmlSchemas.equal(schema, schema2), is(true));
  }

  /** Tests that the Mini-LookML schema obtained by parsing
   * {@code mini-lookml-schema.lkml} is equivalent to the one created by the
   * {@link MiniLookml#schema()} method. */
  @Test void testCompareMiniSchema() {
    final URL url = MiniLookml.getSchemaUrl();
    final LookmlSchema schema =
        LookmlSchemas.load(url, SchemaLookml.schema());
    final LookmlSchema miniSchema = MiniLookml.schema();
    assertThat(LookmlSchemas.compare(schema, miniSchema), empty());
    assertThat(LookmlSchemas.equal(schema, miniSchema), is(true));
  }

  /** Parses the example model. */
  @Test void testParseExample() {
    final ParseFixture f0 =
        ParseFixture.of()
            .withCodePropertyNames("sql", "sql_on", "sql_table_name");
    ParseFixture.Parsed f1 = f0.parse(MiniLookml.exampleModel());
    assertThat(f1.list, hasSize(62));
  }
}

// End LaxTest.java
