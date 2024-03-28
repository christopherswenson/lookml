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

/** Property handler that forwards each event to several consumers. */
class TeePropertyHandler implements PropertyHandler {
  final List<PropertyHandler> consumers;

  TeePropertyHandler(ImmutableList<PropertyHandler> consumers) {
    this.consumers = consumers;
  }

  @Override public PropertyHandler property(Pos pos,
      LookmlSchema.Property property, Object value) {
    consumers.forEach(c -> c.property(pos, property, value));
    return this;
  }

  @Override public ListHandler listOpen(Pos pos,
      LookmlSchema.Property property) {
    final ImmutableList.Builder<ListHandler> newConsumers =
        ImmutableList.builder();
    consumers.forEach(c -> newConsumers.add(c.listOpen(pos, property)));
    return new TeeListHandler(newConsumers.build());
  }

  @Override public PropertyHandler objOpen(Pos pos,
      LookmlSchema.Property property) {
    final ImmutableList.Builder<PropertyHandler> newConsumers =
        ImmutableList.builder();
    consumers.forEach(c -> newConsumers.add(c.objOpen(pos, property)));
    return new TeePropertyHandler(newConsumers.build());
  }

  @Override public PropertyHandler objOpen(Pos pos,
      LookmlSchema.Property property, String name) {
    final ImmutableList.Builder<PropertyHandler> newConsumers =
        ImmutableList.builder();
    consumers.forEach(c -> newConsumers.add(c.objOpen(pos, property, name)));
    return new TeePropertyHandler(newConsumers.build());
  }

  @Override public void close(Pos pos) {
    consumers.forEach(c -> c.close(pos));
  }
}

// End TeePropertyHandler.java
