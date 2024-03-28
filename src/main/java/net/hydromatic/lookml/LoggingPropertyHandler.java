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

/** Handler that converts LookML properties into strings, and appends those
 * strings to a given consumer. */
class LoggingPropertyHandler implements PropertyHandler {
  protected final Consumer<String> consumer;
  protected final boolean includePos;

  LoggingPropertyHandler(Consumer<String> consumer, boolean includePos) {
    this.consumer = consumer;
    this.includePos = includePos;
  }

  static LoggingPropertyHandler root(Consumer<String> consumer,
      boolean includePos) {
    return new RootLoggingPropertyHandler(consumer, includePos);
  }

  @Override public PropertyHandler property(Pos pos,
      LookmlSchema.Property property, Object value) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("property(").append(property.name())
                .append(", ").append(property.type())
                .append(", ").append(value).append(")")));
    return this;
  }

  @Override public ListHandler listOpen(Pos pos,
      LookmlSchema.Property property) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("listOpen(").append(property.name())
                .append(", ").append(property.type()).append(")")));
    return new LoggingListHandler(this.consumer, includePos);
  }

  @Override public PropertyHandler objOpen(Pos pos,
      LookmlSchema.Property property) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("objOpen(").append(property.name())
                .append(", ").append(property.type()).append(")")));
    return this;
  }

  @Override public PropertyHandler objOpen(Pos pos,
      LookmlSchema.Property property, String name) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("objOpen(").append(property.name())
                .append(", ").append(property.type())
                .append(", ").append(name).append(")")));
    return this;
  }

  @Override public void close(Pos pos) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("objClose()")));
  }

  /** Handler for the root of the document. Its behavior is as
   * {@link LoggingPropertyHandler} except that {@link ObjectHandler#close(Pos)}
   * does not generate a message. */
  static class RootLoggingPropertyHandler extends LoggingPropertyHandler {
    RootLoggingPropertyHandler(Consumer<String> consumer, boolean includePos) {
      super(consumer, includePos);
    }

    @Override public PropertyHandler objOpen(Pos pos,
        LookmlSchema.Property property) {
      super.objOpen(pos, property);
      return new LoggingPropertyHandler(consumer, includePos);
    }

    @Override public PropertyHandler objOpen(Pos pos,
        LookmlSchema.Property property, String name) {
      super.objOpen(pos, property, name);
      return new LoggingPropertyHandler(consumer, includePos);
    }

    @Override public void close(Pos pos) {
      // ignore
    }
  }
}

// End LoggingPropertyHandler.java
