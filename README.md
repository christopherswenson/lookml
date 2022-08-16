<!--
{% comment %}
Licensed to the LookML Authors under one or more contributor
license agreements.  See the NOTICE file distributed with this
work for additional information regarding copyright ownership.
The LookML Authors license this file to you under the Apache
License, Version 2.0 (the "License"); you may not use this
file except in compliance with the License.  You may obtain a
copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied.  See the License for the specific
language governing permissions and limitations under the
License.
{% endcomment %}
-->
[![Build Status](https://github.com/hydromatic/lookml/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/hydromatic/lookml/actions?query=branch%3Amain)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.hydromatic/lookml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.hydromatic/lookml)
[![javadoc](https://javadoc.io/badge2/net.hydromatic/lookml/javadoc.svg)](https://javadoc.io/doc/net.hydromatic/lookml)

# LookML

Compiler toolchain for the LookML language.

## Get LookML

### From Maven

Get LookML from
<a href="https://search.maven.org/#search%7Cga%7C1%7Ca%3Alookml">Maven central</a>:

```xml
<dependency>
  <groupId>net.hydromatic</groupId>
  <artifactId>lookml</artifactId>
  <version>0.1</version>
</dependency>
```

### Download and build

You need Java (8 or higher) and Git.

```bash
$ git clone git://github.com/hydromatic/lookml.git
$ cd lookml
$ ./mvnw clean verify
```

On Windows, the last line is

```bash
> mvnw clean verify
```

On Java versions less than 11, you should add parameters
`-Dcheckstyle.version=9.3`.

## More information

* License: <a href="LICENSE">Apache Software License, Version 2.0</a>
* Author: the LookML Authors
* Source code: https://github.com/hydromatic/lookml
* Issues: https://github.com/hydromatic/lookml/issues
* <a href="HISTORY.md">Release notes and history</a>
* <a href="HOWTO.md">HOWTO</a>
