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

import net.hydromatic.lookml.util.PairList;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Various implementations of {@link ObjectHandler} */
public class LaxHandlers {
  private LaxHandlers() {}

  /** Creates a writer.
   *
   * @param buf String builder to which to write the LookML
   * @param offset Number of spaces to increase indentation each time we enter
   *              a nested object or list
   * @param pretty Whether to pretty-print (with newlines and indentation) */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static ObjectHandler writer(StringBuilder buf, int offset,
      boolean pretty) {
    final LookmlWriter sink = new LookmlWriter(pretty, buf, offset);
    return build(pairList ->
        // Cast is valid because subclasses of ValueImpl are the only valid
        // implementations of interface Value.
        sink.propertyList((PairList<String, ValueImpl>) (PairList) pairList));
  }

  /** Creates a handler that writes each event,
   * as a string, to a consumer. */
  public static ObjectHandler logger(Consumer<String> consumer) {
    return LoggingObjectHandler.create(consumer);
  }

  /** Creates a handler that writes each event to a consumer. */
  public static ObjectHandler filter(ObjectHandler consumer) {
    return new FilterObjectHandler(consumer);
  }

  /** Creates a handler that writes each event to several object handlers. */
  public static ObjectHandler tee(ObjectHandler... consumers) {
    return new TeeObjectHandler(ImmutableList.copyOf(consumers));
  }

  /** Creates a list handler that swallows all events. */
  public static ListHandler nullListHandler() {
    return NullListHandler.INSTANCE;
  }

  /** Creates an object handler that swallows all events. */
  public static ObjectHandler nullObjectHandler() {
    return NullObjectHandler.INSTANCE;
  }

  /** Creates an ObjectHandler that converts events into a document. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static ObjectHandler build(
      Consumer<PairList<String, Value>> consumer) {
    return new ObjectBuilder(
        (Consumer<PairList<String, ValueImpl>>) (Consumer) consumer);
  }

  /** Implementation of {@link ObjectHandler}
   * that builds a list of properties,
   * then calls a consumer on the completed list. */
  static class ObjectBuilder implements ObjectHandler {
    final PairList<String, ValueImpl> properties = PairList.of();
    final Consumer<PairList<String, ValueImpl>> onClose;

    ObjectBuilder(Consumer<PairList<String, ValueImpl>> onClose) {
      this.onClose = onClose;
    }

    @Override public ObjectBuilder comment(String comment) {
      // ignore comment
      return this;
    }

    @Override public ObjectBuilder number(String propertyName, Number value) {
      properties.add(propertyName, Values.number(value));
      return this;
    }

    @Override public ObjectBuilder string(String propertyName, String value) {
      properties.add(propertyName, Values.string(value));
      return this;
    }

    @Override public ObjectBuilder identifier(String propertyName,
        String value) {
      properties.add(propertyName, Values.identifier(value));
      return this;
    }

    @Override public ObjectBuilder code(String propertyName, String value) {
      properties.add(propertyName, Values.code(value));
      return this;
    }

    @Override public ListBuilder listOpen(String propertyName) {
      return new ListBuilder(list ->
          properties.add(propertyName, Values.list(list)));
    }

    @Override public ObjectHandler objOpen(String propertyName, String name) {
      return new ObjectBuilder(properties ->
          this.properties.add(propertyName,
              Values.namedObject(name, properties)));
    }

    @Override public ObjectHandler objOpen(String propertyName) {
      return new ObjectBuilder(properties ->
          this.properties.add(propertyName,
              Values.object(properties)));
    }

    @Override public void close() {
      onClose.accept(properties);
    }
  }

  /** Implementation of {@link ListHandler}
   * that builds a list of values,
   * then calls a consumer when done. */
  static class ListBuilder implements ListHandler {
    final Consumer<List<ValueImpl>> onClose;
    final List<ValueImpl> list = new ArrayList<>();

    ListBuilder(Consumer<List<ValueImpl>> onClose) {
      this.onClose = onClose;
    }

    @Override public ListHandler string(String value) {
      list.add(Values.string(value));
      return this;
    }

    @Override public ListHandler number(Number value) {
      list.add(Values.number(value));
      return this;
    }

    @Override public ListHandler identifier(String value) {
      list.add(Values.identifier(value));
      return this;
    }

    @Override public ListHandler pair(String ref, String identifier) {
      list.add(Values.pair(ref, identifier));
      return this;
    }

    @Override public ListHandler comment(String comment) {
      // Ignore the comment
      return this;
    }

    @Override public ListHandler listOpen() {
      return new ListBuilder(list -> this.list.add(Values.list(list)));
    }

    @Override public void close() {
      onClose.accept(list);
    }
  }

  /** Implementation of {@link ListHandler}
   * that discards all events. */
  enum NullListHandler implements ListHandler {
    INSTANCE
  }

  /** Implementation of {@link ObjectHandler}
   * that discards all events. */
  enum NullObjectHandler implements ObjectHandler {
    INSTANCE
  }
}

// End LaxHandlers.java
