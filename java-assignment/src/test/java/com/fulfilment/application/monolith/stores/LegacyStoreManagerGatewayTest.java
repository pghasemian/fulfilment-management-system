package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LegacyStoreManagerGatewayTest {

  private LegacyStoreManagerGateway gateway;

  @BeforeEach
  void setUp() {
    // Given — fresh gateway instance for each test
    gateway = new LegacyStoreManagerGateway();
  }

  @Test
  void shouldCreateStoreOnLegacySystemWithoutException() {
    // Given
    Store store = new Store("TestStore");
    store.quantityProductsInStock = 42;

    // When & Then
    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  void shouldUpdateStoreOnLegacySystemWithoutException() {
    // Given
    Store store = new Store("UpdatedStore");
    store.quantityProductsInStock = 99;

    // When & Then
    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }

  @Test
  void shouldHandleStoreWithZeroStock() {
    // Given
    Store store = new Store("EmptyStore");
    store.quantityProductsInStock = 0;

    // When & Then
    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  void shouldHandleStoreWithLargeStock() {
    // Given
    Store store = new Store("LargeStore");
    store.quantityProductsInStock = Integer.MAX_VALUE;

    // When & Then
    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  void shouldHandleStoreWithSpecialCharactersInName() {
    // Given
    Store store = new Store("Store-With_Special.Chars");
    store.quantityProductsInStock = 10;

    // When & Then
    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  void shouldHandleMultipleCallsSequentially() {
    // Given
    Store store1 = new Store("Store1");
    store1.quantityProductsInStock = 10;
    Store store2 = new Store("Store2");
    store2.quantityProductsInStock = 20;

    // When & Then
    assertDoesNotThrow(() -> {
      gateway.createStoreOnLegacySystem(store1);
      gateway.updateStoreOnLegacySystem(store2);
      gateway.createStoreOnLegacySystem(store1);
    });
  }

  @Test
  void shouldHandleUpdateAfterCreate() {
    // Given
    Store store = new Store("LifecycleStore");
    store.quantityProductsInStock = 10;

    // When — create then update
    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
    store.quantityProductsInStock = 20;

    // Then
    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }

  @Test
  void shouldHandleNegativeStock() {
    // Given
    Store store = new Store("NegativeStore");
    store.quantityProductsInStock = -100;

    // When & Then
    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }
}