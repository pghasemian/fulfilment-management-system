package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StoreEventObserver {

  private static final Logger LOGGER = Logger.getLogger(StoreEventObserver.class.getName());

  @Inject
  LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreCreated(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent event) {
    LOGGER.info(
        "Transaction committed successfully, propagating store event: " + event.type());
    switch (event.type()) {
      case CREATED -> legacyStoreManagerGateway.createStoreOnLegacySystem(event.store());
      case UPDATED -> legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store());
      default ->
              throw new IllegalArgumentException("Unsupported store event type: " + event.type());
    }
  }
}