package com.fulfilment.application.monolith.health;

import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness probe: the application is only able to serve traffic if it can actually read the
 * warehouse register, which exercises the datasource and the mapping layer end to end.
 */
@Readiness
@ApplicationScoped
public class WarehouseStoreHealthCheck implements HealthCheck {

  @Inject WarehouseStore warehouseStore;

  @Override
  public HealthCheckResponse call() {
    try {
      int activeWarehouses = warehouseStore.getAll().size();
      return HealthCheckResponse.named("warehouse-store")
          .up()
          .withData("activeWarehouses", activeWarehouses)
          .build();
    } catch (RuntimeException e) {
      return HealthCheckResponse.named("warehouse-store")
          .down()
          .withData("error", String.valueOf(e.getMessage()))
          .build();
    }
  }
}
