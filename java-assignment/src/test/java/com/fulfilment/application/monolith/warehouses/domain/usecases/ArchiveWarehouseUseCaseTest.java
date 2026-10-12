package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;

  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  void shouldArchiveTheActiveWarehouseOfTheBusinessUnitCode() {
    var persisted = new Warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(persisted);

    useCase.archive(new Warehouse("MWH.001", "ZWOLLE-001", 40, 10));

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(captor.capture());
    assertNotNull(captor.getValue().archivedAt, "archiving must stamp archivedAt");
    assertEquals("MWH.001", captor.getValue().businessUnitCode);
  }

  @Test
  void shouldArchiveThePersistedStateRatherThanTheProvidedInstance() {
    var persisted = new Warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    persisted.id = 7L;
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(persisted);

    // the caller only knows the business unit code, and sends stale values for everything else
    var stale = new Warehouse("MWH.001", "TILBURG-001", 1, 1);
    useCase.archive(stale);

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(captor.capture());
    assertEquals(7L, captor.getValue().id);
    assertEquals("ZWOLLE-001", captor.getValue().location);
  }

  @Test
  void shouldRejectWhenWarehouseDoesNotExist() {
    when(warehouseStore.findByBusinessUnitCode("MWH.404")).thenReturn(null);

    var exception =
        assertThrows(
            WarehouseNotFoundException.class,
            () -> useCase.archive(new Warehouse("MWH.404", "ZWOLLE-001", 10, 1)));

    assertEquals(
        "Warehouse with business unit code MWH.404 does not exist or is already archived",
        exception.getMessage());
    verify(warehouseStore, never()).update(any());
  }

  @Test
  void shouldRejectWhenWarehouseIsAlreadyArchived() {
    var alreadyArchived = new Warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    alreadyArchived.archivedAt = LocalDateTime.now();

    var exception =
        assertThrows(WarehouseValidationException.class, () -> useCase.archive(alreadyArchived));

    assertEquals("Warehouse MWH.001 is already archived", exception.getMessage());
    verify(warehouseStore, never()).update(any());
  }

  @Test
  void shouldRejectWhenBusinessUnitCodeIsMissing() {
    assertThrows(WarehouseValidationException.class, () -> useCase.archive(null));
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.archive(new Warehouse(null, "ZWOLLE-001", 10, 1)));

    verify(warehouseStore, never()).update(any());
  }
}
