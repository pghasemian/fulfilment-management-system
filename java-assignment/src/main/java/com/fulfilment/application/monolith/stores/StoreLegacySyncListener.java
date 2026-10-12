package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Propagates {@link Store} changes to the legacy store management system.
 *
 * <p>Both observers are bound to {@link TransactionPhase#AFTER_SUCCESS}, which means the CDI
 * container only delivers the event once the surrounding JTA transaction has been committed. This
 * guarantees the downstream legacy system is never told about data that was subsequently rolled
 * back.
 *
 * <p>A failure while calling the legacy system is logged and swallowed on purpose: the local
 * transaction is already committed and must not be reported as failed to the caller. Making this
 * reliable would require an outbox table with retries (see IMPLEMENTATION.md).
 */
@ApplicationScoped
public class StoreLegacySyncListener {

  private static final Logger LOGGER = Logger.getLogger(StoreLegacySyncListener.class);

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent.StoreCreated event) {
    try {
      LOGGER.infof("Propagating creation of store '%s' to the legacy system", event.store().name);
      legacyStoreManagerGateway.createStoreOnLegacySystem(event.store());
    } catch (RuntimeException e) {
      LOGGER.errorf(e, "Failed to propagate creation of store '%s' to the legacy system", event.store().name);
    }
  }

  public void onStoreUpdated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent.StoreUpdated event) {
    try {
      LOGGER.infof("Propagating update of store '%s' to the legacy system", event.store().name);
      legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store());
    } catch (RuntimeException e) {
      LOGGER.errorf(e, "Failed to propagate update of store '%s' to the legacy system", event.store().name);
    }
  }
}
