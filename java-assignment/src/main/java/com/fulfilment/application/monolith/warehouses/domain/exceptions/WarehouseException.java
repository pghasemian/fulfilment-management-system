package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Base type for every business error raised by the Warehouse domain.
 *
 * <p>These exceptions are deliberately free of any JAX-RS or persistence types: translating them
 * into an HTTP response is the responsibility of the REST adapter.
 */
public abstract class WarehouseException extends RuntimeException {

  protected WarehouseException(String message) {
    super(message);
  }
}
