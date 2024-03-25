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

import java.util.List;

/** Object handler that forwards each event to several consumers. */
class TeeObjectHandler implements ObjectHandler {
  final List<ObjectHandler> consumers;

  TeeObjectHandler(ImmutableList<ObjectHandler> consumers) {
    this.consumers = consumers;
  }

  @Override public ObjectHandler comment(String comment) {
    consumers.forEach(c -> c.comment(comment));
    return this;
  }

  @Override public ObjectHandler number(String propertyName,
      Number value) {
    consumers.forEach(c -> c.number(propertyName, value));
    return this;
  }

  @Override public ObjectHandler string(String propertyName,
      String value) {
    consumers.forEach(c -> c.string(propertyName, value));
    return this;
  }

  @Override public ObjectHandler identifier(String propertyName,
      String value) {
    consumers.forEach(c -> c.identifier(propertyName, value));
    return this;
  }

  @Override public ObjectHandler code(String propertyName,
      String value) {
    consumers.forEach(c -> c.code(propertyName, value));
    return this;
  }

  @Override public ListHandler listOpen(String propertyName) {
    final ImmutableList.Builder<ListHandler> newConsumers =
        ImmutableList.builder();
    consumers.forEach(c -> newConsumers.add(c.listOpen(propertyName)));
    return new TeeListHandler(newConsumers.build());
  }

  @Override public ObjectHandler objOpen(String propertyName) {
    final ImmutableList.Builder<ObjectHandler> newConsumers =
        ImmutableList.builder();
    consumers.forEach(c ->
        newConsumers.add(c.objOpen(propertyName)));
    return new TeeObjectHandler(newConsumers.build());
  }

  @Override public ObjectHandler objOpen(String propertyName,
      String name) {
    final ImmutableList.Builder<ObjectHandler> newConsumers =
        ImmutableList.builder();
    consumers.forEach(c ->
        newConsumers.add(c.objOpen(propertyName, name)));
    return new TeeObjectHandler(newConsumers.build());
  }

  @Override public void close() {
    consumers.forEach(ObjectHandler::close);
  }
}

// End TeeObjectHandler.java
