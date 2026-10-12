package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

/** Persistence port for Warehouses. Implementations only ever expose active (non archived) units. */
public interface WarehouseStore {

  /** @return all the currently active warehouses. */
  List<Warehouse> getAll();

  void create(Warehouse warehouse);

  void update(Warehouse warehouse);

  void remove(Warehouse warehouse);

  /** @return the active warehouse holding this business unit code, or {@code null}. */
  Warehouse findByBusinessUnitCode(String buCode);

  /** @return the active warehouse with this technical id, or {@code null}. */
  Warehouse getById(Long id);
}
