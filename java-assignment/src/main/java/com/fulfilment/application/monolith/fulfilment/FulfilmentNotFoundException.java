package com.fulfilment.application.monolith.fulfilment;

/** Raised when a requested store, product or warehouse does not exist. Maps to HTTP 404. */
public class FulfilmentNotFoundException extends RuntimeException {

  public FulfilmentNotFoundException(String message) {
    super(message);
  }
}
