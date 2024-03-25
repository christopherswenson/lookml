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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;

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
}

// End LaxTest.java
