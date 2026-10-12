package com.fulfilment.application.monolith.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.fulfilment.FulfilmentNotFoundException;
import com.fulfilment.application.monolith.fulfilment.FulfilmentValidationException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Single application wide error mapper.
 *
 * <p>It replaces the two identical {@code ErrorMapper} providers that used to be nested in the
 * Product and Store resources: both were registered for {@code Exception}, so which one handled a
 * given failure was undefined. Domain exceptions are translated here, which keeps the domain layer
 * free of any HTTP concern.
 */
@Provider
public class RestExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(RestExceptionMapper.class);

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {
    int code = statusCodeOf(exception);

    if (code >= 500) {
      LOGGER.error("Failed to handle request", exception);
    } else {
      // expected, caller driven failures: no stack trace needed
      LOGGER.warnf("Request rejected with status %d: %s", code, exception.getMessage());
    }

    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("exceptionType", exception.getClass().getName());
    exceptionJson.put("code", code);

    if (exception.getMessage() != null) {
      exceptionJson.put("error", exception.getMessage());
    }

    return Response.status(code).entity(exceptionJson).build();
  }

  private int statusCodeOf(Exception exception) {
    if (exception instanceof WarehouseNotFoundException) {
      return Response.Status.NOT_FOUND.getStatusCode();
    }
    if (exception instanceof FulfilmentNotFoundException) {
      return Response.Status.NOT_FOUND.getStatusCode();
    }
    if (exception instanceof WarehouseValidationException) {
      return Response.Status.BAD_REQUEST.getStatusCode();
    }
    if (exception instanceof FulfilmentValidationException) {
      return Response.Status.BAD_REQUEST.getStatusCode();
    }
    if (exception instanceof WebApplicationException webApplicationException) {
      return webApplicationException.getResponse().getStatus();
    }
    return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
  }
}
