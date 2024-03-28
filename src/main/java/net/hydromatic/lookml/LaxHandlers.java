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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

  /** Creates a writer.
   *
   * @param buf String builder to which to write the LookML
   * @param offset Number of spaces to increase indentation each time we enter
   *              a nested object or list
   * @param pretty Whether to pretty-print (with newlines and indentation) */
  public static PropertyHandler writerTyped(StringBuilder buf, int offset,
      boolean pretty) {
    return untype(writer(buf, offset, pretty));
  }

  /** Creates an object handler that writes each event,
   * as a string, to a consumer. */
  public static ObjectHandler logger(Consumer<String> consumer,
      boolean includePos) {
    return LoggingObjectHandler.create(consumer, includePos);
  }

  /** Creates a handler that writes each event,
   * as a string without position information, to a consumer. */
  public static ObjectHandler logger(Consumer<String> consumer) {
    return logger(consumer, false);
  }

  /** Creates a property handler that writes each event,
   * as a string, to a consumer. */
  public static PropertyHandler loggerTyped(Consumer<String> consumer,
      boolean includePos) {
    return LoggingPropertyHandler.root(consumer, includePos);
  }

  /** Creates a handler that writes each event,
   * as a string, to a consumer. */
  public static PropertyHandler loggerTyped(Consumer<String> consumer) {
    return loggerTyped(consumer, false);
  }

  /** Creates a handler that writes each error event, as a string,
   * to a consumer. */
  public static ErrorHandler errorLogger(Consumer<String> list) {
    return new LoggingErrorHandler(list);
  }

  /** Creates an object handler that writes each event to a consumer. */
  public static ObjectHandler filter(ObjectHandler consumer) {
    return new FilterObjectHandler(consumer);
  }

  /** Creates a property handler that writes to an object handler. */
  public static PropertyHandler untype(ObjectHandler consumer) {
    return new UntypingHandler(consumer);
  }

  /** Creates a handler that writes each event to several object handlers. */
  public static ObjectHandler tee(ObjectHandler... consumers) {
    return new TeeObjectHandler(ImmutableList.copyOf(consumers));
  }

  /** Creates a handler that writes each event to several property handlers. */
  public static PropertyHandler tee(PropertyHandler... consumers) {
    return new TeePropertyHandler(ImmutableList.copyOf(consumers));
  }

  /** Creates a handler that validates each event against a
   * {@link LookmlSchema}. */
  public static ObjectHandler validator(PropertyHandler consumer,
      LookmlSchema schema, ErrorHandler errorHandler) {
    return ValidatingHandler.create(schema, consumer, errorHandler);
  }

  /** Creates a handler that validates each event against a
   * {@link LookmlSchema} and writes to an {@link ObjectHandler} consumer. */
  public static ObjectHandler validator(ObjectHandler consumer,
      LookmlSchema schema, ErrorHandler errorHandler) {
    PropertyHandler propertyHandler = untype(consumer);
    return ValidatingHandler.create(schema, propertyHandler, errorHandler);
  }

  /** Checks whether the input contains at least one instance of every
   * property. */
  public static PropertyHandler completenessChecker(LookmlSchema schema,
      Consumer<LookmlSchema.Property> propertiesSeen) {
    return new RootCompletenessChecker(schema, propertiesSeen);
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

    @Override public ObjectBuilder comment(Pos pos, String comment) {
      // ignore comment
      return this;
    }

    @Override public ObjectBuilder number(Pos pos, String propertyName,
        Number value) {
      properties.add(propertyName, Values.number(value));
      return this;
    }

    @Override public ObjectBuilder string(Pos pos, String propertyName,
        String value) {
      properties.add(propertyName, Values.string(value));
      return this;
    }

    @Override public ObjectBuilder identifier(Pos pos, String propertyName,
        String value) {
      properties.add(propertyName, Values.identifier(value));
      return this;
    }

    @Override public ObjectBuilder code(Pos pos, String propertyName,
        String value) {
      properties.add(propertyName, Values.code(value));
      return this;
    }

    @Override public ListBuilder listOpen(Pos pos, String propertyName) {
      return new ListBuilder(list ->
          properties.add(propertyName, Values.list(list)));
    }

    @Override public ObjectHandler objOpen(Pos pos, String propertyName,
        String name) {
      return new ObjectBuilder(properties ->
          this.properties.add(propertyName,
              Values.namedObject(name, properties)));
    }

    @Override public ObjectHandler objOpen(Pos pos, String propertyName) {
      return new ObjectBuilder(properties ->
          this.properties.add(propertyName,
              Values.object(properties)));
    }

    @Override public void close(Pos pos) {
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

    @Override public ListHandler string(Pos pos, String value) {
      list.add(Values.string(value));
      return this;
    }

    @Override public ListHandler number(Pos pos, Number value) {
      list.add(Values.number(value));
      return this;
    }

    @Override public ListHandler identifier(Pos pos, String value) {
      list.add(Values.identifier(value));
      return this;
    }

    @Override public ListHandler pair(Pos pos, String ref, String identifier) {
      list.add(Values.pair(ref, identifier));
      return this;
    }

    @Override public ListHandler comment(Pos pos, String comment) {
      // Ignore the comment
      return this;
    }

    @Override public ListHandler listOpen(Pos pos) {
      return new ListBuilder(list -> this.list.add(Values.list(list)));
    }

    @Override public void close(Pos pos) {
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

  /** Implementation of {@link PropertyHandler}
   * that writes to an {@link ObjectHandler}.
   *
   * <p>This allows you to reverse the effects of schema validation, and go
   * from typed to untyped events. Raw values are given {@link Value} wrappers,
   * so that they can be interpreted correctly (say as string, identifier or
   * enum) without accompanying properties.
   */
  private static class UntypingHandler implements PropertyHandler {
    private final ObjectHandler consumer;

    UntypingHandler(ObjectHandler consumer) {
      this.consumer = consumer;
    }

    @Override public PropertyHandler property(Pos pos,
        LookmlSchema.Property property, Object value) {
      switch (property.type()) {
      case STRING:
        consumer.string(pos, property.name(), (String) value);
        break;
      case CODE:
        consumer.code(pos, property.name(), (String) value);
        break;
      case NUMBER:
        consumer.number(pos, property.name(), (Number) value);
        break;
      case ENUM:
      case REF:
        consumer.identifier(pos, property.name(), (String) value);
        break;
      case REF_LIST:
        // It's not true that each element has the same position, but it's all
        // the information we have.
        final ListHandler listHandler = consumer.listOpen(pos, property.name());
        ((List<String>) value).forEach(v -> listHandler.identifier(pos, v));
        break;
      default:
        throw new AssertionError(property.type());
      }
      return this;
    }

    @Override public ListHandler listOpen(Pos pos,
        LookmlSchema.Property property) {
      return consumer.listOpen(pos, property.name());
    }

    @Override public PropertyHandler objOpen(Pos pos,
        LookmlSchema.Property property) {
      final ObjectHandler subConsumer = consumer.objOpen(pos, property.name());
      return new UntypingHandler(subConsumer);
    }

    @Override public PropertyHandler objOpen(Pos pos,
        LookmlSchema.Property property, String name) {
      final ObjectHandler subConsumer =
          consumer.objOpen(pos, property.name(), name);
      return new UntypingHandler(subConsumer);
    }

    @Override public void close(Pos pos) {
      consumer.close(pos);
    }
  }

  /** Property handler that notes each property seen while traversing a
   * document, and at the end outputs all properties in the schema that were
   * never seen. */
  private static class CompletenessChecker implements PropertyHandler {
    final Set<LookmlSchema.Property> propertiesSeen;

    private CompletenessChecker(Set<LookmlSchema.Property> propertiesSeen) {
      this.propertiesSeen = propertiesSeen;
    }

    @Override public PropertyHandler property(Pos pos,
        LookmlSchema.Property property, Object value) {
      propertiesSeen.add(property);
      return this;
    }

    @Override public ListHandler listOpen(Pos pos,
        LookmlSchema.Property property) {
      propertiesSeen.add(property);
      return nullListHandler(); // we don't care about list elements
    }

    @Override public PropertyHandler objOpen(Pos pos,
        LookmlSchema.Property property) {
      propertiesSeen.add(property);
      return new CompletenessChecker(propertiesSeen);
    }

    @Override public PropertyHandler objOpen(Pos pos,
        LookmlSchema.Property property, String name) {
      propertiesSeen.add(property);
      return new CompletenessChecker(propertiesSeen);
    }

    @Override public void close(Pos pos) {
      // nothing to do
    }
  }

  /** The root of a tree of {@link CompletenessChecker}. It contains state
   * shared with the whole tree, and outputs not-seen properties on close. */
  private static class RootCompletenessChecker extends CompletenessChecker {
    final LookmlSchema schema;
    final Consumer<LookmlSchema.Property> propertiesSeenConsumer;

    private RootCompletenessChecker(LookmlSchema schema,
        Consumer<LookmlSchema.Property> propertiesSeenConsumer) {
      super(new LinkedHashSet<>());
      this.schema = schema;
      this.propertiesSeenConsumer = propertiesSeenConsumer;
    }

    @Override public void close(Pos pos) {
      propertiesSeen.forEach(propertiesSeenConsumer);
    }
  }

}

// End LaxHandlers.java
