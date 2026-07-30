enablePlugins(ScalaNativePlugin)

name         := "cimdtest"
scalaVersion := "3.8.4"

libraryDependencies ++= Seq(
    "io.getkyo" %% "kyo-core" % "1.0.0-RC5",
    "io.getkyo" %% "kyo-http" % "1.0.0-RC5"
)

// Required by Kyo: https://github.com/getkyo/kyo#getting-started
scalacOptions ++= Seq(
    "-Wvalue-discard",
    "-Wnonunit-statement",
    "-Wconf:msg=(unused.*value|discarded.*value|pure.*statement):error",
    "-language:strictEquality"
)

// kyo-http bundles a TLS shim (native/kyo_tls.c) that's compiled unconditionally, even
// though this app doesn't use TLS. Link against the system OpenSSL to satisfy it.
//
// -Wl,-z,noexecstack: Scala Native's hand-written asm (e.g. SafepointPollTrampoline)
// lacks a .note.GNU-stack section, so GNU ld warns it may need an executable stack.
// This flag forces a non-executable stack, silencing the warning and being safer.
nativeConfig ~= { c =>
    c.withLinkingOptions(
        c.linkingOptions ++ Seq("-lssl", "-lcrypto", "-Wl,-z,noexecstack")
    )
}
