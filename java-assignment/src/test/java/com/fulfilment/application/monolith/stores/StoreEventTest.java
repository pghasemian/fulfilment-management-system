package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

public class StoreEventTest {

  @Test
  void shouldCreateStoreEventWithCreatedType() {
    // Given
    Store store = new Store("TestStore");

    // When
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    // Then
    assertEquals(store, event.store());
    assertEquals(StoreEvent.Type.CREATED, event.type());
  }

  @Test
  void shouldCreateStoreEventWithUpdatedType() {
    // Given
    Store store = new Store("UpdatedStore");

    // When
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.UPDATED);

    // Then
    assertEquals(store, event.store());
    assertEquals(StoreEvent.Type.UPDATED, event.type());
  }

  @Test
  void shouldSupportEqualityForSameData() {
    // Given
    Store store = new Store("Store1");

    // When
    StoreEvent event1 = new StoreEvent(store, StoreEvent.Type.CREATED);
    StoreEvent event2 = new StoreEvent(store, StoreEvent.Type.CREATED);

    // Then
    assertEquals(event1, event2);
    assertEquals(event1.hashCode(), event2.hashCode());
  }

  @Test
  void shouldNotBeEqualForDifferentTypes() {
    // Given
    Store store = new Store("Store1");

    // When
    StoreEvent createdEvent = new StoreEvent(store, StoreEvent.Type.CREATED);
    StoreEvent updatedEvent = new StoreEvent(store, StoreEvent.Type.UPDATED);

    // Then
    assertNotEquals(createdEvent, updatedEvent);
  }

  @Test
  void shouldNotBeEqualForDifferentStores() {
    // Given
    Store store1 = new Store("Store1");
    Store store2 = new Store("Store2");

    // When
    StoreEvent event1 = new StoreEvent(store1, StoreEvent.Type.CREATED);
    StoreEvent event2 = new StoreEvent(store2, StoreEvent.Type.CREATED);

    // Then
    assertNotEquals(event1, event2);
  }

  @Test
  void shouldHandleNullStore() {
    // Given
    Store nullStore = null;

    // When
    StoreEvent event = new StoreEvent(nullStore, StoreEvent.Type.CREATED);

    // Then
    assertNull(event.store());
    assertEquals(StoreEvent.Type.CREATED, event.type());
  }

  @Test
  void shouldHaveAllEnumValues() {
    // Given — no setup needed

    // When
    StoreEvent.Type[] values = StoreEvent.Type.values();

    // Then
    assertEquals(2, values.length);
    assertEquals(StoreEvent.Type.CREATED, StoreEvent.Type.valueOf("CREATED"));
    assertEquals(StoreEvent.Type.UPDATED, StoreEvent.Type.valueOf("UPDATED"));
  }

  @Test
  void shouldProduceReadableToString() {
    // Given
    Store store = new Store("TestStore");
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    // When
    String toString = event.toString();

    // Then
    assertNotNull(toString);
    assertTrue(toString.contains("CREATED"));
  }
}