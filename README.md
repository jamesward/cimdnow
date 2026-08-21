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

## Hosted service

A public instance runs at **<https://www.cimd.now>** — you don't need to deploy
anything to use it.

Your client's `client_id` is simply the metadata URL that encodes the port (and
optional path) your local client listens on:

```
https://www.cimd.now/<port>            ->  redirect_uri http://localhost:<port>/
https://www.cimd.now/<port>/<path>     ->  redirect_uri http://localhost:<port>/<path>
```

By default the redirect host is `localhost`. If your client binds to a loopback
IP literal instead — as RFC 8252 §7.3/§8.3 recommends for native apps — prefix
the port with the host:

```
https://www.cimd.now/127.0.0.1:<port>/<path>  ->  redirect_uri http://127.0.0.1:<port>/<path>
https://www.cimd.now/[::1]:<port>/<path>      ->  redirect_uri http://[::1]:<port>/<path>
```

Only the loopback hosts `localhost`, `127.0.0.1`, and `[::1]` are accepted; any
other host returns `400`.

Hand that URL to your OAuth provider as the `client_id`. The provider fetches
it, reads the metadata document below, and redirects back to your local client
after authorization.

```console
$ curl -s https://www.cimd.now/8080/callback | jq
{
  "client_name": "cimdtest",
  "client_uri": "https://www.cimd.now",
  "grant_types": ["authorization_code", "refresh_token"],
  "response_types": ["code"],
  "token_endpoint_auth_method": "none",
  "application_type": "native",
  "client_id": "https://www.cimd.now/8080/callback",
  "redirect_uris": ["http://localhost:8080/callback"]
}
```

For example, a CLI client listening on `localhost:8080/callback` would start
authorization with:

```
client_id=https://www.cimd.now/8080/callback
redirect_uri=http://localhost:8080/callback
```

Everything below is for running your own instance.

## Endpoints

| Method & path            | Response                                                                 |
| ------------------------ | ------------------------------------------------------------------------ |
| `GET /:port`             | Client metadata JSON with `redirect_uris = [http://localhost::port/]`    |
| `GET /:port/:path`       | Same, with `redirect_uris = [http://localhost::port/:path]`              |
| `GET /:host::port/:path` | Same, with `:host` in `redirect_uris`; `:host` must be a loopback host   |
