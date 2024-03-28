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

import net.hydromatic.lookml.MiniLookml;
import net.hydromatic.lookml.Source;
import net.hydromatic.lookml.Sources;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/** Tests utilities. */
public class UtilTest {
  /** Unit test for {@link Source}. */
  @Test void testSource() {
    final Source source1 = Sources.fromString("abc");
    assertThat(source1, hasToString("<inline>"));
    assertThat(source1.preferStream(), is(false));
    assertThat(source1.contentsAsString(), is("abc"));

    final Source source2 = MiniLookml.getSchemaSource();
    assertThat(source2, hasToString(startsWith("file:")));
    assertThat(source2,
        hasToString(endsWith("/lookml/mini-lookml-schema.lkml")));
    assertThat(source2.preferStream(), is(true));
    assertThat(source2.contentsAsString(), startsWith("# Licensed to "));
    assertThat(source2.contentsAsString(), containsString("schema: mini"));
  }
}

// End UtilTest.java
