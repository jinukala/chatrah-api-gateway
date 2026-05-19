package org.chatrah.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.*;

@Path("/{path: .*}")
public class RouteForwarderController {

    @Inject
    ProxyService proxy;

    @Context
    ContainerRequestContext ctx;

    @GET
    public Response forwardGet(String body) {
        return proxy.forward(ctx, body);
    }

    @POST
    public Response forwardPost(String body) {
        return proxy.forward(ctx, body);
    }

    @PUT
    public Response forwardPut(String body) {
        return proxy.forward(ctx, body);
    }

    @DELETE
    public Response forwardDelete(String body) {
        return proxy.forward(ctx, body);
    }

    @PATCH
    public Response forwardPatch(String body) {
        return proxy.forward(ctx, body);
    }
}
