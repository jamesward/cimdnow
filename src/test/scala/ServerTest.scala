import kyo.*
import kyo.test.*

// Exercises the real server: starts it on an ephemeral port with the app's
// actual routes and drives it over HTTP with kyo-http's client, asserting on
// good requests (valid port -> 200 + correct metadata) and bad requests
// (invalid port segment -> 400).
class ServerTest extends Test[Any]:

    private val serverConfig = HttpServerConfig.default.port(0).host("localhost")

    "server" - {

        "GET /:port/:path returns CIMD metadata whose client_id is the request URL" in {
            for
                server <- HttpServer.init(serverConfig)(Main.routes*)
                client <- HttpClient.init()
                md <- HttpClient.let(client)(
                    HttpClient.getJson[ClientMetadata](
                        s"http://localhost:${server.port}/8080/callback",
                        headers = Seq("Host" -> "www.cimd.now")
                    )
                )
            yield
                assert(md.clientName == "cimdnow")
                assert(md.applicationType == "native")
                assert(md.clientId == "http://www.cimd.now/8080/callback")
                assert(md.redirectUris == List("http://localhost:8080/callback"))
        }

        "GET /:port (no path) defaults the redirect_uri path to /" in {
            for
                server <- HttpServer.init(serverConfig)(Main.routes*)
                client <- HttpClient.init()
                md <- HttpClient.let(client)(
                    HttpClient.getJson[ClientMetadata](
                        s"http://localhost:${server.port}/9000",
                        headers = Seq("Host" -> "www.cimd.now")
                    )
                )
            yield
                assert(md.clientId == "http://www.cimd.now/9000")
                assert(md.redirectUris == List("http://localhost:9000/"))
        }

        "GET /127.0.0.1:port/:path uses the IPv4 loopback literal in redirect_uri" in {
            for
                server <- HttpServer.init(serverConfig)(Main.routes*)
                client <- HttpClient.init()
                md <- HttpClient.let(client)(
                    HttpClient.getJson[ClientMetadata](
                        s"http://localhost:${server.port}/127.0.0.1:8080/callback",
                        headers = Seq("Host" -> "www.cimd.now")
                    )
                )
            yield
                assert(md.clientId == "http://www.cimd.now/127.0.0.1:8080/callback")
                assert(md.redirectUris == List("http://127.0.0.1:8080/callback"))
        }

        "GET /[::1]:port/:path uses the IPv6 loopback literal in redirect_uri" in {
            for
                server <- HttpServer.init(serverConfig)(Main.routes*)
                client <- HttpClient.init()
                md <- HttpClient.let(client)(
                    HttpClient.getJson[ClientMetadata](
                        s"http://localhost:${server.port}/[::1]:8080/callback",
                        headers = Seq("Host" -> "www.cimd.now")
                    )
                )
            yield
                assert(md.clientId == "http://www.cimd.now/[::1]:8080/callback")
                assert(md.redirectUris == List("http://[::1]:8080/callback"))
        }

        "GET /localhost:port/:path is accepted explicitly" in {
            for
                server <- HttpServer.init(serverConfig)(Main.routes*)
                client <- HttpClient.init()
                md <- HttpClient.let(client)(
                    HttpClient.getJson[ClientMetadata](
                        s"http://localhost:${server.port}/localhost:8080/callback",
                        headers = Seq("Host" -> "www.cimd.now")
                    )
                )
            yield
                assert(md.redirectUris == List("http://localhost:8080/callback"))
        }

        "GET /:host:port with a non-loopback host returns 400" in {
            for
                server <- HttpServer.init(serverConfig)(Main.routes*)
                client <- HttpClient.init()
                resp <- HttpClient.let(client)(
                    HttpClient.getTextResponse(
                        s"http://localhost:${server.port}/evil.example.com:8080/callback",
                        failOnError = false
                    )
                )
            yield
                assert(resp.status.code == 400)
        }

        "GET /:port with a non-numeric port segment returns 400" in {
            for
                server <- HttpServer.init(serverConfig)(Main.routes*)
                client <- HttpClient.init()
                resp <- HttpClient.let(client)(
                    HttpClient.getTextResponse(
                        s"http://localhost:${server.port}/notaport",
                        failOnError = false
                    )
                )
            yield
                assert(resp.status.code == 400)
                assert(resp.status.isClientError)
        }

        "GET /:port with an out-of-range port returns 400" in {
            for
                server <- HttpServer.init(serverConfig)(Main.routes*)
                client <- HttpClient.init()
                resp <- HttpClient.let(client)(
                    HttpClient.getTextResponse(
                        s"http://localhost:${server.port}/99999",
                        failOnError = false
                    )
                )
            yield
                assert(resp.status.code == 400)
        }
    }
end ServerTest
