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
package net.hydromatic.lookml;

import net.hydromatic.lookml.test.LaxTest;

import com.google.common.base.Suppliers;

import java.net.URL;
import java.util.function.Supplier;

/** Defines a very small subset of LookML, called Mini-LookML.
 *
 * <p>Mini-LookML is used for testing and also as an example for creating
 * more serious dialects (schemas) of LookML.
 *
 * <p>Mini-LookML has at least one example of each syntactic type of property
 * (number, string, boolean, ref-list, etc.) We also provide an example model,
 * {@link #exampleModel}, that contains at least one instance of each property.
 * It can therefore be used to validate builders, writers, and so forth. */
public class MiniLookml {
  private MiniLookml() {}

  /** Caches a LookML string that contains at least one instance
   * of every property in {@link #schema()}. */
  private static final Supplier<String> EXAMPLE_MODEL_SUPPLIER =
      Suppliers.memoize(() ->
          LookmlSchemas.urlContents(
              LaxTest.class.getResource(
                  "/lookml/mini-lookml-example-model.lkml")));

  /** Caches the schema. */
  private static final Supplier<LookmlSchema> SCHEMA_SUPPLIER =
      Suppliers.memoize(MiniLookml::schema_);

  /** Returns the LookML source text of an example model. */
  public static String exampleModel() {
    return EXAMPLE_MODEL_SUPPLIER.get();
  }

  /** Returns the schema of Mini-LookML. */
  public static LookmlSchema schema() {
    return SCHEMA_SUPPLIER.get();
  }

  private static LookmlSchema schema_() {
    return LookmlSchemas.schemaBuilder()
        .setName("mini")
        .addEnum("boolean", "false", "true")
        .addEnum("join_type", "left_outer", "full_outer", "inner", "cross")
        .addEnum("relationship_type", "many_to_one", "many_to_many",
            "one_to_many", "one_to_one")
        .addEnum("dimension_field_type", "bin", "date", "date_time", "distance",
            "duration", "location", "number", "string", "tier", "time",
            "unquoted", "yesno", "zipcode")
        .addEnum("measure_field_type", "average", "average_distinct", "count",
            "count_distinct", "date", "list", "max", "median",
            "median_distinct", "min", "number", "percent_of_previous",
            "percent_of_total", "percentile", "percentile_distinct",
            "running_total", "string", "sum", "sum_distinct", "yesno")
        .addObjectType("conditionally_filter", b ->
            b.addRefStringMapProperty("filters")
                .addRefListProperty("unless")
                .build())
        .addObjectType("dimension", b ->
            b.addEnumProperty("type", "dimension_field_type")
                .addCodeProperty("sql")
                .addStringProperty("label")
                .addEnumProperty("primary_key", "boolean")
                .addStringListProperty("tags")
                .build())
        .addObjectType("measure", b ->
            b.addEnumProperty("type", "measure_field_type")
                .addCodeProperty("sql")
                .addStringProperty("label")
                .build())
        .addObjectType("view", b ->
            b.addRefProperty("from")
                .addStringProperty("label")
                .addCodeProperty("sql_table_name")
                .addNamedObjectProperty("dimension")
                .addNamedObjectProperty("measure")
                .addRefListProperty("drill_fields")
                .build())
        .addObjectType("join", b ->
            b.addRefProperty("from")
                .addCodeProperty("sql_on")
                .addEnumProperty("relationship", "relationship_type")
                .build())
        .addObjectType("explore", b ->
            b.addRefProperty("from")
                .addRefProperty("view_name")
                .addNamedObjectProperty("join")
                .addObjectProperty("conditionally_filter")
                .build())
        .addNamedObjectProperty("model", b ->
            b.addNamedObjectProperty("explore")
                .addNamedObjectProperty("view")
                .addNumberProperty("fiscal_month_offset")
                .build())
        .build();
  }

  /** Returns the URL of a file that contains the Mini-LookML schema.
   *
   * <p>The contents of this file creates a schema identical to that returned
   * from {@link #schema()}. This is verified by a test. */
  public static URL getSchemaUrl() {
    return LaxTest.class.getResource("/lookml/mini-lookml-schema.lkml");
  }
}

// End MiniLookml.java
