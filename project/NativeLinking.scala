import sbt.*
import sbt.Keys.*
import scala.scalanative.sbtplugin.ScalaNativePlugin
import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport.*

/**
 * Scala Native selection + link configuration for this app, factored into an
 * AutoPlugin so build.sbt stays declarative.
 *
 * `requires = ScalaNativePlugin` + `trigger = allRequirements` means the link
 * settings here are injected ONLY on projects where ScalaNativePlugin is
 * enabled. So build.sbt never references `nativeConfig` (which does not exist
 * unless the native plugin is enabled): it enables ScalaNativePlugin when
 * `NativeLinking.nativeEnabled` is true, and this plugin rides along.
 *
 * kyo-http bundles a TLS shim (kyo_tls.c, needs OpenSSL) and kyo-net bundles an
 * io_uring shim (kyo_uring.c, needs liburing); Scala Native compiles both, so we
 * link -lssl -lcrypto and liburing. -Wl,-z,noexecstack forces a non-executable
 * stack (Scala Native's hand-written asm lacks a .note.GNU-stack section).
 */
object NativeLinking extends AutoPlugin {
  override def requires: Plugins      = ScalaNativePlugin
  override def trigger: PluginTrigger = allRequirements

  // Is `fileName` present in any directory drawn from the given env vars
  // (path-separator split) plus the given fallback directories?
  private def onSearchPath(envVars: Seq[String], fallback: Seq[String], fileName: String): Boolean =
    (envVars.flatMap(sys.env.get).flatMap(_.split(java.io.File.pathSeparatorChar).toSeq) ++ fallback)
      .filter(_.nonEmpty)
      .exists(dir => new java.io.File(dir, fileName).isFile)

  private def headerOnIncludePath(header: String): Boolean =
    onSearchPath(
      Seq("C_INCLUDE_PATH", "CPATH", "CPLUS_INCLUDE_PATH"),
      Seq("/usr/include", "/usr/local/include", "/usr/include/x86_64-linux-gnu"),
      header
    )

  private def libOnSearchPath(fileName: String): Boolean =
    onSearchPath(
      Seq("LIBRARY_PATH", "LD_LIBRARY_PATH"),
      Seq("/usr/lib", "/usr/lib/x86_64-linux-gnu", "/usr/local/lib"),
      fileName
    )

  /**
   * True when the native toolchain headers are present, so this app should build
   * as Scala Native rather than JVM (pure detection, no env vars):
   *   * on Heroku — buildpack-scala-native installs OpenSSL + liburing and puts
   *     them on C_INCLUDE_PATH, and
   *   * in the nix dev shell — shell.nix does the same.
   * build.sbt reads this to decide whether to enable ScalaNativePlugin.
   */
  val nativeEnabled: Boolean =
    headerOnIncludePath("liburing.h") && headerOnIncludePath("openssl/ssl.h")

  // Link liburing statically when a static archive is on the library search path
  // (Heroku: apt liburing-dev ships liburing.a -> self-contained slug); otherwise
  // link the shared library (nix dev shell, which provides liburing.so).
  private val uringLinkOption: String =
    if (libOnSearchPath("liburing.a")) "-l:liburing.a" else "-luring"

  override def projectSettings: Seq[Setting[?]] = Seq(
    nativeConfig ~= { c =>
      c.withLinkingOptions(
        c.linkingOptions ++ Seq("-lssl", "-lcrypto", uringLinkOption, "-Wl,-z,noexecstack")
      )
    }
  )
}
