package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Panache based implementation of the {@link WarehouseStore} port.
 *
 * <p>Archiving is a soft delete: an archived row keeps its history and its business unit code, but
 * is excluded from every lookup so that the code can be reused by a replacement warehouse.
 */
@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final Logger LOGGER = Logger.getLogger(WarehouseRepository.class);

  private static final String ACTIVE = "archivedAt is null";

  @Override
  public List<Warehouse> getAll() {
    return find(ACTIVE).list().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    if (warehouse.createdAt == null) {
      warehouse.createdAt = LocalDateTime.now();
    }

    var dbWarehouse = new DbWarehouse(warehouse);
    persist(dbWarehouse);

    // propagate the generated id back so that callers can expose it
    warehouse.id = dbWarehouse.id;
    LOGGER.infof(
        "Created warehouse %s at %s (id=%d)",
        warehouse.businessUnitCode, warehouse.location, warehouse.id);
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse = findRowOf(warehouse);
    dbWarehouse.applyFrom(warehouse);
    persist(dbWarehouse);
    LOGGER.infof("Updated warehouse %s (id=%d)", dbWarehouse.businessUnitCode, dbWarehouse.id);
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    DbWarehouse dbWarehouse = findRowOf(warehouse);
    delete(dbWarehouse);
    LOGGER.infof("Removed warehouse %s (id=%d)", dbWarehouse.businessUnitCode, dbWarehouse.id);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    if (buCode == null || buCode.isBlank()) {
      return null;
    }

    return find(ACTIVE + " and businessUnitCode = ?1", buCode)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }

  @Override
  public Warehouse getById(Long id) {
    if (id == null) {
      return null;
    }

    return find(ACTIVE + " and id = ?1", id)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }

  /** Resolves the managed row backing a domain Warehouse, by id when known, by code otherwise. */
  private DbWarehouse findRowOf(Warehouse warehouse) {
    DbWarehouse dbWarehouse =
        warehouse.id != null
            ? findById(warehouse.id)
            : find(ACTIVE + " and businessUnitCode = ?1", warehouse.businessUnitCode)
                .firstResult();

    if (dbWarehouse == null) {
      throw new IllegalStateException(
          "Warehouse " + warehouse.businessUnitCode + " is not persisted and cannot be modified");
    }
    return dbWarehouse;
  }
}
