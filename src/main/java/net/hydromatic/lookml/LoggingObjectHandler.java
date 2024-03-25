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

/** Handler that converts LookML parse events into strings, and appends those
 * strings to a given consumer. */
class LoggingObjectHandler implements ObjectHandler {
  protected final Consumer<String> consumer;

  static ObjectHandler create(Consumer<String> consumer) {
    return new RootLoggingObjectHandler(consumer);
  }

  private LoggingObjectHandler(Consumer<String> consumer) {
    this.consumer = consumer;
  }

  @Override public ObjectHandler comment(String comment) {
    consumer.accept("comment(" + comment + ")");
    return this;
  }

  @Override public ObjectHandler number(String propertyName, Number value) {
    consumer.accept("number(" + propertyName + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler string(String propertyName, String value) {
    consumer.accept("string(" + propertyName + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler identifier(String propertyName, String value) {
    consumer.accept("identifier(" + propertyName + ", " + value + ")");
    return this;
  }

  @Override public ObjectHandler code(String propertyName, String value) {
    consumer.accept("code(" + propertyName + ", " + value + ")");
    return this;
  }

  @Override public ListHandler listOpen(String propertyName) {
    consumer.accept("listOpen(" + propertyName + ")");
    return new LoggingListHandler(consumer);
  }

  @Override public ObjectHandler objOpen(String propertyName) {
    consumer.accept("objOpen(" + propertyName + ")");
    return this;
  }

  @Override public ObjectHandler objOpen(String propertyName, String name) {
    consumer.accept("objOpen(" + propertyName + ", " + name + ")");
    return this;
  }

  @Override public void close() {
    consumer.accept("objClose()");
  }

  /** Handler for the root of the document. Its behavior is as
   * {@link LoggingObjectHandler} except that {@link #close()} does not
   * generate a message. */
  private static class RootLoggingObjectHandler extends LoggingObjectHandler {
    RootLoggingObjectHandler(Consumer<String> consumer) {
      super(consumer);
    }

    @Override public ObjectHandler objOpen(String propertyName) {
      consumer.accept("objOpen(" + propertyName + ")");
      return new LoggingObjectHandler(consumer);
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      consumer.accept("objOpen(" + propertyName + ", " + name + ")");
      return new LoggingObjectHandler(consumer);
    }

    @Override public void close() {
      // swallows the 'onClose()' message
    }
  }
}

// End LoggingObjectHandler.java
