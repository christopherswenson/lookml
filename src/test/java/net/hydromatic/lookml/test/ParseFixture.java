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
import net.hydromatic.lookml.ObjectHandler;
import net.hydromatic.lookml.parse.LookmlParsers;

import com.google.common.collect.ImmutableSortedSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contains necessary state for testing the parser.
 */
class ParseFixture {
  final Set<String> codePropertyNames;

  private ParseFixture(Set<String> codePropertyNames) {
    this.codePropertyNames = codePropertyNames;
  }

  /** Creates a ParseFixture. */
  static ParseFixture of() {
    return new ParseFixture(ImmutableSortedSet.of("sql"));
  }

  /** Assigns the current LookML string and parses. */
  Parsed parse(String code) {
    final List<String> list = new ArrayList<>();
    final ObjectHandler logger = LaxHandlers.logger(list::add);
    LookmlParsers.parse(logger, code,
        LookmlParsers.config().withCodePropertyNames(codePropertyNames));
    return new Parsed(this, list, code);
  }

  /** The result of the phase that parses a LookML string. */
  static class Parsed {
    final ParseFixture parseFixture;
    final List<String> list;
    final String code;

    Parsed(ParseFixture parseFixture, List<String> list, String code) {
      this.parseFixture = parseFixture;
      this.list = list;
      this.code = code;
    }
  }
}

// End ParseFixture.java
