import kyo.*
import kyo.schema.*

case class ClientMetadata(
    @rename("client_name") clientName: String,
    @rename("client_uri") clientUri: String,
    @rename("grant_types") grantTypes: List[String],
    @rename("response_types") responseTypes: List[String],
    @rename("token_endpoint_auth_method") tokenEndpointAuthMethod: String,
    @rename("application_type") applicationType: String,
    @rename("client_id") clientId: String,
    @rename("redirect_uris") redirectUris: List[String]
) derives Schema

object Main extends KyoApp:

    private val port = sys.env.get("PORT").flatMap(_.toIntOption).getOrElse(8080)

    // Only loopback hosts are permitted in a redirect_uri. Allowing arbitrary
    // hosts would let a caller mint a client_id under this trusted domain whose
    // redirect_uri points anywhere (CIMD draft §8.1, client impersonation). The
    // IP literals are the forms RFC 8252 §7.3/§8.3 recommends for native apps;
    // "localhost" is kept for backwards compatibility with existing client_ids.
    private val allowedHosts = Set("localhost", "127.0.0.1", "[::1]")

    // Parses the leading path segment as "[host:]port", e.g. "8080",
    // "127.0.0.1:8080", or "[::1]:8080". Returns the (host, port) to use in the
    // redirect_uri, defaulting the host to "localhost" when omitted, or None if
    // the host is not an allowed loopback host or the port is out of range.
    private def parseHostPort(segment: String): Option[(String, Int)] =
        val (host, portStr) =
            if segment.startsWith("[") then
                // IPv6 literal: host is bracketed, disambiguating it from the port colon.
                segment.indexOf("]:") match
                    case -1 => (segment, "")
                    case i  => (segment.substring(0, i + 1), segment.substring(i + 2))
            else
                segment.lastIndexOf(':') match
                    case -1 => ("localhost", segment)
                    case i  => (segment.substring(0, i), segment.substring(i + 1))
        portStr.toIntOption match
            case Some(p) if p > 0 && p <= 65535 && allowedHosts.contains(host) => Some((host, p))
            case _                                                             => None
    end parseHostPort

    // Matches /[host:]port or /[host:]port/:path, e.g. "8080", "8080/callback",
    // "127.0.0.1:8080/callback", or "[::1]:8080/callback".
    val clientMetadata =
        HttpRoute.getRaw(HttpPath.Capture.Rest("rest")).response(_.bodyJson[ClientMetadata]).handler { req =>
            val rest = req.fields.rest
            val (hostPortSegment, redirectPath) = rest.indexOf('/') match
                case -1 => (rest, "")
                case i  => (rest.substring(0, i), "/" + rest.substring(i + 1))

            parseHostPort(hostPortSegment) match
                case Some((redirectHost, redirectPort)) =>
                    val proto  = req.headers.get("X-Forwarded-Proto").getOrElse("http")
                    val domain = req.headers.get("Host").getOrElse(s"localhost:$port")
                    val path   = if redirectPath.isEmpty then "/" else redirectPath
                    HttpResponse.ok(ClientMetadata(
                        clientName = "cimdnow",
                        clientUri = "https://www.cimd.now",
                        grantTypes = List("authorization_code", "refresh_token"),
                        responseTypes = List("code"),
                        tokenEndpointAuthMethod = "none",
                        applicationType = "native",
                        clientId = s"$proto://$domain${req.path}",
                        redirectUris = List(s"http://$redirectHost:$redirectPort$path")
                    ))
                case None =>
                    HttpResponse.halt(HttpResponse.badRequest)
            end match
        }

    // Redirects the root path to the project's GitHub repository. Registered on the
    // empty path (zero segments), so it matches exactly "/" while requests like
    // "/8080/callback" still fall through to the clientMetadata Rest route.
    val rootRedirect =
        HttpRoute.getRaw(HttpPath.empty).handler { _ =>
            HttpResponse.redirect("https://github.com/jamesward/cimdapp")
        }

    // Exposed so tests can start a server with the exact same routes.
    val routes = Seq(rootRedirect, clientMetadata)

    run {
        for
            server <- HttpServer.init(HttpServerConfig.default.port(port).host("0.0.0.0"))(routes*)
            _      <- Console.printLine(s"Server running at http://localhost:${server.port}")
            _      <- server.await
        yield ()
    }
end Main
