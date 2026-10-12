package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * States that a {@link DbWarehouse} fulfils a given {@link Product} for a given {@link Store}.
 *
 * <p>The triple is unique: the database constraint is the last line of defence behind the explicit
 * checks in {@link FulfilmentService}, and guards against two concurrent requests both passing the
 * application level validation.
 */
@Entity
@Table(
    name = "fulfilment_association",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_fulfilment_store_product_warehouse",
            columnNames = {"store_id", "product_id", "warehouse_id"}))
public class FulfilmentAssociation extends PanacheEntity {

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false)
  public Store store;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  public Product product;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id", nullable = false)
  public DbWarehouse warehouse;

  public LocalDateTime createdAt;

  public FulfilmentAssociation() {}

  public FulfilmentAssociation(Store store, Product product, DbWarehouse warehouse) {
    this.store = store;
    this.product = product;
    this.warehouse = warehouse;
    this.createdAt = LocalDateTime.now();
  }
}
