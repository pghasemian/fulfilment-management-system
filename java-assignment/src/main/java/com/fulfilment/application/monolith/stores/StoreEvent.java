package com.fulfilment.application.monolith.stores;

public record StoreEvent(Store store, Type type) {

  public enum Type {
    CREATED,
    UPDATED
  }

}