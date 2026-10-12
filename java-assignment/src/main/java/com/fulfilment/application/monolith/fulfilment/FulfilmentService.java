package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Enforces the fulfilment constraints of the bonus task.
 *
 * <ol>
 *   <li>a product can be fulfilled by at most {@value #MAX_WAREHOUSES_PER_PRODUCT_PER_STORE}
 *       warehouses per store;
 *   <li>a store can be fulfilled by at most {@value #MAX_WAREHOUSES_PER_STORE} warehouses;
 *   <li>a warehouse can store at most {@value #MAX_PRODUCT_TYPES_PER_WAREHOUSE} product types.
 * </ol>
 *
 * <p>Limits 2 and 3 count <em>distinct</em> participants, so adding a product to a warehouse that
 * already serves the store, or a store to a warehouse that already stores the product, does not
 * consume a new slot.
 */
@ApplicationScoped
public class FulfilmentService {

  public static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  public static final int MAX_WAREHOUSES_PER_STORE = 3;
  public static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;

  private static final Logger LOGGER = Logger.getLogger(FulfilmentService.class);

  @Inject FulfilmentRepository fulfilmentRepository;
  @Inject ProductRepository productRepository;
  @Inject WarehouseRepository warehouseRepository;

  @Transactional
  public List<FulfilmentView> listAll() {
    return fulfilmentRepository.listAllOrdered().stream().map(FulfilmentView::of).toList();
  }

  @Transactional
  public List<FulfilmentView> listByStore(Long storeId) {
    requireStore(storeId);
    return fulfilmentRepository.listByStore(storeId).stream().map(FulfilmentView::of).toList();
  }

  /** Associates a warehouse as a fulfilment unit of a product for a store. */
  @Transactional
  public FulfilmentView associate(FulfilmentRequest request) {
    validateRequest(request);

    Store store = requireStore(request.storeId());
    Product product = requireProduct(request.productId());
    DbWarehouse warehouse = requireActiveWarehouse(request.warehouseBusinessUnitCode());

    checkConstraints(store, product, warehouse);

    var association = new FulfilmentAssociation(store, product, warehouse);
    fulfilmentRepository.persist(association);

    LOGGER.infof(
        "Warehouse %s now fulfils product %d for store %d (association id=%d)",
        warehouse.businessUnitCode, product.id, store.id, association.id);
    return FulfilmentView.of(association);
  }

  /** Removes an existing association. Idempotency is intentionally not offered: unknown ids 404. */
  @Transactional
  public void dissociate(Long associationId) {
    FulfilmentAssociation association = fulfilmentRepository.findById(associationId);
    if (association == null) {
      throw new FulfilmentNotFoundException(
          "Fulfilment association with id of " + associationId + " does not exist.");
    }

    fulfilmentRepository.delete(association);
    LOGGER.infof("Removed fulfilment association id=%d", associationId);
  }

  private void checkConstraints(Store store, Product product, DbWarehouse warehouse) {
    if (fulfilmentRepository.findAssociation(store.id, product.id, warehouse.id).isPresent()) {
      throw new FulfilmentValidationException(
          "Warehouse "
              + warehouse.businessUnitCode
              + " already fulfils product "
              + product.id
              + " for store "
              + store.id
              + ".");
    }

    if (fulfilmentRepository.countWarehousesFulfilling(store.id, product.id)
        >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new FulfilmentValidationException(
          "Product "
              + product.id
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses for store "
              + store.id
              + ".");
    }

    boolean warehouseAlreadyServesStore =
        fulfilmentRepository.warehouseAlreadyServesStore(store.id, warehouse.id);
    if (!warehouseAlreadyServesStore
        && fulfilmentRepository.countDistinctWarehousesOfStore(store.id)
            >= MAX_WAREHOUSES_PER_STORE) {
      throw new FulfilmentValidationException(
          "Store "
              + store.id
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " warehouses.");
    }

    boolean warehouseAlreadyStoresProduct =
        fulfilmentRepository.warehouseAlreadyStoresProduct(warehouse.id, product.id);
    if (!warehouseAlreadyStoresProduct
        && fulfilmentRepository.countDistinctProductsOfWarehouse(warehouse.id)
            >= MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
      throw new FulfilmentValidationException(
          "Warehouse "
              + warehouse.businessUnitCode
              + " already stores the maximum of "
              + MAX_PRODUCT_TYPES_PER_WAREHOUSE
              + " product types.");
    }
  }

  private void validateRequest(FulfilmentRequest request) {
    if (request == null) {
      throw new FulfilmentValidationException("A fulfilment request body is required.");
    }
    if (request.storeId() == null) {
      throw new FulfilmentValidationException("The storeId is required.");
    }
    if (request.productId() == null) {
      throw new FulfilmentValidationException("The productId is required.");
    }
    if (request.warehouseBusinessUnitCode() == null
        || request.warehouseBusinessUnitCode().isBlank()) {
      throw new FulfilmentValidationException("The warehouseBusinessUnitCode is required.");
    }
  }

  private Store requireStore(Long storeId) {
    Store store = Store.findById(storeId);
    if (store == null) {
      throw new FulfilmentNotFoundException("Store with id of " + storeId + " does not exist.");
    }
    return store;
  }

  private Product requireProduct(Long productId) {
    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new FulfilmentNotFoundException("Product with id of " + productId + " does not exist.");
    }
    return product;
  }

  private DbWarehouse requireActiveWarehouse(String businessUnitCode) {
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    if (warehouse == null) {
      throw new FulfilmentNotFoundException(
          "Warehouse with business unit code of " + businessUnitCode + " does not exist.");
    }
    return warehouseRepository.findById(warehouse.id);
  }
}
