package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Business rule coverage of the fulfilment associations (bonus task).
 *
 * <p>Every test runs inside a transaction that is rolled back afterwards ({@link TestTransaction}),
 * so each case builds its own fixtures without leaking state into the other test classes.
 */
@QuarkusTest
class FulfilmentServiceTest {

  @Inject FulfilmentService fulfilmentService;
  @Inject FulfilmentRepository fulfilmentRepository;
  @Inject ProductRepository productRepository;
  @Inject WarehouseRepository warehouseRepository;

  // --- positive paths ------------------------------------------------------

  @Test
  @TestTransaction
  void shouldAssociateAWarehouseAsFulfilmentUnitOfAProductForAStore() {
    Store store = newStore();
    Product product = newProduct();
    DbWarehouse warehouse = newWarehouse();

    FulfilmentView view = fulfilmentService.associate(request(store, product, warehouse));

    assertNotNull(view.id());
    assertEquals(store.id, view.storeId());
    assertEquals(product.id, view.productId());
    assertEquals(warehouse.businessUnitCode, view.warehouseBusinessUnitCode());
    assertNotNull(view.createdAt());
  }

  @Test
  @TestTransaction
  void shouldAllowTwoWarehousesToFulfilTheSameProductForTheSameStore() {
    Store store = newStore();
    Product product = newProduct();

    fulfilmentService.associate(request(store, product, newWarehouse()));
    FulfilmentView second = fulfilmentService.associate(request(store, product, newWarehouse()));

    assertNotNull(second.id());
    assertEquals(2, fulfilmentRepository.countWarehousesFulfilling(store.id, product.id));
  }

  @Test
  @TestTransaction
  void shouldNotConsumeANewStoreSlotWhenTheWarehouseAlreadyFulfilsThatStore() {
    Store store = newStore();
    DbWarehouse first = newWarehouse();
    fulfilmentService.associate(request(store, newProduct(), first));
    fulfilmentService.associate(request(store, newProduct(), newWarehouse()));
    fulfilmentService.associate(request(store, newProduct(), newWarehouse()));

    // the store is already served by the maximum of 3 warehouses, but `first` is one of them
    FulfilmentView view = fulfilmentService.associate(request(store, newProduct(), first));

    assertNotNull(view.id());
    assertEquals(3, fulfilmentRepository.countDistinctWarehousesOfStore(store.id));
  }

  @Test
  @TestTransaction
  void shouldNotConsumeANewProductSlotWhenTheWarehouseAlreadyStoresThatProductType() {
    DbWarehouse warehouse = newWarehouse();
    Product shared = newProduct();
    fulfilmentService.associate(request(newStore(), shared, warehouse));
    for (int i = 0; i < FulfilmentService.MAX_PRODUCT_TYPES_PER_WAREHOUSE - 1; i++) {
      fulfilmentService.associate(request(newStore(), newProduct(), warehouse));
    }

    // the warehouse is full on product types, but `shared` is already one of them
    FulfilmentView view = fulfilmentService.associate(request(newStore(), shared, warehouse));

    assertNotNull(view.id());
    assertEquals(5, fulfilmentRepository.countDistinctProductsOfWarehouse(warehouse.id));
  }

  // --- constraint violations -----------------------------------------------

  @Test
  @TestTransaction
  void shouldRejectAThirdWarehouseForTheSameProductAndStore() {
    Store store = newStore();
    Product product = newProduct();
    fulfilmentService.associate(request(store, product, newWarehouse()));
    fulfilmentService.associate(request(store, product, newWarehouse()));

    FulfilmentRequest third = request(store, product, newWarehouse());
    var exception =
        assertThrows(
            FulfilmentValidationException.class, () -> fulfilmentService.associate(third));

    assertTrue(exception.getMessage().contains("maximum of 2 warehouses"));
  }

  @Test
  @TestTransaction
  void shouldRejectAFourthWarehouseForTheSameStore() {
    Store store = newStore();
    fulfilmentService.associate(request(store, newProduct(), newWarehouse()));
    fulfilmentService.associate(request(store, newProduct(), newWarehouse()));
    fulfilmentService.associate(request(store, newProduct(), newWarehouse()));

    FulfilmentRequest fourth = request(store, newProduct(), newWarehouse());
    var exception =
        assertThrows(
            FulfilmentValidationException.class, () -> fulfilmentService.associate(fourth));

    assertTrue(exception.getMessage().contains("maximum of 3 warehouses"));
  }

  @Test
  @TestTransaction
  void shouldRejectASixthProductTypeInTheSameWarehouse() {
    DbWarehouse warehouse = newWarehouse();
    for (int i = 0; i < FulfilmentService.MAX_PRODUCT_TYPES_PER_WAREHOUSE; i++) {
      fulfilmentService.associate(request(newStore(), newProduct(), warehouse));
    }

    FulfilmentRequest sixth = request(newStore(), newProduct(), warehouse);
    var exception =
        assertThrows(
            FulfilmentValidationException.class, () -> fulfilmentService.associate(sixth));

    assertTrue(exception.getMessage().contains("maximum of 5 product types"));
  }

  @Test
  @TestTransaction
  void shouldRejectADuplicatedAssociation() {
    FulfilmentRequest duplicate = request(newStore(), newProduct(), newWarehouse());
    fulfilmentService.associate(duplicate);

    var exception =
        assertThrows(
            FulfilmentValidationException.class, () -> fulfilmentService.associate(duplicate));

    assertTrue(exception.getMessage().contains("already fulfils"));
  }

