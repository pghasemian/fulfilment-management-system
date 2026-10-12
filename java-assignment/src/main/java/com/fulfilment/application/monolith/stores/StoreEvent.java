package com.fulfilment.application.monolith.stores;

/**
 * Domain events fired when a {@link Store} is persisted.
 *
 * <p>They carry a detached snapshot of the store rather than the managed entity, so that observers
 * running after the transaction has completed never touch a closed persistence context.
 */
public final class StoreEvent {

  private StoreEvent() {}

  public record StoreCreated(Store store) {
    public static StoreCreated of(Store managed) {
      return new StoreCreated(snapshotOf(managed));
    }
  }

  public record StoreUpdated(Store store) {
    public static StoreUpdated of(Store managed) {
      return new StoreUpdated(snapshotOf(managed));
    }
  }

  private static Store snapshotOf(Store managed) {
    var snapshot = new Store(managed.name);
    snapshot.id = managed.id;
    snapshot.quantityProductsInStock = managed.quantityProductsInStock;
    return snapshot;
  }
}
