package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when the targeted Warehouse does not exist or is no longer active. Mapped to HTTP 404. */
public class WarehouseNotFoundException extends WarehouseException {

  public WarehouseNotFoundException(String message) {
    super(message);
  }
}
