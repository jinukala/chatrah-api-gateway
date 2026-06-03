package org.chatrah.api;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    @Inject
    JsonWebToken jwt;

    @Inject
    RolePermissions permissions;

    @Override
    public void filter(ContainerRequestContext ctx) {

        String path = ctx.getUriInfo().getPath();
        if (path.startsWith("/")) path = path.substring(1);

        // Public routes — no JWT required
        if (isPublicPath(path)) return;

        if (jwt.getSubject() == null) {
            ctx.abortWith(Response.status(401)
                    .entity("{\"error\":\"Authentication required\"}")
                    .header("Content-Type", "application/json")
                    .build());
            return;
        }

        String role = jwt.getClaim("role");

        if (!permissions.allowed(role, "/" + path)) {
            ctx.abortWith(Response.status(403)
                    .entity("{\"error\":\"Access denied\"}")
                    .header("Content-Type", "application/json")
                    .build());
        }

        // Impersonation support
        if (jwt.containsClaim("impersonated")) {
            ctx.setProperty("actingUser", jwt.getClaim("actingAs"));
            ctx.setProperty("realAdmin", jwt.getClaim("realAdminId"));
        } else {
            ctx.setProperty("actingUser", jwt.getSubject());
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("gateway/auth")
                || path.startsWith("api/auth/login")
                || path.startsWith("api/auth/otp/")
                || path.startsWith("api/auth/password/reset")
                || path.startsWith("api/school/profile")
                || path.startsWith("api/events/upcoming")
                || path.startsWith("api/blogs/approved")
                || path.startsWith("api/students/birthdays/today")
                || path.startsWith("api/payments/webhook");
    }
}
