package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

/**
 * Archives a Warehouse unit.
 *
 * <p>Archiving is a soft delete on purpose: the unit keeps its cost and operational history, while
 * freeing its business unit code and its slot at the location for a replacement.
 */
@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ArchiveWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    if (warehouse == null || warehouse.businessUnitCode == null) {
      throw new WarehouseValidationException("Business unit code is mandatory to archive a warehouse");
    }
    if (warehouse.isArchived()) {
      throw new WarehouseValidationException(
          "Warehouse " + warehouse.businessUnitCode + " is already archived");
    }

    // always work on the persisted state rather than on the caller provided instance
    Warehouse active = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (active == null) {
      throw new WarehouseNotFoundException(
          "Warehouse with business unit code "
              + warehouse.businessUnitCode
              + " does not exist or is already archived");
    }

    active.archivedAt = LocalDateTime.now();
    warehouseStore.update(active);
    LOGGER.infof("Warehouse %s archived at %s", active.businessUnitCode, active.archivedAt);
  }
}
