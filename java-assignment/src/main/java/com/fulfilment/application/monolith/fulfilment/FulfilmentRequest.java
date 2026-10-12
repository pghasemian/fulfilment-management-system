package com.fulfilment.application.monolith.fulfilment;

/**
 * Payload of {@code POST /fulfilment}.
 *
 * <p>The warehouse is addressed by its business unit code rather than by its surrogate id: the code
 * is the identifier the business uses, and it survives a warehouse replacement.
 */
public record FulfilmentRequest(Long storeId, Long productId, String warehouseBusinessUnitCode) {}
