package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * REST adapter for the Warehouse API generated from {@code warehouse-openapi.yaml}.
 *
 * <p>It only translates between the generated transport beans and the domain model; every business
 * rule lives in the use cases, and domain exceptions are turned into HTTP responses by the
 * application wide {@code RestExceptionMapper}.
 *
 * <p>The {@code {id}} path parameter is the technical identifier of the unit, as used by the
 * existing integration test. The business unit code remains the key of the replacement endpoint,
 * exactly as declared in the specification.
 */
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject WarehouseStore warehouseStore;

  @Inject CreateWarehouseOperation createWarehouseOperation;

  @Inject ReplaceWarehouseOperation replaceWarehouseOperation;

  @Inject ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseStore.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = toDomainModel(data);
    createWarehouseOperation.create(warehouse);

    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    var warehouse = warehouseStore.getById(parseId(id));
    if (warehouse == null) {
      throw new WarehouseNotFoundException("Warehouse with id of " + id + " does not exist.");
    }

    return toWarehouseResponse(warehouse);
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    var warehouse = warehouseStore.getById(parseId(id));
    if (warehouse == null) {
      throw new WarehouseNotFoundException("Warehouse with id of " + id + " does not exist.");
    }

    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var newWarehouse = toDomainModel(data);
    // the path parameter is authoritative for the unit being replaced
    newWarehouse.businessUnitCode = businessUnitCode;

    replaceWarehouseOperation.replace(newWarehouse);

    return toWarehouseResponse(newWarehouse);
  }

  private Long parseId(String id) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException e) {
      throw new WarehouseValidationException("Warehouse id must be a number, but was '" + id + "'");
    }
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainModel(
      Warehouse data) {
    if (data == null) {
      throw new WarehouseValidationException("Warehouse payload is mandatory");
    }

    if (data.getBusinessUnitCode() == null || data.getBusinessUnitCode().isBlank()) {
      throw new WarehouseValidationException("Business unit code is mandatory");
    }

    return new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse(
        data.getBusinessUnitCode(), data.getLocation(), data.getCapacity(), data.getStock());
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setId(warehouse.id == null ? null : String.valueOf(warehouse.id));
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
