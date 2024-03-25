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

import com.google.common.collect.ImmutableList;

/** Implementation of {@link ListHandler} that forwards to several consumers. */
class TeeListHandler implements ListHandler {
  final ImmutableList<ListHandler> consumers;

  TeeListHandler(ImmutableList<ListHandler> consumers) {
    this.consumers = consumers;
  }

  @Override public ListHandler string(String value) {
    consumers.forEach(c -> c.string(value));
    return this;
  }

  @Override public ListHandler number(Number value) {
    consumers.forEach(c -> c.number(value));
    return this;
  }

  @Override public ListHandler identifier(String value) {
    consumers.forEach(c -> c.identifier(value));
    return this;
  }

  @Override public ListHandler pair(String ref, String identifier) {
    consumers.forEach(c -> c.pair(ref, identifier));
    return this;
  }

  @Override public ListHandler comment(String comment) {
    consumers.forEach(c -> c.comment(comment));
    return this;
  }

  @Override public ListHandler listOpen() {
    final ImmutableList.Builder<ListHandler> newConsumers =
        ImmutableList.builder();
    consumers.forEach(c -> newConsumers.add(c.listOpen()));
    return new TeeListHandler(newConsumers.build());
  }

  @Override public void close() {
    consumers.forEach(ListHandler::close);
  }
}

// End TeeListHandler.java
