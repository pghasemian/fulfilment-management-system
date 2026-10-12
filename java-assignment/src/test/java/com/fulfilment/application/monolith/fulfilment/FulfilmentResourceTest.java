package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * HTTP level coverage of the fulfilment endpoints: status codes, payload shape and the translation
 * of the domain exceptions. The rules themselves are covered by {@link FulfilmentServiceTest}.
 *
 * <p>Each test cleans up the association it created so that the suite stays order independent.
 */
@QuarkusTest
class FulfilmentResourceTest {

  private static final String PATH = "fulfilment";

  @Test
  void shouldCreateAndThenRemoveAnAssociation() {
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body(request(1L, 1L, "MWH.001"))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body(
                "id", notNullValue(),
                "storeId", is(1),
                "productId", is(1),
                "warehouseBusinessUnitCode", is("MWH.001"))
            .extract()
            .path("id");

    given().when().get(PATH).then().statusCode(200).body(containsString("MWH.001"));
    given().when().get(PATH + "/store/1").then().statusCode(200).body(containsString("MWH.001"));

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().delete(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  void shouldReturnBadRequestWhenTheSameAssociationIsCreatedTwice() {
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body(request(2L, 2L, "MWH.012"))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    try {
      given()
          .contentType(ContentType.JSON)
          .body(request(2L, 2L, "MWH.012"))
          .when()
          .post(PATH)
          .then()
          .statusCode(400)
          .body("error", containsString("already fulfils"));
    } finally {
      given().when().delete(PATH + "/" + id).then().statusCode(204);
    }
  }

  @Test
  void shouldReturnNotFoundForAnUnknownStore() {
    given()
        .contentType(ContentType.JSON)
        .body(request(99999L, 1L, "MWH.001"))
        .when()
        .post(PATH)
        .then()
        .statusCode(404)
        .body("error", containsString("Store with id of 99999"));
  }

  @Test
  void shouldReturnNotFoundForAnUnknownWarehouse() {
    given()
        .contentType(ContentType.JSON)
        .body(request(1L, 1L, "NOPE.001"))
        .when()
        .post(PATH)
        .then()
        .statusCode(404)
        .body("error", containsString("NOPE.001"));
  }

  @Test
  void shouldReturnBadRequestWhenMandatoryFieldsAreMissing() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("storeId", 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("productId is required"));
  }

  @Test
  void shouldReturnNotFoundWhenListingAnUnknownStore() {
    given().when().get(PATH + "/store/99999").then().statusCode(404);
  }

  private Map<String, Object> request(Long storeId, Long productId, String businessUnitCode) {
    return Map.of(
        "storeId", storeId,
        "productId", productId,
        "warehouseBusinessUnitCode", businessUnitCode);
  }
}
