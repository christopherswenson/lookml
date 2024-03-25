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

/**
 * LookML value in a property or a list.
 *
 * <p>This interface is intentionally opaque. There are no defined methods,
 * and the client of the framework must not create instances of {@code Value};
 * the framework assumes that any instance of {@code Value} was created by
 * itself, using one of its private subclasses.
 *
 * <p>For a client, the only purpose of the {@code Value} interface is to pass
 * values to and from a builder (see {@link LaxHandlers#build(Consumer)}).
 */
public interface Value {
}

// End Value.java
