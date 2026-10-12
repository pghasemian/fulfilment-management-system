package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplaceWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;
  @Mock ArchiveWarehouseOperation archiveWarehouseOperation;
  @Mock CreateWarehouseOperation createWarehouseOperation;

  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new ReplaceWarehouseUseCase(
            warehouseStore, archiveWarehouseOperation, createWarehouseOperation);
  }

  @Test
  void shouldArchiveThePreviousWarehouseBeforeCreatingTheNewOne() {
    var previous = new Warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);

    var replacement = new Warehouse("MWH.001", "ZWOLLE-001", 35, 10);
    useCase.replace(replacement);

    // the order matters: archiving first is what frees the business unit code and the location slot
    InOrder inOrder = inOrder(archiveWarehouseOperation, createWarehouseOperation);
    inOrder.verify(archiveWarehouseOperation).archive(previous);
    inOrder.verify(createWarehouseOperation).create(replacement);
  }

  @Test
  void shouldKeepTheBusinessUnitCodeOfTheReplacedWarehouse() {
    var previous = new Warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);

    useCase.replace(new Warehouse("MWH.001", "ZWOLLE-002", 35, 10));

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(createWarehouseOperation).create(captor.capture());
    assertEquals("MWH.001", captor.getValue().businessUnitCode);
    assertEquals("ZWOLLE-002", captor.getValue().location);
  }

  @Test
  void shouldRejectWhenThereIsNoActiveWarehouseForTheBusinessUnitCode() {
    when(warehouseStore.findByBusinessUnitCode("MWH.404")).thenReturn(null);

    var exception =
        assertThrows(
            WarehouseNotFoundException.class,
            () -> useCase.replace(new Warehouse("MWH.404", "ZWOLLE-001", 40, 10)));

    assertEquals(
        "No active warehouse found with business unit code MWH.404", exception.getMessage());
    verify(archiveWarehouseOperation, never()).archive(any());
    verify(createWarehouseOperation, never()).create(any());
  }

  @Test
  void shouldRejectWhenTheNewCapacityCannotAccommodateThePreviousStock() {
    when(warehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(new Warehouse("MWH.001", "ZWOLLE-001", 40, 30));

    // internally consistent payload (stock <= capacity) that is still too small for the 30 units
    // currently held by the warehouse being replaced
    var exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.replace(new Warehouse("MWH.001", "ZWOLLE-001", 20, 20)));

    assertEquals(
        "Capacity 20 of the new warehouse cannot accommodate the stock 30 of the warehouse being replaced",
        exception.getMessage());
    verify(archiveWarehouseOperation, never()).archive(any());
  }

  @Test
  void shouldRejectWhenTheStockDoesNotMatchThePreviousWarehouse() {
    when(warehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(new Warehouse("MWH.001", "ZWOLLE-001", 40, 30));

    var exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.replace(new Warehouse("MWH.001", "ZWOLLE-001", 40, 25)));

    assertEquals(
        "Stock 25 of the new warehouse must match the stock 30 of the warehouse being replaced",
        exception.getMessage());
    verify(archiveWarehouseOperation, never()).archive(any());
    verify(createWarehouseOperation, never()).create(any());
  }

  @Test
  void shouldAcceptAReplacementWhoseCapacityExactlyMatchesThePreviousStock() {
    when(warehouseStore.findByBusinessUnitCode("MWH.001"))
        .thenReturn(new Warehouse("MWH.001", "ZWOLLE-001", 40, 30));

    useCase.replace(new Warehouse("MWH.001", "ZWOLLE-001", 30, 30));

    verify(createWarehouseOperation).create(any());
  }

  @Test
  void shouldApplyTheRegularCreationValidationsToTheReplacement() {
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(new Warehouse("MWH.001", "ZWOLLE-001", 10, 20)));

    verify(warehouseStore, never()).findByBusinessUnitCode(any());
  }

  @Test
  void shouldNotArchiveWhenTheCreationOfTheReplacementFails() {
    var previous = new Warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    doThrow(new WarehouseValidationException("location is full"))
        .when(createWarehouseOperation)
        .create(any());

    // the failure must bubble up so that the surrounding transaction rolls the archiving back
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(new Warehouse("MWH.001", "ZWOLLE-001", 35, 10)));
  }
}
