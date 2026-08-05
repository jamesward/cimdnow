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

    // Matches /:port or /:port/:path, e.g. "8080" or "8080/callback".
    val clientMetadata =
        HttpRoute.getRaw(HttpPath.Capture.Rest("rest")).response(_.bodyJson[ClientMetadata]).handler { req =>
            val rest = req.fields.rest
            val (portSegment, redirectPath) = rest.indexOf('/') match
                case -1 => (rest, "")
                case i  => (rest.substring(0, i), "/" + rest.substring(i + 1))

            portSegment.toIntOption match
                case Some(redirectPort) if redirectPort > 0 && redirectPort <= 65535 =>
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
                        redirectUris = List(s"http://localhost:$redirectPort$path")
                    ))
                case _ =>
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
