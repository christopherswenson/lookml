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
import net.hydromatic.lookml.Pos;
import net.hydromatic.lookml.SchemaLookml;
import net.hydromatic.lookml.Source;
import net.hydromatic.lookml.Sources;
import net.hydromatic.lookml.parse.LookmlParsers;

import com.google.common.collect.ImmutableList;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static net.hydromatic.lookml.test.ParseFixture.minus;

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
    Pos p = Pos.ZERO;
    h.obj(p, "model", "m", h1 ->
            h1.number(p, "n", 1)
                .string(p, "s", "hello")
                .identifier(p, "b", "true")
                .code(p, "code", "VALUES 1")
                .list(p, "list", h2 ->
                    h2.identifier(p, "asc")
                        .number(p, -2.5)
                        .string(p, "abc")
                        .list(p, h3 -> h3.string(p, "singleton")))
                .list(p, "emptyList", h2 -> {}))
        .close(p);
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

  @Test void testMinus() {
    final List<Integer> list = ImmutableList.of();
    final List<Integer> list123 = ImmutableList.of(1, 2, 3);
    final List<Integer> list1232 = ImmutableList.of(1, 2, 3, 2);
    final List<Integer> list2 = ImmutableList.of(2);
    final List<Integer> list13 = ImmutableList.of(1, 3);
    assertThat(minus(list123, list), hasToString("[1, 2, 3]"));
    assertThat(minus(list123, list13), hasToString("[2]"));
    assertThat(minus(list123, list2), hasToString("[1, 3]"));
    assertThat(minus(list1232, list2), hasToString("[1, 3, 2]"));
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
          @Override public ObjectHandler code(Pos pos, String propertyName,
              String value) {
            if (propertyName.equals("lyric")) {
              out.println(value);
            }
            return this;
          }
        };
    LookmlParsers.parse(h,
        LookmlParsers.config()
            .withCodePropertyNames(Collections.singleton("lyric"))
            .withSource(Sources.fromString(code)));
    assertThat(sw,
        hasToString(" Living is easy with eyes closed\n"
            + "      Misunderstanding all you see\n"
            + "      It's getting hard to be someone, but it all works out\n"
            + "      It doesn't matter much to me \n"));
  }

  @Test void testParsePosition() {
    checkParsePosition(false);
    checkParsePosition(true);
  }

  private void checkParsePosition(boolean withSchema) {
    ParseFixture f = ParseFixture.of();
    if (withSchema) {
      f = f.withSchema(MiniLookml.schema());
    }
    ParseFixture.Parsed parsed =
        f.withIncludePos(true)
            .parse("model: m {\n"
                + "  view: v {\n"
                + "  # a comment\n"
                + "    drill_fields: [f, g]\n"
                + "  }\n"
                + "}\n");
    final String expectedToString;
    if (withSchema) {
      expectedToString = "["
          + "objOpen(model, NAMED_OBJECT, m) at 1.1-1.11, "
          + "objOpen(view, NAMED_OBJECT, v) at 2.3-2.12, "
          + "listOpen(drill_fields, REF_LIST) at 4.5-4.20, "
          + "identifier(f) at 4.20, "
          + "identifier(g) at 4.23, "
          + "listClose() at 4.24, "
          + "objClose() at 5.3, "
          + "objClose() at 6.1]";
    } else {
      expectedToString = "["
          + "objOpen(model, m) at 1.1-1.11, "
          + "objOpen(view, v) at 2.3-2.12, "
          + "comment(# a comment) at 3.3-3.14, "
          + "listOpen(drill_fields) at 4.5-4.20, "
          + "identifier(f) at 4.20, "
          + "identifier(g) at 4.23, "
          + "listClose() at 4.24, "
          + "objClose() at 5.3, "
          + "objClose() at 6.1]";
    }
    assertThat(parsed.list, hasToString(expectedToString));
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

  /** Validates some small LookML documents according to the Mini-LookML
   * schema. */
  @Test void testValidateMini() {
    final LookmlSchema schema = MiniLookml.schema();
    final ParseFixture f = ParseFixture.of().withSchema(schema);
    final String s = "dimension: d {x: 1}";
    ParseFixture.Parsed f2 = f.parse(s);
    assertThat(f2.errorList,
        hasToString("[invalidRootProperty(dimension)]"));
    assertThat("all events should be discarded", f2.list, hasSize(0));

    ParseFixture.Parsed f3 = f.parse("model: {x: 1}");
    assertThat(f3.errorList,
        hasToString("[nameRequired(model)]"));
    assertThat("all events should be discarded", f3.list, hasSize(0));

    final Consumer<String> fn = m -> {
      ParseFixture.Parsed f4 = f.parse("model: " + m);
      assertThat(f4.errorList,
          hasToString("[invalidRootProperty(model)]"));
      assertThat("all events should be discarded", f4.list, hasSize(0));
    };
    fn.accept("1");
    fn.accept("\"a string\"");
    fn.accept("yes");
    fn.accept("inner_join");
    fn.accept("[]");

    ParseFixture.Parsed f4 = f.parse("model: m {\n"
        + "  dimension: d {}\n"
        + "}");
    assertThat(f4.errorList,
        hasToString("[invalidPropertyOfParent(dimension, model)]"));
    assertThat("d events should be discarded", f4.list2, hasSize(2));
    assertThat(f4.list2, hasToString("[objOpen(model, m), objClose()]"));

    ParseFixture.Parsed f5 = f.parse("model: m {\n"
        + "  view: v {\n"
        + "    drill_fields: true\n"
        + "    dimension: d {\n"
        + "      sql: VALUES ;;\n"
        + "      type: number\n"
        + "      label: \"a label\"\n"
        + "      tags: 123\n"
        + "    }\n"
        + "    measure: m {\n"
        + "      sql: VALUES 1;;\n"
        + "      type: average\n"
        + "      label: 1\n"
        + "    }\n"
        + "    dimension: d2 {\n"
        + "      type: average\n"
        + "      primary_key: \"a string\"\n"
        + "    }\n"
        + "    bad_object: {\n"
        + "      type: median\n"
        + "    }\n"
        + "  }\n"
        + "  explore: e {\n"
        + "    conditionally_filter: {\n"
        + "      filters: [f1: \"123\", f2: \"abc\"]\n"
        + "      unless: [f3, f4]\n"
        + "    }\n"
        + "  }\n"
        + "  explore: e2 {\n"
        + "    conditionally_filter: {\n"
        + "      bad: true\n"
        + "      filters: true\n"
        + "      unless: [\"a\", 1, f3, [2]]\n"
        + "    }\n"
        + "  }\n"
        + "}");
    assertThat(f5.errorList,
        hasToString("["
            + "invalidPropertyType(drill_fields, REF_LIST, REF), "
            + "invalidPropertyType(tags, STRING_LIST, NUMBER), "
            + "invalidPropertyType(label, STRING, NUMBER), "
            + "invalidPropertyType(dimension, type,"
            + " dimension_field_type, average), "
            + "invalidPropertyType(primary_key, ENUM, STRING), "
            + "invalidPropertyOfParent(bad_object, view), "
            + "invalidPropertyOfParent(bad, conditionally_filter), "
            + "invalidPropertyType(filters, REF_STRING_MAP, REF), "
            + "invalidListElement(unless, STRING, REF_LIST), "
            + "invalidListElement(unless, NUMBER, REF_LIST), "
            + "invalidListElement(unless, REF_LIST, REF_LIST)"
            + "]"));
    final List<String> discardedEvents = f5.discardedEvents();
    assertThat(discardedEvents, hasSize(15));
    assertThat(discardedEvents,
        hasToString("["
            + "identifier(drill_fields, true), "
            + "number(tags, 123), "
            + "number(label, 1), "
            + "identifier(type, average), "
            + "string(primary_key, a string), "
            + "objOpen(bad_object), "
            + "identifier(type, median), "
            + "objClose(), "
            + "identifier(bad, true), "
            + "identifier(filters, true), "
            + "string(a), "
            + "number(1), "
            + "listOpen(), "
            + "number(2), "
            + "listClose()]"));
  }

  /** Validates a LookML document that contains duplicate elements. */
  @Test void testValidateDuplicates() {
    final LookmlSchema schema = MiniLookml.schema();
    final ParseFixture f0 = ParseFixture.of().withSchema(schema);

    // Valid - no duplicates
    final String s = "model: m {\n"
        + "  explore: e1 {}\n"
        + "  explore: e2 {}\n"
        + "}";
    ParseFixture.Parsed f = f0.parse(s);
    assertThat(f.errorList, empty());

    // Invalid - duplicate explore e1
    final String s2 = "model: m {\n"
        + "  explore: e1 {}\n"
        + "  explore: e2 {}\n"
        + "  explore: e1 {}\n"
        + "}";
    f = f0.parse(s2);
    assertThat(f.errorList, hasSize(1));
    assertThat(f.errorList,
        hasToString("[duplicateNamedProperty(explore, e1)]"));
    assertThat(f.discardedEvents(), hasSize(2));
    assertThat(f.discardedEvents(),
        hasToString("[objOpen(explore, e1), objClose()]"));

    // Invalid - duplicate properties of type string, list, number, boolean,
    // enum (dimension.type)
    final String s3 = "model: m {\n"
        + "  fiscal_month_offset: 3\n"
        + "  view: v1 {\n"
        + "    label: \"label 1\"\n"
        + "    drill_fields: []\n"
        + "    drill_fields: [f1]\n"
        + "    label: \"label 2\"\n"
        + "    dimension: d1{\n"
        + "      primary_key: true\n"
        + "      type: date\n"
        + "      primary_key: true\n"
        + "      type: tier\n"
        + "    }\n"
        + "  }\n"
        + "  fiscal_month_offset: 2\n"
        + "}\n";
    f = f0.parse(s3);
    assertThat(f.errorList, hasSize(5));
    assertThat(f.errorList,
        hasToString("["
            + "duplicateProperty(drill_fields), "
            + "duplicateProperty(label), "
            + "duplicateProperty(primary_key), "
            + "duplicateProperty(type), "
            + "duplicateProperty(fiscal_month_offset)]"));
    assertThat(f.discardedEvents(), hasSize(7));
    assertThat(f.discardedEvents(),
        hasToString("["
            + "listOpen(drill_fields), "
            + "identifier(f1), "
            + "listClose(), "
            + "string(label, label 2), "
            + "identifier(primary_key, true), "
            + "identifier(type, tier), "
            + "number(fiscal_month_offset, 2)]"));
  }

  /** Tests that the schema for Schema-LookML obtained by parsing
   * {@code schema-schema.lkml} is equivalent to the one created by the
   * {@link SchemaLookml#schema()} method.
   *
   * <p>Also lets the schema-schema validate itself. */
  @Test void testCompareSchemaSchema() {
    final Source source = SchemaLookml.getSchemaSource();
    final LookmlSchema schema = LookmlSchemas.load(source, null);
    final LookmlSchema schemaSchema = SchemaLookml.schema();
    assertThat(LookmlSchemas.compare(schema, schemaSchema), empty());
    assertThat(LookmlSchemas.equal(schema, schemaSchema), is(true));

    // Use the schema to validate itself.
    final LookmlSchema schema2 = LookmlSchemas.load(source, schema);
    assertThat(LookmlSchemas.equal(schema, schema2), is(true));
  }

  /** Tests that the Mini-LookML schema obtained by parsing
   * {@code mini-lookml-schema.lkml} is equivalent to the one created by the
   * {@link MiniLookml#schema()} method. */
  @Test void testCompareMiniSchema() {
    final Source source = MiniLookml.getSchemaSource();
    final LookmlSchema schema =
        LookmlSchemas.load(source, SchemaLookml.schema());
    final LookmlSchema miniSchema = MiniLookml.schema();
    assertThat(LookmlSchemas.compare(schema, miniSchema), empty());
    assertThat(LookmlSchemas.equal(schema, miniSchema), is(true));
  }

  /** Tests that the example document for the Mini-LookML schema contains at
   * least one instance of each property. */
  @Test void testCheckMiniExampleCompleteness() {
    final LookmlSchema schema = MiniLookml.schema();
    final String model = MiniLookml.exampleModel();
    final List<String> errorList = new ArrayList<>();
    final LookmlParsers.Config config =
        LookmlParsers.config()
            .withCodePropertyNames(schema.codePropertyNames())
            .withSource(Sources.fromString(model));
    LookmlSchemas.checkCompleteness(schema,
        objectHandler -> LookmlParsers.parse(objectHandler, config),
        errorList);
    assertThat(errorList, empty());
  }

  /** Builds a model. */
  @Test void testBuild() {
    final ParseFixture f0 = ParseFixture.of().withSchema(MiniLookml.schema());
    ParseFixture.Parsed f1 = f0.parse("model: m {\n"
        + "  view: v1 {}\n"
        + "  view: v2 {}\n"
        + "  explore: e {\n"
        + "    join: v2 {}"
        + "  }\n"
        + "}");
    assertThat(f1.errorList, empty());
    ParseFixture.Validated f2 = f1.validate();
    assertThat(f2.list, empty());
    assertThat(f2.model, notNullValue());
  }

  /** Parses the example model. */
  @Test void testParseExample() {
    final ParseFixture f0 =
        ParseFixture.of()
            .withCodePropertyNames("sql", "sql_on", "sql_table_name");
    ParseFixture.Parsed f1 = f0.parse(MiniLookml.exampleModel());
    assertThat(f1.list, hasSize(62));
  }

  /** Builds the example model,
   * which {@link #testCheckMiniExampleCompleteness()}
   * has proved contains every attribute. */
  @Test void testBuildExample() {
    final ParseFixture f0 = ParseFixture.of().withSchema(MiniLookml.schema());
    ParseFixture.Parsed f1 = f0.parse(MiniLookml.exampleModel());
    assertThat(f1.errorList, empty());
    ParseFixture.Validated f2 = f1.validate();
    assertThat(f2.list, empty());
    assertThat(f2.model, notNullValue());
  }

  /** Validates a model. */
  @Test void testValidate() {
    final ParseFixture f0 = ParseFixture.of().withSchema(MiniLookml.schema());
    ParseFixture.Parsed f1 = f0.parse("model: m {\n"
        + "  view: v1 {}\n"
        + "  explore: e {\n"
        + "    view_name: v2"
        + "  }\n"
        + "}");
    assertThat(f1.errorList, empty());
    ParseFixture.Validated f2 = f1.validate();
    assertThat(f2.list, empty());
  }
}

// End LaxTest.java
