package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StoreResourceTest {

  private static final String PATH = "/store";

  // ==================== GET All ====================

  @Test
  @Order(1)
  void shouldListAllStores() {
    // Given — no setup needed, stores may already exist from seed data

    // When & Then
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("size()", greaterThanOrEqualTo(0));
  }

  // ==================== POST ====================

  @Test
  @Order(2)
  void shouldCreateStoreSuccessfully() {
    // Given
    String body = """
        {
          "name": "IntegrationTestStore",
          "quantityProductsInStock": 42
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("name", equalTo("IntegrationTestStore"))
        .body("quantityProductsInStock", equalTo(42))
        .body("id", notNullValue());
  }

  @Test
  @Order(3)
  void shouldRejectCreateWhenIdIsSet() {
    // Given
    String body = """
        {
          "id": 999,
          "name": "InvalidStore",
          "quantityProductsInStock": 10
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body("error", containsString("Id was invalidly set"));
  }

  @Test
  @Order(4)
  void shouldCreateStoreWithZeroStock() {
    // Given
    String body = """
        {
          "name": "ZeroStockStore",
          "quantityProductsInStock": 0
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("quantityProductsInStock", equalTo(0));
  }

  @Test
  @Order(5)
  void shouldCreateStoreWithLargeStock() {
    // Given
    String body = """
        {
          "name": "LargeStockStore",
          "quantityProductsInStock": 2147483647
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("quantityProductsInStock", equalTo(2147483647));
  }

  // ==================== GET by ID ====================

  @Test
  @Order(6)
  void shouldGetStoreByValidId() {
    // Given — create a store to get a known ID
    String createBody = """
        {
          "name": "GetByIdStore",
          "quantityProductsInStock": 15
        }
        """;

    Integer id = given()
        .contentType(ContentType.JSON)
        .body(createBody)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    // When & Then
    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("GetByIdStore"))
        .body("quantityProductsInStock", equalTo(15));
  }

  @Test
  @Order(7)
  void shouldReturn404ForNonExistentStore() {
    // Given
    long nonExistentId = 999999L;

    // When & Then
    given()
        .when()
        .get(PATH + "/" + nonExistentId)
        .then()
        .statusCode(404)
        .body("error", containsString("does not exist"));
  }

  // ==================== PUT ====================

  @Test
  @Order(8)
  void shouldUpdateStoreSuccessfully() {
    // Given — create a store first
    String createBody = """
        {
          "name": "ToBeUpdated",
          "quantityProductsInStock": 10
        }
        """;

    Integer id = given()
        .contentType(ContentType.JSON)
        .body(createBody)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    String updateBody = """
        {
          "name": "UpdatedName",
          "quantityProductsInStock": 99
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("UpdatedName"))
        .body("quantityProductsInStock", equalTo(99));
  }

  @Test
  @Order(9)
  void shouldRejectUpdateWhenNameIsNull() {
    // Given
    String updateBody = """
        {
          "quantityProductsInStock": 10
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422)
        .body("error", containsString("Store Name was not set"));
  }

  @Test
  @Order(10)
  void shouldReturn404WhenUpdatingNonExistentStore() {
    // Given
    String updateBody = """
        {
          "name": "Ghost",
          "quantityProductsInStock": 10
        }
        """;
    long nonExistentId = 999999L;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/" + nonExistentId)
        .then()
        .statusCode(404)
        .body("error", containsString("does not exist"));
  }

  // ==================== PATCH ====================

  @Test
  @Order(11)
  void shouldPatchStoreSuccessfully() {
    // Given — create a store first
    String createBody = """
        {
          "name": "ToBePatched",
          "quantityProductsInStock": 30
        }
        """;

    Integer id = given()
        .contentType(ContentType.JSON)
        .body(createBody)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    String patchBody = """
        {
          "name": "PatchedName",
          "quantityProductsInStock": 55
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PatchedName"))
        .body("quantityProductsInStock", equalTo(55));
  }

  @Test
  @Order(12)
  void shouldPatchWithoutNameKeepsOriginalName() {
    // Given — create a store first so we have a guaranteed valid ID and known name
    String createBody = """
        {
          "name": "PatchNoNameStore",
          "quantityProductsInStock": 30
        }
        """;

    Integer id = given()
        .contentType(ContentType.JSON)
        .body(createBody)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    String patchBody = """
        {
          "quantityProductsInStock": 10
        }
        """;

    // When & Then — PATCH is a partial update, so omitting name should keep the original
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PatchNoNameStore"))
        .body("quantityProductsInStock", equalTo(10));
  }

  @Test
  @Order(13)
  void shouldReturn404WhenPatchingNonExistentStore() {
    // Given
    String patchBody = """
        {
          "name": "Ghost",
          "quantityProductsInStock": 10
        }
        """;
    long nonExistentId = 999999L;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .when()
        .patch(PATH + "/" + nonExistentId)
        .then()
        .statusCode(404)
        .body("error", containsString("does not exist"));
  }

  @Test
  @Order(14)
  void shouldPatchStoreWithZeroStockKeepingOldStock() {
    // Given — create a store with non-zero stock
    String createBody = """
        {
          "name": "PatchedZeroStock",
          "quantityProductsInStock": 30
        }
        """;

    Integer id = given()
        .contentType(ContentType.JSON)
        .body(createBody)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    String patchBody = """
        {
          "name": "PatchedZeroStock",
          "quantityProductsInStock": 99
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PatchedZeroStock"))
        .body("quantityProductsInStock", equalTo(99));
  }

  // ==================== DELETE ====================

  @Test
  @Order(15)
  void shouldDeleteStoreSuccessfully() {
    // Given — create a store first
    String createBody = """
        {
          "name": "ToBeDeleted",
          "quantityProductsInStock": 5
        }
        """;

    Integer id = given()
        .contentType(ContentType.JSON)
        .body(createBody)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    // When
    given()
        .when()
        .delete(PATH + "/" + id)
        .then()
        .statusCode(204);

    // Then — verify it's gone
    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(404);
  }

  @Test
  @Order(16)
  void shouldReturn404WhenDeletingNonExistentStore() {
    // Given
    long nonExistentId = 999999L;

    // When & Then
    given()
        .when()
        .delete(PATH + "/" + nonExistentId)
        .then()
        .statusCode(404)
        .body("error", containsString("does not exist"));
  }

  // ==================== Error Mapper ====================

  @Test
  @Order(17)
  void shouldReturnJsonErrorFormatOn404() {
    // Given
    long nonExistentId = 999999L;

    // When & Then
    given()
        .when()
        .get(PATH + "/" + nonExistentId)
        .then()
        .statusCode(404)
        .body("exceptionType", equalTo("jakarta.ws.rs.WebApplicationException"))
        .body("code", equalTo(404))
        .body("error", containsString("does not exist"));
  }

  @Test
  @Order(18)
  void shouldReturnJsonErrorFormatOn422() {
    // Given
    String body = """
        {
          "id": 1,
          "name": "Bad",
          "quantityProductsInStock": 0
        }
        """;

    // When & Then
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body("exceptionType", equalTo("jakarta.ws.rs.WebApplicationException"))
        .body("code", equalTo(422))
        .body("error", containsString("Id was invalidly set"));
  }
}