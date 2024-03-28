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

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link ListHandler} that forwards to a consumer.
 *
 * <p>As it stands, it is a no-op. But it is useful for subclassing.
 *
 * @see FilterObjectHandler
 */
public class FilterListHandler implements ListHandler {
  final ListHandler consumer;

  /** Creates a FilterListHandler.
   *
   * @param consumer List-handler to which to forward all unhandled events
   */
  protected FilterListHandler(ListHandler consumer) {
    this.consumer = requireNonNull(consumer);
  }

  @Override public ListHandler comment(Pos pos, String comment) {
    consumer.comment(pos, comment);
    return this;
  }

  @Override public ListHandler string(Pos pos, String value) {
    consumer.string(pos, value);
    return this;
  }

  @Override public ListHandler number(Pos pos, Number value) {
    consumer.number(pos, value);
    return this;
  }

  @Override public ListHandler identifier(Pos pos, String value) {
    consumer.identifier(pos, value);
    return this;
  }

  @Override public ListHandler pair(Pos pos, String ref, String identifier) {
    consumer.pair(pos, ref, identifier);
    return this;
  }

  @Override public ListHandler listOpen(Pos pos) {
    final ListHandler listHandler = consumer.listOpen(pos);
    return new FilterListHandler(listHandler);
  }

  @Override public void close(Pos pos) {
    consumer.close(pos);
  }
}

// End FilterListHandler.java
