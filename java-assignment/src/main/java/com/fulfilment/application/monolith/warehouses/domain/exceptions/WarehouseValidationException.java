package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Raised when a Warehouse operation violates a business rule or carries invalid data. Mapped to an
 * HTTP 400, as described by the Warehouse OpenAPI specification.
 */
public class WarehouseValidationException extends WarehouseException {

  public WarehouseValidationException(String message) {
    super(message);
  }
}
