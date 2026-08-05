{ pkgs ? import <nixpkgs> { } }:

# Dev shell for building this Scala Native project on NixOS.
#
# kyo-http bundles a TLS shim (kyo_tls.c) that #includes <openssl/ssl.h>, and
# kyo-net bundles an io_uring shim (kyo_uring.c) guarded by
# `#if defined(__linux__) && __has_include(<liburing.h>)`. Both are compiled by
# the C compiler Scala Native drives. On NixOS there is no /usr/include, so that
# compiler can't find the OpenSSL or liburing headers/libs on its own — and if
# <liburing.h> is missing, kyo_uring.c compiles to nothing and the native link
# fails with undefined `kyo_uring_*` references.
#
# We expose the headers/libs via the standard C_INCLUDE_PATH / LIBRARY_PATH
# variables, which any clang honors (whether or not it's the Nix cc-wrapper).
# Combined with the `-lssl -lcrypto -luring` linking options in build.sbt, this
# satisfies both the compile and link steps.
pkgs.mkShell {
  # Scala Native toolchain + OpenSSL + liburing.
  packages = [
    pkgs.clang
    pkgs.llvm
    pkgs.openssl
    pkgs.liburing
  ];

  # Scala Native compiles the bundled C shims in debug mode (no -O). The Nix
  # cc-wrapper injects -D_FORTIFY_SOURCE by default, which requires optimization
  # and otherwise just emits a noisy glibc warning. Turn it off for this shell.
  hardeningDisable = [ "fortify" ];

  shellHook = ''
    export C_INCLUDE_PATH="${pkgs.openssl.dev}/include:${pkgs.liburing.dev}/include:''${C_INCLUDE_PATH:-}"
    export CPLUS_INCLUDE_PATH="${pkgs.openssl.dev}/include:${pkgs.liburing.dev}/include:''${CPLUS_INCLUDE_PATH:-}"
    export LIBRARY_PATH="${pkgs.lib.getLib pkgs.openssl}/lib:${pkgs.lib.getLib pkgs.liburing}/lib:''${LIBRARY_PATH:-}"
  '';
}
