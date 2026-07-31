# cimdtest

A tiny [Scala Native](https://scala-native.org/) HTTP server that generates
**OAuth Client ID Metadata Documents (CIMD)** on the fly, built with
[Kyo](https://getkyo.io/) and [`kyo-http`](https://github.com/getkyo/kyo).

## What it does

With CIMD, an OAuth client's `client_id` is itself a URL that serves the
client's metadata document. This server does exactly that: request it at a URL
that encodes the local port (and optional path) your client listens on, and it
returns a metadata document whose `client_id` is the request URL and whose
`redirect_uris` point back at your local client.

This is handy for developing native/CLI OAuth clients against providers that
support CIMD, without pre-registering a client.

## Endpoints

| Method & path            | Response                                                                 |
| ------------------------ | ------------------------------------------------------------------------ |
| `GET /`                  | `302` redirect to <https://github.com/jamesward/cimdapp>                 |
| `GET /:port`             | Client metadata JSON with `redirect_uris = [http://localhost::port/]`    |
| `GET /:port/:path`       | Same, with `redirect_uris = [http://localhost::port/:path]`              |

The request `Host` and `X-Forwarded-Proto` headers determine the `client_id`
(so it reflects the public URL when running behind a proxy). A non-numeric or
out-of-range port yields `400 Bad Request`.

### Example

```console
$ curl -s http://localhost:8080/9090/callback | jq
{
  "client_name": "cimdtest",
  "client_uri": "https://www.cimd.now",
  "grant_types": ["authorization_code", "refresh_token"],
  "response_types": ["code"],
  "token_endpoint_auth_method": "none",
  "application_type": "native",
  "client_id": "http://localhost:8080/9090/callback",
  "redirect_uris": ["http://localhost:9090/callback"]
}
```

## Tech stack

- Scala 3.8.4 on Scala Native 0.5.12
- kyo-core / kyo-http 1.0.0-RC5
- sbt 2.0.4

## Prerequisites

Scala Native compiles to a native binary via **clang**, and `kyo-http` bundles a
TLS shim (`kyo_tls.c`) that needs **OpenSSL** headers and libraries.

- A JDK (25+, required by Kyo's macros) and sbt — the repo ships an `./sbt` launcher.
- `clang`
- OpenSSL development files:
  - Debian/Ubuntu: `sudo apt-get install clang libssl-dev`
  - macOS: `brew install llvm openssl`
  - **NixOS**: use the provided `shell.nix` (see below)

## Build and run

```bash
# Compile
sbt compile

# Produce the native binary
sbt nativeLink

# Run it (defaults to port 8080)
./target/out/native0.5/scala-3.8.4/cimdtest/cimdtest

# Or run directly through sbt
sbt run
```

### Configuration

| Variable | Default | Description                    |
| -------- | ------- | ------------------------------ |
| `PORT`   | `8080`  | Port to bind on (`0.0.0.0`).   |

## NixOS

There is no `/usr/include` on NixOS, so the OpenSSL headers must be provided
through Nix. A `shell.nix` is included that puts OpenSSL on the compiler's
search path and disables the `fortify` hardening flag (which is noisy in debug
builds). Build from inside it:

```bash
nix-shell --run "sbt nativeLink"
```

Note: `sbt` runs a persistent server. If you switch build environments, shut it
down (or kill stray `sbt`/`sbtn` processes) before rebuilding so the native
toolchain is picked up from the current shell.

## CI and dependencies

- [`.github/workflows/ci.yml`](.github/workflows/ci.yml) — on push/PR to `main`,
  installs the native toolchain, then runs `sbt compile` and `sbt nativeLink`.
- [`.github/dependabot.yml`](.github/dependabot.yml) — weekly version-update PRs
  for the `sbt` and `github-actions` ecosystems.

## Project layout

```
src/main/scala/Main.scala   # server: routes + ClientMetadata model
build.sbt                   # deps, Scala Native linking options
shell.nix                   # NixOS dev shell (clang + OpenSSL)
.github/                    # CI workflow + Dependabot config
```
