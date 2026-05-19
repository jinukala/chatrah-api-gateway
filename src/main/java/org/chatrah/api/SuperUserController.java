package org.chatrah.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Super-user operations (SYS_ADMIN / PRINCIPAL only).
 * These are gateway-level operations that do NOT proxy to backend.
 */
@Path("/gateway/super")
@RolesAllowed({"SYS_ADMIN", "PRINCIPAL"})
@Produces(MediaType.APPLICATION_JSON)
public class SuperUserController {

    @POST
    @Path("/impersonate/{userId}")
    public Response impersonate(@PathParam("userId") Long userId) {
        // Impersonation is disabled — requires dedicated TokenService implementation
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity("{\"error\":\"Impersonation is not yet implemented\",\"userId\":" + userId + "}")
                .build();
    }

    @POST
    @Path("/user/{id}/disable")
    public Response disable(@PathParam("id") Long id) {
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity("{\"error\":\"User disable not yet implemented\"}")
                .build();
    }

    @POST
    @Path("/user/{id}/enable")
    public Response enable(@PathParam("id") Long id) {
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity("{\"error\":\"User enable not yet implemented\"}")
                .build();
    }

    @POST
    @Path("/user/{id}/reset-password")
    public Response resetPassword(@PathParam("id") Long id) {
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity("{\"error\":\"Password reset not yet implemented\"}")
                .build();
    }
}
