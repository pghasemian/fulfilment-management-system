package com.fulfilment.application.monolith.products;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * Covers the error and update paths of the Product API, which the original CRUD test left out.
 * Every test works on its own product so that it stays independent from the others.
 */
@QuarkusTest
class ProductResourceTest {

  private static final String PATH = "product";

  @Test
  void shouldGetASingleProduct() {
    given().when().get(PATH + "/2").then().statusCode(200).body("name", is("KALLAX"));
  }

  @Test
  void shouldReturnNotFoundForAnUnknownProduct() {
    given()
        .when()
        .get(PATH + "/9999")
        .then()
        .statusCode(404)
        .body("error", containsString("does not exist"));
  }

  @Test
  void shouldCreateAProduct() {
    given()
        .body("{\"name\":\"LISABO\",\"description\":\"table\",\"price\":99.95,\"stock\":4}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("name", is("LISABO"), "stock", is(4));
  }

  @Test
  void shouldRejectCreationWhenIdIsSet() {
    given()
        .body("{\"id\":42,\"name\":\"NORDLI\"}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body("error", containsString("Id was invalidly set"));
  }

  @Test
  void shouldUpdateAProduct() {
    Integer id = createProduct("STRANDMON");

    given()
        .body("{\"name\":\"STRANDMON-2\",\"description\":\"armchair\",\"price\":10.50,\"stock\":2}")
        .contentType("application/json")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("STRANDMON-2"), "description", is("armchair"), "stock", is(2));
  }

  @Test
  void shouldRejectUpdateWithoutName() {
    given()
        .body("{\"stock\":2}")
        .contentType("application/json")
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(422)
        .body("error", containsString("Product Name was not set"));
  }

  @Test
  void shouldReturnNotFoundWhenUpdatingAnUnknownProduct() {
    given()
        .body("{\"name\":\"GHOST\"}")
        .contentType("application/json")
        .when()
        .put(PATH + "/9999")
        .then()
        .statusCode(404);
  }

  @Test
  void shouldDeleteAProduct() {
    Integer id = createProduct("EKTORP");

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH).then().statusCode(200).body(not(containsString("EKTORP")));
  }

  @Test
  void shouldReturnNotFoundWhenDeletingAnUnknownProduct() {
    given().when().delete(PATH + "/9999").then().statusCode(404);
  }

  private Integer createProduct(String name) {
    return given()
        .body("{\"name\":\"" + name + "\",\"stock\":1}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }
}
