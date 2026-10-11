package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StoreEventObserverTest {

  @Mock
  private LegacyStoreManagerGateway legacyStoreManagerGateway;

  private StoreEventObserver storeEventObserver;

  @BeforeEach
  void setUp() throws Exception {
    // Given — inject the mock via reflection since CDI @Inject is not available in unit tests
    storeEventObserver = new StoreEventObserver();
    Field field = StoreEventObserver.class.getDeclaredField("legacyStoreManagerGateway");
    field.setAccessible(true);
    field.set(storeEventObserver, legacyStoreManagerGateway);
  }

  @Test
  void shouldCallCreateOnLegacySystemWhenCreatedEvent() {
    // Given
    Store store = new Store("NewStore");
    store.quantityProductsInStock = 50;
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    // When
    storeEventObserver.onStoreCreated(event);

    // Then
    verify(legacyStoreManagerGateway, times(1)).createStoreOnLegacySystem(store);
    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());
  }

  @Test
  void shouldCallUpdateOnLegacySystemWhenUpdatedEvent() {
    // Given
    Store store = new Store("ExistingStore");
    store.quantityProductsInStock = 100;
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.UPDATED);

    // When
    storeEventObserver.onStoreCreated(event);

    // Then
    verify(legacyStoreManagerGateway, times(1)).updateStoreOnLegacySystem(store);
    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any());
  }

  @Test
  void shouldPassCorrectStoreToLegacyGatewayOnCreate() {
    // Given
    Store store = new Store("SpecificStore");
    store.quantityProductsInStock = 25;
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    // When
    storeEventObserver.onStoreCreated(event);

    // Then
    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(argThat(s ->
        "SpecificStore".equals(s.name) && s.quantityProductsInStock == 25
    ));
  }

  @Test
  void shouldPassCorrectStoreToLegacyGatewayOnUpdate() {
    // Given
    Store store = new Store("UpdatedStore");
    store.quantityProductsInStock = 75;
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.UPDATED);

    // When
    storeEventObserver.onStoreCreated(event);

    // Then
    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(argThat(s ->
        "UpdatedStore".equals(s.name) && s.quantityProductsInStock == 75
    ));
  }

  @Test
  void shouldHandleStoreWithZeroStock() {
    // Given
    Store store = new Store("EmptyStore");
    store.quantityProductsInStock = 0;
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    // When
    storeEventObserver.onStoreCreated(event);

    // Then
    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(store);
  }

  @Test
  void shouldPropagateExceptionFromLegacyGateway() {
    // Given
    Store store = new Store("FailStore");
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);
    doThrow(new RuntimeException("Legacy system unavailable"))
        .when(legacyStoreManagerGateway).createStoreOnLegacySystem(any());

    // When & Then
    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> storeEventObserver.onStoreCreated(event)
    );
    assertEquals("Legacy system unavailable", ex.getMessage());
  }
}