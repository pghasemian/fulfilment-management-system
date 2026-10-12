package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Creates a new Warehouse unit, enforcing the constraints of the business unit code, the location
 * and the declared capacity.
 */
@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    validateRequiredFields(warehouse);

    // the business unit code identifies a warehouse, it cannot be held by two active units
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WarehouseValidationException(
          "Warehouse with business unit code "
              + warehouse.businessUnitCode
              + " already exists and is active");
    }

    Location location = resolveLocation(warehouse.location);
    validateAgainstLocation(warehouse, location);

    warehouse.archivedAt = null;
    if (warehouse.createdAt == null) {
      warehouse.createdAt = LocalDateTime.now();
    }

    warehouseStore.create(warehouse);
    LOGGER.infof(
        "Warehouse %s created at %s with capacity %d and stock %d",
        warehouse.businessUnitCode, warehouse.location, warehouse.capacity, warehouse.stock);
  }

  /**
   * Resolves a location identifier, rejecting the request when it is not part of the catalogue.
   * Shared with the replacement flow.
   */
  Location resolveLocation(String identifier) {
    Location location = locationResolver.resolveByIdentifier(identifier);
    if (location == null) {
      throw new WarehouseValidationException("Location " + identifier + " is not a valid location");
    }
    return location;
  }

  static void validateRequiredFields(Warehouse warehouse) {
    if (warehouse == null) {
      throw new WarehouseValidationException("Warehouse payload is mandatory");
    }
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new WarehouseValidationException("Business unit code is mandatory");
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new WarehouseValidationException("Location is mandatory");
    }
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new WarehouseValidationException("Capacity must be a positive number");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new WarehouseValidationException("Stock must be zero or a positive number");
    }
    if (warehouse.stock > warehouse.capacity) {
      throw new WarehouseValidationException(
          "Stock "
              + warehouse.stock
              + " exceeds the capacity "
              + warehouse.capacity
              + " of the warehouse");
    }
  }

  /**
   * Checks that the location can host one more warehouse and that its total capacity is not
   * exceeded. Only active warehouses occupy a location.
   */
  private void validateAgainstLocation(Warehouse warehouse, Location location) {
    List<Warehouse> warehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> location.identification.equals(w.location))
            .toList();

    if (warehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Location "
              + location.identification
              + " already reached its maximum of "
              + location.maxNumberOfWarehouses
              + " warehouses");
    }

    int usedCapacity = warehousesAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Capacity "
              + warehouse.capacity
              + " exceeds the remaining capacity of "
              + (location.maxCapacity - usedCapacity)
              + " at location "
              + location.identification);
    }
  }
}
