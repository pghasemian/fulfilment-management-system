package com.fulfilment.application.monolith.health;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class HealthCheckTest {

  @Test
  void shouldReportTheApplicationAsReady() {
    given()
        .when()
        .get("/q/health/ready")
        .then()
        .statusCode(200)
        .body("status", is("UP"))
        .body("checks.find { it.name == 'warehouse-store' }.status", is("UP"));
  }

  @Test
  void shouldExposeLivenessAndTheOverallHealth() {
    given().when().get("/q/health/live").then().statusCode(200).body("status", is("UP"));
    given().when().get("/q/health").then().statusCode(200).body("status", is("UP"));
  }
}
