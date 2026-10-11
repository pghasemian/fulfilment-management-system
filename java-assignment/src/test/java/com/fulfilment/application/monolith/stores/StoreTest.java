package com.fulfilment.application.monolith.stores;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StoreTest {

  @Test
  void shouldCreateStoreWithDefaultConstructor() {
    // Given — no arguments

    // When
    Store store = new Store();

    // Then
    assertNull(store.name);
    assertNull(store.quantityProductsInStock);
    assertNull(store.id);
  }

  @Test
  void shouldCreateStoreWithNameConstructor() {
    // Given
    String name = "MyStore";

    // When
    Store store = new Store(name);

    // Then
    assertEquals("MyStore", store.name);
    assertNull(store.quantityProductsInStock);
  }

  @Test
  void shouldSetAndGetFields() {
    // Given
    Store store = new Store();

    // When
    store.name = "FieldStore";
    store.quantityProductsInStock = 150;

    // Then
    assertEquals("FieldStore", store.name);
    assertEquals(150, store.quantityProductsInStock);
  }

  @Test
  void shouldAllowNullName() {
    // Given
    String nullName = null;

    // When
    Store store = new Store(nullName);

    // Then
    assertNull(store.name);
  }

  @Test
  void shouldAllowEmptyName() {
    // Given
    String emptyName = "";

    // When
    Store store = new Store(emptyName);

    // Then
    assertEquals("", store.name);
  }

  @Test
  void shouldAllowNegativeStock() {
    // Given
    Store store = new Store("NegativeStore");

    // When
    store.quantityProductsInStock = -5;

    // Then
    assertEquals(-5, store.quantityProductsInStock);
  }

  @Test
  void shouldAllowZeroStock() {
    // Given
    Store store = new Store("ZeroStore");

    // When
    store.quantityProductsInStock = 0;

    // Then
    assertEquals(0, store.quantityProductsInStock);
  }

  @Test
  void shouldHandleMaxLengthName() {
    // Given
    String maxName = "A".repeat(40);

    // When
    Store store = new Store(maxName);

    // Then
    assertEquals(40, store.name.length());
    assertEquals(maxName, store.name);
  }

  @Test
  void shouldHandleMaxIntStock() {
    // Given
    Store store = new Store("MaxStore");

    // When
    store.quantityProductsInStock = Integer.MAX_VALUE;

    // Then
    assertEquals(Integer.MAX_VALUE, store.quantityProductsInStock);
  }

  @Test
  void shouldHandleMinIntStock() {
    // Given
    Store store = new Store("MinStore");

    // When
    store.quantityProductsInStock = Integer.MIN_VALUE;

    // Then
    assertEquals(Integer.MIN_VALUE, store.quantityProductsInStock);
  }

  @Test
  void shouldUpdateNameAfterCreation() {
    // Given
    Store store = new Store("OriginalName");

    // When
    store.name = "UpdatedName";

    // Then
    assertEquals("UpdatedName", store.name);
  }

  @Test
  void shouldUpdateStockAfterCreation() {
    // Given
    Store store = new Store("StockStore");
    store.quantityProductsInStock = 10;

    // When
    store.quantityProductsInStock = 99;

    // Then
    assertEquals(99, store.quantityProductsInStock);
  }
}