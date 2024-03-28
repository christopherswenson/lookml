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
  protected final boolean includePos;
  protected final ListHandler listHandler;

  static ObjectHandler create(Consumer<String> consumer, boolean includePos) {
    return new RootLoggingObjectHandler(consumer, includePos,
        new LoggingListHandler(consumer, includePos));
  }

  /** Appends {@code pos} to a builder if {@code position} is true. */
  static String appendPos(Pos pos, boolean includePos, StringBuilder b) {
    if (includePos) {
      b.append(" at ");
      pos.describeTo2(b);
    }
    return b.toString();
  }

  private LoggingObjectHandler(Consumer<String> consumer, boolean includePos,
      ListHandler listHandler) {
    this.consumer = consumer;
    this.includePos = includePos;
    this.listHandler = listHandler;
  }

  @Override public ObjectHandler comment(Pos pos, String comment) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("comment(").append(comment).append(")")));
    return this;
  }

  @Override public ObjectHandler number(Pos pos, String propertyName,
        Number value) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("number(").append(propertyName)
                .append(", ").append(value).append(")")));
    return this;
  }

  @Override public ObjectHandler string(Pos pos, String propertyName,
        String value) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("string(").append(propertyName)
                .append(", ").append(value).append(")")));
    return this;
  }

  @Override public ObjectHandler identifier(Pos pos, String propertyName,
        String value) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("identifier(").append(propertyName)
                .append(", ").append(value).append(")")));
    return this;
  }

  @Override public ObjectHandler code(Pos pos, String propertyName,
        String value) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("code(").append(propertyName)
                .append(", ").append(value).append(")")));
    return this;
  }

  @Override public ListHandler listOpen(Pos pos, String propertyName) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("listOpen(").append(propertyName).append(")")));
    return listHandler;
  }

  @Override public ObjectHandler objOpen(Pos pos, String propertyName) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("objOpen(").append(propertyName).append(")")));
    return this;
  }

  @Override public ObjectHandler objOpen(Pos pos, String propertyName,
        String name) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("objOpen(").append(propertyName)
                .append(", ").append(name).append(")")));
    return this;
  }

  @Override public void close(Pos pos) {
    consumer.accept(
        appendPos(pos, includePos,
            new StringBuilder("objClose()")));
  }

  /** Handler for the root of the document. Its behavior is as
   * {@link LoggingObjectHandler} except that {@link #close(Pos)} does not
   * generate a message. */
  private static class RootLoggingObjectHandler extends LoggingObjectHandler {
    RootLoggingObjectHandler(Consumer<String> consumer, boolean includePos,
        LoggingListHandler loggingListHandler) {
      super(consumer, includePos, loggingListHandler);
    }

    @Override public ObjectHandler objOpen(Pos pos, String propertyName) {
      consumer.accept(
          appendPos(pos, includePos,
              new StringBuilder("objOpen(").append(propertyName).append(")")));
      return new LoggingObjectHandler(consumer, includePos, listHandler);
    }

    @Override public ObjectHandler objOpen(Pos pos, String propertyName,
        String name) {
      consumer.accept(
          appendPos(pos, includePos,
              new StringBuilder("objOpen(").append(propertyName)
                  .append(", ").append(name).append(")")));
      return new LoggingObjectHandler(consumer, includePos, listHandler);
    }

    @Override public void close(Pos pos) {
      // swallows the 'onClose()' message
    }
  }
}

// End LoggingObjectHandler.java
