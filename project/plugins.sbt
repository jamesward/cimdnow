addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.12")

// Hot reload for local JVM dev: `./sbt ~cimdtest/runReload` restarts the app
// on source changes. sbt 2.x plugin (revolver-style); auto-activates on JVM rows.
addSbtPlugin("com.jamesward" % "sbt-reload" % "0.0.7")
addSbtPlugin("com.jamesward" % "sbt-mcp" % "0.1.3")
addSbtPlugin("com.skillsjars" % "skillsjars-sbt-plugin" % "0.0.9")
