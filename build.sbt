// ReloadPlugin (sbt-reload) auto-activates on any JVM-enabled project. We keep
// it for JVM dev (`sbt ~runReload`) and disable it in native mode below.
import com.jamesward.sbtreload.ReloadPlugin

lazy val scala3     = "3.8.4"
lazy val kyoVersion = "1.0.0-RC6"

// This app builds from ONE source tree as EITHER a Scala Native binary OR a JVM
// app, chosen purely by detection (see NativeLinking.nativeEnabled — true when
// the native headers are present, i.e. on Heroku or in the nix dev shell).
// build.sbt only decides whether to ENABLE ScalaNativePlugin; the native link
// options live in project/NativeLinking.scala (an AutoPlugin that requires
// ScalaNativePlugin), so `nativeConfig` is set only when the plugin is enabled.
enablePlugins((if (NativeLinking.nativeEnabled) Seq[AutoPlugin](ScalaNativePlugin) else Nil)*)
disablePlugins((if (NativeLinking.nativeEnabled) Seq[AutoPlugin](ReloadPlugin) else Nil)*)

name         := "cimdnow"
scalaVersion := scala3

libraryDependencies ++= Seq(
  // `%%` resolves the right artifact automatically: kyo-core_native0.5_3 when
  // ScalaNativePlugin is enabled, kyo-core_3 on the JVM.
  "io.getkyo" %% "kyo-core" % kyoVersion,
  "io.getkyo" %% "kyo-http" % kyoVersion,
)

// Required by Kyo: https://github.com/getkyo/kyo#getting-started
scalacOptions ++= Seq(
  "-Wvalue-discard",
  "-Wnonunit-statement",
  "-Wconf:msg=(unused.*value|discarded.*value|pure.*statement):error",
  "-language:strictEquality"
)
