package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse")
@Cacheable
public class DbWarehouse {

  @Id @GeneratedValue public Long id;

  @Column(nullable = false)
  public String businessUnitCode;

  @Column(nullable = false)
  public String location;

  public Integer capacity;

  public Integer stock;

  public LocalDateTime createdAt;

  public LocalDateTime archivedAt;

  public DbWarehouse() {}

  public DbWarehouse(Warehouse warehouse) {
    this.applyFrom(warehouse);
  }

  /** Copies the mutable state of a domain Warehouse onto this row. The id is never overwritten. */
  public void applyFrom(Warehouse warehouse) {
    this.businessUnitCode = warehouse.businessUnitCode;
    this.location = warehouse.location;
    this.capacity = warehouse.capacity;
    this.stock = warehouse.stock;
    this.createdAt = warehouse.createdAt;
    this.archivedAt = warehouse.archivedAt;
  }

  public Warehouse toWarehouse() {
    var warehouse = new Warehouse();
    warehouse.id = this.id;
    warehouse.businessUnitCode = this.businessUnitCode;
    warehouse.location = this.location;
    warehouse.capacity = this.capacity;
    warehouse.stock = this.stock;
    warehouse.createdAt = this.createdAt;
    warehouse.archivedAt = this.archivedAt;
    return warehouse;
  }
}
