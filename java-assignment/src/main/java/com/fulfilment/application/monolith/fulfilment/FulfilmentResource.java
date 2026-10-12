package com.fulfilment.application.monolith.fulfilment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

/** REST adapter for the fulfilment associations (bonus task). */
@Path("fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  @Inject FulfilmentService fulfilmentService;

  @GET
  public List<FulfilmentView> list() {
    return fulfilmentService.listAll();
  }

  @GET
  @Path("store/{storeId}")
  public List<FulfilmentView> listByStore(@PathParam("storeId") Long storeId) {
    return fulfilmentService.listByStore(storeId);
  }

  @POST
  public Response associate(FulfilmentRequest request) {
    return Response.status(Response.Status.CREATED)
        .entity(fulfilmentService.associate(request))
        .build();
  }

  @DELETE
  @Path("{id}")
  public Response dissociate(@PathParam("id") Long id) {
    fulfilmentService.dissociate(id);
    return Response.noContent().build();
  }
}
