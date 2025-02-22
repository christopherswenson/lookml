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

import net.hydromatic.lookml.ErrorHandler;
import net.hydromatic.lookml.LaxHandlers;
import net.hydromatic.lookml.LookmlSchema;
import net.hydromatic.lookml.MiniLookml;
import net.hydromatic.lookml.ObjectHandler;
import net.hydromatic.lookml.Pos;
import net.hydromatic.lookml.PropertyHandler;
import net.hydromatic.lookml.Sources;
import net.hydromatic.lookml.parse.LookmlParsers;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import static java.util.Objects.requireNonNull;

/**
 * Contains necessary state for testing the parser and validator.
 */
class ParseFixture {
  final @Nullable LookmlSchema schema;
  final Set<String> codePropertyNames;
  final boolean includePos;

  private ParseFixture(@Nullable LookmlSchema schema,
      Set<String> codePropertyNames, boolean includePos) {
    this.schema = schema;
    this.codePropertyNames = ImmutableSortedSet.copyOf(codePropertyNames);
    this.includePos = includePos;
  }

  /** Creates a ParseFixture. */
  static ParseFixture of() {
    return new ParseFixture(null, ImmutableSortedSet.of("sql"), false);
  }

  /** Returns a ParseFixture that is a copy of this with a given set of code
   * property names. */
  ParseFixture withCodePropertyNames(String... codePropertyNames) {
    return new ParseFixture(schema,
        ImmutableSortedSet.copyOf(codePropertyNames), includePos);
  }

  /** Returns a ParseFixture that is a copy of this with a given schema. */
  ParseFixture withSchema(LookmlSchema schema) {
    return new ParseFixture(requireNonNull(schema), schema.codePropertyNames(),
        includePos);
  }

  /** Returns a ParseFixture that is a copy of this and optionally includes
   * position ({@link Pos}) when printing each event. */
  public ParseFixture withIncludePos(boolean includePos) {
    return includePos == this.includePos ? this
        : new ParseFixture(schema, codePropertyNames, includePos);
  }

  /** Assigns the current LookML string and parses;
   * if {@link #schema} is not null, also validates. */
  Parsed parse(String code) {
    final List<String> list = new ArrayList<>();
    final List<String> list2 = new ArrayList<>();
    final List<String> errorList = new ArrayList<>();
    final LookmlParsers.Config config =
        LookmlParsers.config()
            .withCodePropertyNames(codePropertyNames)
            .withSource(Sources.fromString(code));
    if (schema != null) {
      final PropertyHandler logger =
          LaxHandlers.loggerTyped(list::add, includePos);
      final PropertyHandler logger2 =
          LaxHandlers.untype(
              LaxHandlers.logger(list2::add, includePos));
      final ErrorHandler errorHandler =
          LaxHandlers.errorLogger(errorList::add);
      final ObjectHandler validator =
          LaxHandlers.validator(
              LaxHandlers.tee(logger, logger2), schema, errorHandler);
      LookmlParsers.parse(validator, config);
    } else {
      final ObjectHandler logger =
          LaxHandlers.logger(list::add, includePos);
      LookmlParsers.parse(logger, config);
    }
    return new Parsed(this, list, list2, errorList, code);
  }

  /** Subtracts one list from another in a merge-like manner. */
  static <E> List<E> minus(List<E> list0, List<E> list1) {
    List<E> list = new ArrayList<>();
    for (int i0 = 0, i1 = 0; i0 < list0.size();) {
      if (i1 < list1.size() && list0.get(i0).equals(list1.get(i1))) {
        // This element is in both. Skip it in both.
        ++i0;
        ++i1;
      } else {
        list.add(list0.get(i0));
        ++i0;
      }
    }
    return list;
  }

  /** The result of the phase that parses a LookML string and runs it through
   * the schema validator. */
  static class Parsed {
    final ParseFixture parseFixture;
    final List<String> list;
    final List<String> list2;
    final List<String> errorList;
    final String code;

    Parsed(ParseFixture parseFixture, List<String> list, List<String> list2,
        List<String> errorList, String code) {
      this.parseFixture = parseFixture;
      this.list = list;
      this.list2 = list2;
      this.errorList = errorList;
      this.code = code;
    }

    /** Returns a list of events that are emitted without validation
     * but omitted with validation. */
    List<String> discardedEvents() {
      final List<String> list3 = new ArrayList<>();
      final ObjectHandler logger = LaxHandlers.logger(list3::add);
      final LookmlParsers.Config config =
          LookmlParsers.config()
              .withCodePropertyNames(parseFixture.codePropertyNames)
              .withSource(Sources.fromString(code));
      LookmlParsers.parse(logger, config);
      return minus(list3, this.list2);
    }

    Validated validate() {
      assertThat("can't validate without a schema", parseFixture.schema,
          notNullValue());
      final MiniLookml.Model model = build();
      final MiniLookml.Validator v = new MiniLookml.Validator();
      final List<String> errorList = new ArrayList<>();
      v.validate(model, errorList);
      return new Validated(this, model, errorList);
    }

    /** Converts the model into an AST. */
    MiniLookml.Model build() {
      final Map<String, Object> list = new LinkedHashMap<>();
      final PropertyHandler astBuilder =
          MiniLookml.builder(parseFixture.schema, list::put);
      final List<String> errorList = new ArrayList<>();
      final ObjectHandler validator =
          LaxHandlers.validator(astBuilder, parseFixture.schema,
              LaxHandlers.errorLogger(errorList::add));
      final LookmlParsers.Config config =
          LookmlParsers.config()
              .withCodePropertyNames(parseFixture.codePropertyNames)
              .withSource(Sources.fromString(code));
      LookmlParsers.parse(validator, config);
      assertThat(errorList, empty());
      return (MiniLookml.Model) Iterables.getOnlyElement(list.values());
    }
  }

  /** The result of the phase that creates an AST (model) and validates it. */
  static class Validated {
    final Parsed parsed;
    final MiniLookml.Model model;
    final List<String> list;

    Validated(Parsed parsed, MiniLookml.Model model, List<String> list) {
      this.parsed = parsed;
      this.model = model;
      this.list = list;
    }
  }
}

// End ParseFixture.java
