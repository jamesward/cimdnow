{ pkgs ? import <nixpkgs> { } }:

# Dev shell for building this Scala Native project on NixOS.
#
# kyo-http bundles a TLS shim (native/kyo_tls.c) that #includes <openssl/ssl.h>
# and is compiled unconditionally. On NixOS there is no /usr/include, so the C
# compiler Scala Native drives can't find the OpenSSL headers/libs on its own.
#
# We expose them via the standard C_INCLUDE_PATH / LIBRARY_PATH variables, which
# any clang honors (whether or not it's the Nix cc-wrapper). Combined with the
# `-lssl -lcrypto` linking options in build.sbt, this satisfies both the compile
# and link steps.
pkgs.mkShell {
  # Scala Native toolchain + OpenSSL.
  packages = [
    pkgs.clang
    pkgs.llvm
    pkgs.openssl
  ];

  # Scala Native compiles the bundled C shims in debug mode (no -O). The Nix
  # cc-wrapper injects -D_FORTIFY_SOURCE by default, which requires optimization
  # and otherwise just emits a noisy glibc warning. Turn it off for this shell.
  hardeningDisable = [ "fortify" ];

  shellHook = ''
    export C_INCLUDE_PATH="${pkgs.openssl.dev}/include:''${C_INCLUDE_PATH:-}"
    export CPLUS_INCLUDE_PATH="${pkgs.openssl.dev}/include:''${CPLUS_INCLUDE_PATH:-}"
    export LIBRARY_PATH="${pkgs.lib.getLib pkgs.openssl}/lib:''${LIBRARY_PATH:-}"
  '';
}
