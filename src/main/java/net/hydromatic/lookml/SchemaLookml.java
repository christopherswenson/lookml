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

import com.google.common.base.Suppliers;

import java.net.URL;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/** Defines Schema-LookML, a dialect of LookML for defining schemas.
 *
 * <p>Here is a Schema-LookML document that defines a schema:
 *
 * <blockquote><pre>{@code
 * schema: schema {
 *   enum_type: type {
 *     values: ["number", "string", "enum", "code", "object", "named_object",
 *         "ref", "ref_list", "string_list", "ref_string_map", "ref_string"]
 *   }
 *   object_type: enum_type {
 *     property: values {
 *       type: string_list
 *     }
 *   }
 *   object_type: object_type {
 *     property: property {
 *       type: named_object
 *     }
 *   }
 *   object_type: property {
 *     property: type {
 *       type: ref
 *     }
 *   }
 *   object_type: schema {
 *     property: enum_type {
 *       type: named_object
 *     }
 *     property: object_type {
 *       type: named_object
 *     }
 *     property: root_properties {
 *       type: ref_list
 *     }
 *   }
 *   root_properties: [schema]
 * }
 * }</pre></blockquote>
 *
 */
public class SchemaLookml {
  /** Cached schema. */
  private static final Supplier<LookmlSchema> SCHEMA_SUPPLIER =
      Suppliers.memoize(SchemaLookml::schema_);

  private SchemaLookml() {}

  /** Returns the URL of a file that contains the Schema-LookML schema.
   *
   * <p>The contents of this file creates a schema identical to that returned
   * from {@link #schema()}. This is verified by a test. */
  public static URL getSchemaUrl() {
    final URL url =
        SchemaLookml.class.getResource("/lookml/schema-schema.lkml");
    return requireNonNull(url);
  }

  /** Returns the schema Schema-LookML. It can be used to validate any
   * schema, including itself.
   *
   * <p>It is equivalent to "/lookml/schema-schema.lkml". */
  public static LookmlSchema schema() {
    return SCHEMA_SUPPLIER.get();
  }

  static LookmlSchema schema_() {
    return LookmlSchemas.schemaBuilder()
        .setName("schema")
        .addEnum("type", "number", "string", "enum", "code", "object",
            "named_object", "ref", "ref_list", "string_list",
            "ref_string_map", "ref_string")
        .addObjectType("enum_type", b ->
            b.addStringListProperty("values")
                .build())
        .addObjectType("object_type", b ->
            b.addNamedObjectProperty("property")
                .build())
        .addObjectType("property", b ->
            b.addRefProperty("type")
                .build())
        .addObjectType("schema", b ->
            b.addNamedObjectProperty("enum_type")
                .addNamedObjectProperty("object_type")
                .addRefListProperty("root_properties")
                .build())
        .addNamedObjectProperty("schema")
        .build();
  }
}

// End SchemaLookml.java
