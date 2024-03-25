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

/** Object handler that forwards all events to a consumer.
 *
 * <p>As it stands, it is a no-op. But it is useful for subclassing.
 *
 * @see FilterListHandler */
public class FilterObjectHandler implements ObjectHandler {
  final ObjectHandler consumer;


  /** Creates a FilterObjectHandler.
   *
   * @param consumer Handler to which to forward all unhandled events
   */
  protected FilterObjectHandler(ObjectHandler consumer) {
    this.consumer = requireNonNull(consumer);
  }

  @Override public ObjectHandler comment(String comment) {
    consumer.comment(comment);
    return this;
  }

  @Override public ObjectHandler number(String propertyName, Number value) {
    consumer.number(propertyName, value);
    return this;
  }

  @Override public ObjectHandler string(String propertyName, String value) {
    consumer.string(propertyName, value);
    return this;
  }

  @Override public ObjectHandler identifier(String propertyName, String value) {
    consumer.identifier(propertyName, value);
    return this;
  }

  @Override public ObjectHandler code(String propertyName, String value) {
    consumer.code(propertyName, value);
    return this;
  }

  @Override public ListHandler listOpen(String propertyName) {
    final ListHandler listHandler = consumer.listOpen(propertyName);
    return new FilterListHandler(listHandler);
  }

  @Override public ObjectHandler objOpen(String propertyName) {
    final ObjectHandler objectHandler = consumer.objOpen(propertyName);
    return new FilterObjectHandler(objectHandler);
  }

  @Override public ObjectHandler objOpen(String propertyName, String name) {
    final ObjectHandler objectHandler = consumer.objOpen(propertyName, name);
    return new FilterObjectHandler(objectHandler);
  }

  @Override public void close() {
    consumer.close();
  }
}

// End FilterObjectHandler.java
