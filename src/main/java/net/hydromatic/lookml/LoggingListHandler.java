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

import java.util.function.Consumer;

/** Implementation of {@link ListHandler}
 * that logs events, as strings, to a consumer.
 *
 * <p>This class is necessary because there are methods in common between
 * the {@link ObjectHandler} and {@link ListHandler} interfaces. If there
 * were no methods in common, a single object could have implemented both
 * interfaces. */
class LoggingListHandler implements ListHandler {
  private final Consumer<String> consumer;

  LoggingListHandler(Consumer<String> consumer) {
    this.consumer = consumer;
  }

  @Override public ListHandler string(String value) {
    consumer.accept("string(" + value + ")");
    return this;
  }

  @Override public ListHandler number(Number value) {
    consumer.accept("number(" + value + ")");
    return this;
  }

  @Override public ListHandler identifier(String value) {
    consumer.accept("identifier(" + value + ")");
    return this;
  }

  @Override public ListHandler pair(String ref, String identifier) {
    consumer.accept("pair(" + ref + ", " + identifier + ")");
    return this;
  }

  @Override public ListHandler comment(String comment) {
    consumer.accept("comment(" + comment + ")");
    return this;
  }

  @Override public ListHandler listOpen() {
    consumer.accept("listOpen()");
    return this;
  }

  @Override public void close() {
    consumer.accept("listClose()");
  }
}

// End LoggingListHandler.java
