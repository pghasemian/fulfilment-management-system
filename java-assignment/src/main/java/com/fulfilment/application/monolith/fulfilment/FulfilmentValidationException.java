package com.fulfilment.application.monolith.fulfilment;

/** Raised when a request breaks one of the fulfilment constraints. Maps to HTTP 400. */
public class FulfilmentValidationException extends RuntimeException {

  public FulfilmentValidationException(String message) {
    super(message);
  }
}
