# sbt-shading


[![Build Status](https://travis-ci.org/coursier/sbt-shading.svg?branch=master)](https://travis-ci.org/coursier/sbt-shading)
[![Maven Central](https://img.shields.io/maven-central/v/io.get-coursier/lm-coursier_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/io.get-coursier/lm-coursier_2.12)

Use like
```scala
// Enable the plugin
enablePlugins(ShadingPlugin)

// Add the dependencies to shade along the other (non-shaded) ones
libraryDependencies += "io.argonaut" %% "argonaut" % "6.2"

// Tell the plugin to shade some dependencies.
// This also shades all their transitive dependencies, except those
// that are also brought by non-shaded dependencies.
// This merges those dependencies JARs in our output JAR, along with our
// classes.
shadedModules += "io.argonaut" %% "argonaut"

// Tell the plugin to rename some namespaces in the output JAR.
// This renames any of our classes in this namespace, and adjust
// any reference in this namespace.
// Be sure that all classes in this namespace are effectively
// in the output JAR.
shadingRules += ShadingRule.moveUnder("argonaut", "test.shaded")

// Tell the plugin that the output JAR can contain classes in that
// namespace. Fail loudly if it find any class outside of it.
validNamespaces += "test"
```

