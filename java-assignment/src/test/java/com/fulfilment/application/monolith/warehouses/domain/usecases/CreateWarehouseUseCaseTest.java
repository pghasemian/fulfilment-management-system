package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateWarehouseUseCaseTest {

  private static final Location ZWOLLE = new Location("ZWOLLE-001", 1, 40);
  private static final Location AMSTERDAM = new Location("AMSTERDAM-001", 5, 100);

  @Mock WarehouseStore warehouseStore;
  @Mock LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void shouldCreateWarehouseWhenEveryConstraintIsSatisfied() {
    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM);
    when(warehouseStore.getAll()).thenReturn(List.of());

    var warehouse = new Warehouse("MWH.100", "AMSTERDAM-001", 50, 20);
    useCase.create(warehouse);

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).create(captor.capture());
    assertEquals("MWH.100", captor.getValue().businessUnitCode);
    assertNotNull(captor.getValue().createdAt, "createdAt must be stamped on creation");
    assertNull(captor.getValue().archivedAt, "a new warehouse must be active");
  }

  @Test
  void shouldRejectWhenBusinessUnitCodeAlreadyExists() {
    when(warehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(new Warehouse("MWH.001", "ZWOLLE-001", 40, 10));

    var exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(new Warehouse("MWH.001", "ZWOLLE-001", 30, 10)));

    assertEquals(
        "Warehouse with business unit code MWH.001 already exists and is active",
        exception.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectWhenLocationDoesNotExist() {
    when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("NOWHERE-001")).thenReturn(null);

    var exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(new Warehouse("MWH.100", "NOWHERE-001", 30, 10)));

    assertEquals("Location NOWHERE-001 is not a valid location", exception.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectWhenLocationAlreadyHostsItsMaximumNumberOfWarehouses() {
    when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(ZWOLLE);
    when(warehouseStore.getAll()).thenReturn(List.of(new Warehouse("MWH.001", "ZWOLLE-001", 10, 5)));

    var exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 10, 5)));

    assertEquals(
        "Location ZWOLLE-001 already reached its maximum of 1 warehouses", exception.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectWhenCapacityExceedsTheRemainingCapacityOfTheLocation() {
    when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM);
    when(warehouseStore.getAll())
        .thenReturn(
            List.of(
                new Warehouse("MWH.001", "AMSTERDAM-001", 60, 5),
                // a warehouse of another location must not consume the budget
                new Warehouse("MWH.002", "ZWOLLE-001", 40, 5)));

    var exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(new Warehouse("MWH.100", "AMSTERDAM-001", 41, 5)));

    assertEquals(
        "Capacity 41 exceeds the remaining capacity of 40 at location AMSTERDAM-001",
        exception.getMessage());
  }

  @Test
  void shouldAcceptAWarehouseThatExactlyFillsTheRemainingCapacity() {
    when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM);
    when(warehouseStore.getAll())
        .thenReturn(List.of(new Warehouse("MWH.001", "AMSTERDAM-001", 60, 5)));

    useCase.create(new Warehouse("MWH.100", "AMSTERDAM-001", 40, 5));

    verify(warehouseStore).create(any());
  }

  @Test
  void shouldRejectWhenPayloadIsNull() {
    assertThrows(WarehouseValidationException.class, () -> useCase.create(null));
  }

  @Test
  void shouldRejectWhenBusinessUnitCodeIsMissing() {
    var exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(new Warehouse("  ", "ZWOLLE-001", 10, 5)));

    assertEquals("Business unit code is mandatory", exception.getMessage());
  }

  @Test
  void shouldRejectWhenLocationIsMissing() {
    var exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(new Warehouse("MWH.100", null, 10, 5)));

    assertEquals("Location is mandatory", exception.getMessage());
  }

  @Test
  void shouldRejectWhenCapacityIsNotPositive() {
    assertEquals(
        "Capacity must be a positive number",
        assertThrows(
                WarehouseValidationException.class,
                () -> useCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 0, 0)))
            .getMessage());

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(new Warehouse("MWH.100", "ZWOLLE-001", null, 0)));
  }

  @Test
  void shouldRejectWhenStockIsNegativeOrMissing() {
    assertEquals(
        "Stock must be zero or a positive number",
        assertThrows(
                WarehouseValidationException.class,
                () -> useCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 10, -1)))
            .getMessage());

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 10, null)));
  }

  @Test
  void shouldRejectWhenStockExceedsTheCapacityOfTheWarehouse() {
    var exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(new Warehouse("MWH.100", "ZWOLLE-001", 10, 11)));

    assertEquals("Stock 11 exceeds the capacity 10 of the warehouse", exception.getMessage());
    verify(warehouseStore, never()).create(any());
  }
}
