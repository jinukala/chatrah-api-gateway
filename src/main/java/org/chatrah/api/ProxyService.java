package org.chatrah.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ProxyService {

    private static final Logger LOG = Logger.getLogger(ProxyService.class.getName());

    @ConfigProperty(name = "chatrah.backend.base-url")
    String backendUrl;

    @ConfigProperty(name = "chatrah.gateway.secret")
    String gatewaySecret;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public Response forward(ContainerRequestContext ctx, String body) {
        try {
            String path = sanitizePath(ctx.getUriInfo().getPath());
            String query = ctx.getUriInfo().getRequestUri().getQuery();
            String contentType = ctx.getHeaderString("Content-Type");

            String targetUri = backendUrl + "/" + path;
            if (query != null && !query.isBlank()) {
                targetUri += "?" + query;
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .timeout(Duration.ofSeconds(30))
                    .method(ctx.getMethod(),
                            body == null || body.isBlank()
                                    ? HttpRequest.BodyPublishers.noBody()
                                    : HttpRequest.BodyPublishers.ofString(body))
                    .header("X-Gateway-Secret", gatewaySecret);

            String auth = ctx.getHeaderString("Authorization");
            if (auth != null) {
                builder.header("Authorization", auth);
            }

            if (contentType != null && !contentType.isBlank()) {
                builder.header("Content-Type", contentType);
            } else if (body != null && !body.isBlank()) {
                builder.header("Content-Type", "application/json");
            }

            HttpResponse<String> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            Response.ResponseBuilder rb = Response.status(resp.statusCode()).entity(resp.body());
            resp.headers().firstValue("Content-Type").ifPresent(ct -> rb.header("Content-Type", ct));
            return rb.build();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Proxy error", e);
            return Response.status(502)
                    .entity("{\"error\":\"Service temporarily unavailable\"}")
                    .header("Content-Type", "application/json")
                    .build();
        }
    }

    private String sanitizePath(String path) {
        if (path == null || path.isBlank()) return "";

        // URL-decode first to catch encoded traversal attempts
        String decoded;
        try {
            decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            decoded = path;
        }

        // Normalize using java.nio.file.Path to resolve any ../ sequences
        String normalized = Path.of("/" + decoded).normalize().toString()
                .replace("\\", "/");

        // Must still start with / after normalization (no escape above root)
        if (!normalized.startsWith("/")) {
            return "";
        }

        // Remove leading slash for URI construction
        String result = normalized.substring(1);

        // Final safety: reject if it still contains ..
        if (result.contains("..")) {
            return "";
        }

        return result;
    }
}
