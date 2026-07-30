import kyo.*
import kyo.schema.*

case class ClientMetadata(
    @rename("client_name") clientName: String,
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
    private val clientMetadata =
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
                        clientName = "cimdtest",
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

    run {
        for
            server <- HttpServer.init(HttpServerConfig.default.port(port).host("0.0.0.0"))(clientMetadata)
            _      <- Console.printLine(s"Server running at http://localhost:${server.port}")
            _      <- server.await
        yield ()
    }
end Main
