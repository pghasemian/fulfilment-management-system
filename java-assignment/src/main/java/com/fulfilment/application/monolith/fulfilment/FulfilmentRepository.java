package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/** Read side of the fulfilment associations, shaped around the three business constraints. */
@ApplicationScoped
public class FulfilmentRepository implements PanacheRepository<FulfilmentAssociation> {

  public List<FulfilmentAssociation> listAllOrdered() {
    return find("order by store.id, product.id, warehouse.businessUnitCode").list();
  }

  public List<FulfilmentAssociation> listByStore(Long storeId) {
    return find("store.id = ?1 order by product.id, warehouse.businessUnitCode", storeId).list();
  }

  public Optional<FulfilmentAssociation> findAssociation(
      Long storeId, Long productId, Long warehouseId) {
    return find(
            "store.id = ?1 and product.id = ?2 and warehouse.id = ?3",
            storeId,
            productId,
            warehouseId)
        .firstResultOptional();
  }

  /** Constraint 1: how many distinct warehouses already fulfil this product for this store. */
  public long countWarehousesFulfilling(Long storeId, Long productId) {
    return count("store.id = ?1 and product.id = ?2", storeId, productId);
  }

  /** Constraint 2: how many distinct warehouses already fulfil anything for this store. */
  public long countDistinctWarehousesOfStore(Long storeId) {
    return countDistinct(
        "select count(distinct f.warehouse.id) from FulfilmentAssociation f where f.store.id = ?1",
        storeId);
  }

  /** Constraint 3: how many distinct product types this warehouse already stores. */
  public long countDistinctProductsOfWarehouse(Long warehouseId) {
    return countDistinct(
        "select count(distinct f.product.id) from FulfilmentAssociation f where f.warehouse.id ="
            + " ?1",
        warehouseId);
  }

  public boolean warehouseAlreadyServesStore(Long storeId, Long warehouseId) {
    return count("store.id = ?1 and warehouse.id = ?2", storeId, warehouseId) > 0;
  }

  public boolean warehouseAlreadyStoresProduct(Long warehouseId, Long productId) {
    return count("warehouse.id = ?1 and product.id = ?2", warehouseId, productId) > 0;
  }

  private long countDistinct(String jpql, Long parameter) {
    return getEntityManager()
        .createQuery(jpql, Long.class)
        .setParameter(1, parameter)
        .getSingleResult();
  }
}
