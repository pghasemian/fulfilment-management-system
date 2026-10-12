package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Replaces the active Warehouse of a business unit code by a new one.
 *
 * <p>The previous unit is archived first, which frees its business unit code and its slot at the
 * location, and the new unit is then created through the regular creation rules. Both steps run in
 * the same transaction, so a rejected creation leaves the previous warehouse untouched.
 */
@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;
  private final CreateWarehouseOperation createWarehouseOperation;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      ArchiveWarehouseOperation archiveWarehouseOperation,
      CreateWarehouseOperation createWarehouseOperation) {
    this.warehouseStore = warehouseStore;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
    this.createWarehouseOperation = createWarehouseOperation;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    CreateWarehouseUseCase.validateRequiredFields(newWarehouse);

    Warehouse toBeReplaced = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (toBeReplaced == null) {
      throw new WarehouseNotFoundException(
          "No active warehouse found with business unit code " + newWarehouse.businessUnitCode);
    }

    validateReplacement(toBeReplaced, newWarehouse);

    archiveWarehouseOperation.archive(toBeReplaced);
    createWarehouseOperation.create(newWarehouse);

    LOGGER.infof(
        "Warehouse %s replaced at %s: capacity %d -> %d",
        newWarehouse.businessUnitCode,
        newWarehouse.location,
        toBeReplaced.capacity,
        newWarehouse.capacity);
  }

  private void validateReplacement(Warehouse toBeReplaced, Warehouse newWarehouse) {
    if (newWarehouse.capacity < toBeReplaced.stock) {
      throw new WarehouseValidationException(
          "Capacity "
              + newWarehouse.capacity
              + " of the new warehouse cannot accommodate the stock "
              + toBeReplaced.stock
              + " of the warehouse being replaced");
    }

    if (!newWarehouse.stock.equals(toBeReplaced.stock)) {
      throw new WarehouseValidationException(
          "Stock "
              + newWarehouse.stock
              + " of the new warehouse must match the stock "
              + toBeReplaced.stock
              + " of the warehouse being replaced");
    }
  }
}