  // --- unknown references ---------------------------------------------------

  @Test
  @TestTransaction
  void shouldRejectAnUnknownStore() {
    var request = new FulfilmentRequest(99999L, newProduct().id, newWarehouse().businessUnitCode);

    var exception =
        assertThrows(
            FulfilmentNotFoundException.class, () -> fulfilmentService.associate(request));

    assertTrue(exception.getMessage().contains("Store with id of 99999"));
  }

  @Test
  @TestTransaction
  void shouldRejectAnUnknownProduct() {
    var request = new FulfilmentRequest(newStore().id, 99999L, newWarehouse().businessUnitCode);

    var exception =
        assertThrows(
            FulfilmentNotFoundException.class, () -> fulfilmentService.associate(request));

    assertTrue(exception.getMessage().contains("Product with id of 99999"));
  }

  @Test
  @TestTransaction
  void shouldRejectAnUnknownOrArchivedWarehouse() {
    var request = new FulfilmentRequest(newStore().id, newProduct().id, "DOES.NOT.EXIST");

    var exception =
        assertThrows(
            FulfilmentNotFoundException.class, () -> fulfilmentService.associate(request));

    assertTrue(exception.getMessage().contains("DOES.NOT.EXIST"));
  }

  @Test
  @TestTransaction
  void shouldRejectAWarehouseThatHasBeenArchived() {
    DbWarehouse warehouse = newWarehouse();
    warehouse.archivedAt = LocalDateTime.now();
    warehouseRepository.persist(warehouse);

    var request = new FulfilmentRequest(newStore().id, newProduct().id, warehouse.businessUnitCode);

    assertThrows(FulfilmentNotFoundException.class, () -> fulfilmentService.associate(request));
  }

  // --- request validation ---------------------------------------------------

  @Test
  void shouldRejectAMissingBody() {
    var exception =
        assertThrows(FulfilmentValidationException.class, () -> fulfilmentService.associate(null));

    assertTrue(exception.getMessage().contains("body is required"));
  }

  @Test
  void shouldRejectAMissingStoreId() {
    var request = new FulfilmentRequest(null, 1L, "MWH.001");

    var exception =
        assertThrows(
            FulfilmentValidationException.class, () -> fulfilmentService.associate(request));

    assertTrue(exception.getMessage().contains("storeId is required"));
  }

  @Test
  void shouldRejectAMissingProductId() {
    var request = new FulfilmentRequest(1L, null, "MWH.001");

    var exception =
        assertThrows(
            FulfilmentValidationException.class, () -> fulfilmentService.associate(request));

    assertTrue(exception.getMessage().contains("productId is required"));
  }

  @Test
  void shouldRejectABlankWarehouseBusinessUnitCode() {
    var request = new FulfilmentRequest(1L, 1L, "  ");

    var exception =
        assertThrows(
            FulfilmentValidationException.class, () -> fulfilmentService.associate(request));

    assertTrue(exception.getMessage().contains("warehouseBusinessUnitCode is required"));
  }

  // --- reads and removal ----------------------------------------------------

  @Test
  @TestTransaction
  void shouldListTheAssociationsOfAStoreOnly() {
    Store store = newStore();
    fulfilmentService.associate(request(store, newProduct(), newWarehouse()));
    fulfilmentService.associate(request(store, newProduct(), newWarehouse()));
    fulfilmentService.associate(request(newStore(), newProduct(), newWarehouse()));

    List<FulfilmentView> views = fulfilmentService.listByStore(store.id);

    assertEquals(2, views.size());
    assertTrue(views.stream().allMatch(view -> store.id.equals(view.storeId())));
    assertTrue(fulfilmentService.listAll().size() >= 3);
  }

  @Test
  @TestTransaction
  void shouldRejectListingTheAssociationsOfAnUnknownStore() {
    assertThrows(FulfilmentNotFoundException.class, () -> fulfilmentService.listByStore(99999L));
  }

  @Test
  @TestTransaction
  void shouldRemoveAnExistingAssociation() {
    Store store = newStore();
    FulfilmentView view = fulfilmentService.associate(request(store, newProduct(), newWarehouse()));

    fulfilmentService.dissociate(view.id());

    assertTrue(fulfilmentService.listByStore(store.id).isEmpty());
  }

  @Test
  @TestTransaction
  void shouldRejectRemovingAnUnknownAssociation() {
    var exception =
        assertThrows(
            FulfilmentNotFoundException.class, () -> fulfilmentService.dissociate(99999L));

    assertTrue(exception.getMessage().contains("does not exist"));
  }

  // --- fixtures -------------------------------------------------------------

  private FulfilmentRequest request(Store store, Product product, DbWarehouse warehouse) {
    return new FulfilmentRequest(store.id, product.id, warehouse.businessUnitCode);
  }

  private Store newStore() {
    var store = new Store("STORE-" + unique());
    store.quantityProductsInStock = 1;
    store.persist();
    return store;
  }

  private Product newProduct() {
    var product = new Product("PRODUCT-" + unique());
    productRepository.persist(product);
    return product;
  }

  private DbWarehouse newWarehouse() {
    var warehouse = new DbWarehouse();
    warehouse.businessUnitCode = "WH." + unique();
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 10;
    warehouse.stock = 1;
    warehouse.createdAt = LocalDateTime.now();
    warehouseRepository.persist(warehouse);
    return warehouse;
  }

  private static String unique() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}
