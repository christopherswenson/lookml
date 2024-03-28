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

import static net.hydromatic.lookml.LoggingObjectHandler.appendPos;

/** Implementation of {@link ListHandler}
 * that logs events, as strings, to a consumer.
 *
 * <p>This class is necessary because there are methods in common between
 * the {@link ObjectHandler} and {@link ListHandler} interfaces. If there
 * were no methods in common, a single object could have implemented both
 * interfaces. */
class LoggingListHandler implements ListHandler {
  private final Consumer<String> consumer;
  private final boolean includePos;

  LoggingListHandler(Consumer<String> consumer, boolean includePos) {
    this.consumer = consumer;
    this.includePos = includePos;
  }

  @Override public ListHandler string(Pos pos, String value) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("string(").append(value).append(")")));
    return this;
  }

  @Override public ListHandler number(Pos pos, Number value) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("number(").append(value).append(")")));
    return this;
  }

  @Override public ListHandler identifier(Pos pos, String value) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("identifier(").append(value).append(")")));
    return this;
  }

  @Override public ListHandler pair(Pos pos, String ref, String identifier) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("pair(").append(ref)
                .append(", ").append(identifier).append(")")));
    return this;
  }

  @Override public ListHandler comment(Pos pos, String comment) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("comment(").append(comment).append(")")));
    return this;
  }

  @Override public ListHandler listOpen(Pos pos) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("listOpen()")));
    return this;
  }

  @Override public void close(Pos pos) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("listClose()")));
  }
}

// End LoggingListHandler.java
