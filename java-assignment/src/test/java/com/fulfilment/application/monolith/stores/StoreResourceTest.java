package com.fulfilment.application.monolith.stores;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.MethodName.class)
class StoreResourceTest {

  private static final String PATH = "store";

  @Test
  void testListAllStoresFromTheInitialDataSet() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"));
  }

  @Test
  void testGetSingleStore() {
    given().when().get(PATH + "/1").then().statusCode(200).body("name", is("TONSTAD"));
  }

  @Test
  void testGetUnknownStoreReturnsNotFound() {
    given().when().get(PATH + "/9999").then().statusCode(404).body("error", containsString("does not exist"));
  }

  @Test
  void testCreateStore() {
    given()
        .body("{\"name\":\"IVAR\",\"quantityProductsInStock\":7}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("name", is("IVAR"), "quantityProductsInStock", is(7));

    given().when().get(PATH).then().statusCode(200).body(containsString("IVAR"));
  }

  @Test
  void testCreateStoreDefaultsStockToZeroWhenOmitted() {
    given()
        .body("{\"name\":\"MALM\"}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("quantityProductsInStock", is(0));
  }

  @Test
  void testCreateStoreWithIdIsRejected() {
    given()
        .body("{\"id\":123,\"name\":\"HEMNES\"}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body("error", containsString("Id was invalidly set"));
  }

  @Test
  void testCreateStoreWithoutNameIsRejected() {
    given()
        .body("{\"quantityProductsInStock\":3}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body("error", containsString("Store Name was not set"));
  }

  @Test
  void testUpdateStoreReplacesAllProperties() {
    Integer id = createStore("LACK", 4);

    given()
        .body("{\"name\":\"LACK-RENAMED\",\"quantityProductsInStock\":9}")
        .contentType("application/json")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("LACK-RENAMED"), "quantityProductsInStock", is(9));
  }

  @Test
  void testUpdateStoreWithoutNameIsRejected() {
    given()
        .body("{\"quantityProductsInStock\":9}")
        .contentType("application/json")
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  void testUpdateUnknownStoreReturnsNotFound() {
    given()
        .body("{\"name\":\"GHOST\",\"quantityProductsInStock\":1}")
        .contentType("application/json")
        .when()
        .put(PATH + "/9999")
        .then()
        .statusCode(404);
  }

  @Test
  void testPatchOnlyAppliesProvidedProperties() {
    Integer id = createStore("BILLY", 4);

    given()
        .body("{\"quantityProductsInStock\":12}")
        .contentType("application/json")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("BILLY"), "quantityProductsInStock", is(12));

    given()
        .body("{\"name\":\"BILLY-2\"}")
        .contentType("application/json")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("BILLY-2"), "quantityProductsInStock", is(12));
  }

  @Test
  void testPatchWithEmptyPayloadIsRejected() {
    given()
        .body("{}")
        .contentType("application/json")
        .when()
        .patch(PATH + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  void testPatchWithBlankNameIsRejected() {
    given()
        .body("{\"name\":\"  \"}")
        .contentType("application/json")
        .when()
        .patch(PATH + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  void testPatchUnknownStoreReturnsNotFound() {
    given()
        .body("{\"name\":\"GHOST\"}")
        .contentType("application/json")
        .when()
        .patch(PATH + "/9999")
        .then()
        .statusCode(404);
  }

  @Test
  void testDeleteStore() {
    Integer id = createStore("POANG", 1);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH).then().statusCode(200).body(not(containsString("POANG")));
  }

  @Test
  void testDeleteUnknownStoreReturnsNotFound() {
    given().when().delete(PATH + "/9999").then().statusCode(404);
  }

  private Integer createStore(String name, int stock) {
    return given()
        .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":" + stock + "}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }
}
