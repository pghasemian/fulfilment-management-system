package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * End to end coverage of the Warehouse API against a real (in memory) database, complementing the
 * use case unit tests that cover the business rules in isolation.
 */
@QuarkusTest
class WarehouseResourceImplTest {

  private static final String PATH = "warehouse";

  @Test
  void shouldListTheActiveWarehousesOfTheInitialDataSet() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"));
  }

  @Test
  void shouldGetAWarehouseById() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("MWH.001"), "location", is("ZWOLLE-001"), "capacity", is(100));
  }

  @Test
  void shouldReturnNotFoundForAnUnknownWarehouseId() {
    given()
        .when()
        .get(PATH + "/9999")
        .then()
        .statusCode(404)
        .body("error", containsString("does not exist"));
  }

  @Test
  void shouldReturnBadRequestForANonNumericWarehouseId() {
    given()
        .when()
        .get(PATH + "/not-a-number")
        .then()
        .statusCode(400)
        .body("error", containsString("must be a number"));
  }

  @Test
  void shouldCreateANewWarehouseUnit() {
    given()
        .body(payload("MWH.200", "AMSTERDAM-002", 30, 10))
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(
            "businessUnitCode", is("MWH.200"),
            "location", is("AMSTERDAM-002"),
            "capacity", is(30),
            "stock", is(10),
            "id", notNullValue());

    given().when().get(PATH).then().statusCode(200).body(containsString("MWH.200"));
  }

  @Test
  void shouldRejectCreationWithAnAlreadyUsedBusinessUnitCode() {
    given()
        .body(payload("MWH.001", "AMSTERDAM-001", 10, 1))
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("already exists"));
  }

  @Test
  void shouldRejectCreationOnAnUnknownLocation() {
    given()
        .body(payload("MWH.201", "ATLANTIS-001", 10, 1))
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("is not a valid location"));
  }

  @Test
  void shouldRejectCreationWhenTheLocationIsFull() {
    // ZWOLLE-001 accepts a single warehouse and MWH.001 already occupies it
    given()
        .body(payload("MWH.202", "ZWOLLE-001", 10, 1))
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 1 warehouses"));
  }

  @Test
  void shouldRejectCreationWhenStockExceedsCapacity() {
    given()
        .body(payload("MWH.203", "AMSTERDAM-001", 10, 50))
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("exceeds the capacity"));
  }

  @Test
  void shouldRejectCreationWhenMandatoryFieldsAreMissing() {
    given()
        .body("{\"location\":\"AMSTERDAM-001\",\"capacity\":10,\"stock\":1}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("Business unit code is mandatory"));
  }

  @Test
  void shouldArchiveAWarehouseAndRemoveItFromTheActiveList() {
    String id =
        given()
            .body(payload("MWH.300", "HELMOND-001", 20, 5))
            .contentType("application/json")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH).then().statusCode(200).body(not(containsString("MWH.300")));
    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  void shouldReturnNotFoundWhenArchivingAnUnknownWarehouse() {
    given().when().delete(PATH + "/9999").then().statusCode(404);
  }

  @Test
  void shouldReplaceTheActiveWarehouseKeepingItsBusinessUnitCode() {
    given()
        .body(payload("MWH.400", "EINDHOVEN-001", 30, 10))
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .body(payload("MWH.400", "EINDHOVEN-001", 35, 10))
        .contentType("application/json")
        .when()
        .post(PATH + "/MWH.400/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("MWH.400"), "capacity", is(35));

    // the business unit code is still listed exactly once, now with the new capacity
    given().when().get(PATH).then().statusCode(200).body(containsString("MWH.400"));
  }

  @Test
  void shouldReturnNotFoundWhenReplacingAnUnknownBusinessUnitCode() {
    given()
        .body(payload("MWH.404", "EINDHOVEN-001", 10, 1))
        .contentType("application/json")
        .when()
        .post(PATH + "/MWH.404/replacement")
        .then()
        .statusCode(404)
        .body("error", containsString("No active warehouse found"));
  }

  @Test
  void shouldRejectAReplacementWhoseStockDoesNotMatch() {
    given()
        .body(payload("MWH.500", "VETSBY-001", 30, 10))
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .body(payload("MWH.500", "VETSBY-001", 30, 7))
        .contentType("application/json")
        .when()
        .post(PATH + "/MWH.500/replacement")
        .then()
        .statusCode(400)
        .body("error", containsString("must match the stock"));

    // the original warehouse must still be active after the rejected replacement
    given().when().get(PATH).then().statusCode(200).body(containsString("MWH.500"));
  }

  private String payload(String buCode, String location, int capacity, int stock) {
    return "{\"businessUnitCode\":\""
        + buCode
        + "\",\"location\":\""
        + location
        + "\",\"capacity\":"
        + capacity
        + ",\"stock\":"
        + stock
        + "}";
  }
}
