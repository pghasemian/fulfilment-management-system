package com.fulfilment.application.monolith.fulfilment;

import java.time.LocalDateTime;

/**
 * Read model of an association. A dedicated view avoids serialising the JPA entity, whose lazy
 * associations would otherwise be touched outside of a transaction.
 */
public record FulfilmentView(
    Long id,
    Long storeId,
    String storeName,
    Long productId,
    String productName,
    String warehouseBusinessUnitCode,
    LocalDateTime createdAt) {

  public static FulfilmentView of(FulfilmentAssociation association) {
    return new FulfilmentView(
        association.id,
        association.store.id,
        association.store.name,
        association.product.id,
        association.product.name,
        association.warehouse.businessUnitCode,
        association.createdAt);
  }
}
