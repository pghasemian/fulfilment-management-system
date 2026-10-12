package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Makes {@code POST /warehouse} answer with the 201 declared in {@code warehouse-openapi.yaml}.
 *
 * <p>The generated {@code WarehouseResource} interface is the JAX-RS resource and returns the
 * Warehouse bean rather than a {@code Response}, which pins the success status to 200. Since the
 * interface is generated it cannot be annotated, so the status is corrected here. The replacement
 * endpoint is intentionally left untouched: the specification declares 200 for it.
 */
@Provider
public class WarehouseCreatedStatusFilter implements ContainerResponseFilter {

  private static final String WAREHOUSE_COLLECTION_PATH = "warehouse";

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if ("POST".equals(requestContext.getMethod())
        && WAREHOUSE_COLLECTION_PATH.equals(trimSlashes(requestContext.getUriInfo().getPath()))
        && responseContext.getStatus() == Response.Status.OK.getStatusCode()) {
      responseContext.setStatus(Response.Status.CREATED.getStatusCode());
    }
  }

  private String trimSlashes(String path) {
    return path == null ? "" : path.replaceAll("^/+|/+$", "");
  }
}
