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

import net.hydromatic.lookml.util.ImmutablePairList;
import net.hydromatic.lookml.util.PairList;

import java.util.List;

import static java.util.Objects.requireNonNull;

/** Implementations of {@link ValueImpl}. */
class Values {
  private Values() {}

  static NumberValue number(Number value) {
    return new NumberValue(value);
  }

  static IdentifierValue identifier(String value) {
    return new IdentifierValue(value);
  }

  static StringValue string(String value) {
    return new StringValue(value);
  }

  static CodeValue code(String value) {
    return new CodeValue(value);
  }

  static ListValue list(List<ValueImpl> list) {
    return new ListValue(list);
  }

  static NamedObjectValue namedObject(String name,
      PairList<String, ValueImpl> properties) {
    return new NamedObjectValue(name, properties);
  }

  static ObjectValue object(PairList<String, ValueImpl> properties) {
    return new ObjectValue(properties);
  }

  static PairValue pair(String ref, String identifier) {
    return new PairValue(ref, identifier);
  }

  /** Value of a property or list element whose value is an identifier. */
  static class IdentifierValue extends ValueImpl {
    final String id;

    IdentifierValue(String id) {
      this.id = id;
    }

    @Override void write(LookmlWriter writer) {
      writer.identifier(id);
    }
  }

  /** Value of a property or list element whose value is a number. */
  static class NumberValue extends ValueImpl {
    private final Number number;

    NumberValue(Number number) {
      this.number = number;
    }

    @Override void write(LookmlWriter writer) {
      writer.number(number);
    }
  }

  /** Value of a property or list element whose value is a string. */
  static class StringValue extends ValueImpl {
    final String s;

    StringValue(String s) {
      this.s = s;
    }

    @Override void write(LookmlWriter writer) {
      writer.string(s);
    }
  }

  /** Value of a property whose value is a code block. */
  static class CodeValue extends ValueImpl {
    private final String s;

    CodeValue(String s) {
      this.s = s;
    }

    @Override void write(LookmlWriter writer) {
      writer.code(s);
    }
  }

  /** Value of a property whose value is a ref-string pair. */
  static class PairValue extends ValueImpl {
    final String ref;
    final String s;

    PairValue(String ref, String s) {
      this.ref = ref;
      this.s = s;
    }

    @Override void write(LookmlWriter writer) {
      writer.label(ref);
      writer.string(s);
    }
  }

  /** Value of a property or list element whose value is a list. */
  static class ListValue extends ValueImpl {
    final List<ValueImpl> list;

    ListValue(List<ValueImpl> list) {
      this.list = list;
    }

    @Override void write(LookmlWriter writer) {
      writer.list(list);
    }
  }

  /** Value of a property whose value is an object.
   *
   * <p>For example,
   * <blockquote><pre>{@code
   * conditionally_filter: {
   *   filters: [f3: "> 10"]
   *   unless: [f1, f2]
   * }
   * }</pre></blockquote>
   *
   * <p>The name of the property, {@code conditionally_filter}, will be held
   * in the enclosing property list. */
  static class ObjectValue extends ValueImpl {
    final ImmutablePairList<String, ValueImpl> properties;

    ObjectValue(PairList<String, ValueImpl> properties) {
      this.properties = ImmutablePairList.copyOf(properties);
    }

    @Override void write(LookmlWriter writer) {
      writer.obj(properties);
    }
  }

  /** Value of a property whose value is an object and that also has a name.
   *
   * <p>For example,
   * <blockquote><pre>{@code
   * dimension: d1 {
   *   sql: orderDate;;
   *   type: int
   * }
   * }</pre></blockquote>
   *
   * <p>{@link #name} is "d1", and {@link #properties} has entries for "sql" and
   * "type". The name of the property, {@code dimension}, is held in the
   * enclosing property list. */
  static class NamedObjectValue extends ObjectValue {
    final String name;

    NamedObjectValue(String name, PairList<String, ValueImpl> properties) {
      super(properties);
      this.name = requireNonNull(name);
    }

    @Override void write(LookmlWriter writer) {
      // Write "name { ... }"
      writer.identifier(name);
      writer.append(' ');
      super.write(writer);
    }
  }
}

// End Values.java
