package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/** Runs against the packaged application, so it also covers the wiring and the serialisation. */
@QuarkusIntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseEndpointIT {

  private static final String PATH = "warehouse";

  @Test
  @Order(1)
  public void testSimpleListWarehouses() {

    // List all, should have all 3 warehouses the database has initially:
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  @Order(2)
  public void testCreateAndReplaceWarehouse() {

    given()
        .body(
            "{\"businessUnitCode\":\"MWH.900\",\"location\":\"AMSTERDAM-002\",\"capacity\":30,\"stock\":10}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("businessUnitCode", is("MWH.900"));

    given()
        .body(
            "{\"businessUnitCode\":\"MWH.900\",\"location\":\"AMSTERDAM-002\",\"capacity\":40,\"stock\":10}")
        .contentType("application/json")
        .when()
        .post(PATH + "/MWH.900/replacement")
        .then()
        .statusCode(200)
        .body("capacity", is(40));
  }

  @Test
  @Order(3)
  public void testSimpleCheckingArchivingWarehouses() {

    // List all, should have all 3 warehouses the database has initially:
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive the ZWOLLE-001:
    given().when().delete(PATH + "/1").then().statusCode(204);

    // List all, ZWOLLE-001 should be missing now:
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            not(containsString("ZWOLLE-001")),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));
  }
}
