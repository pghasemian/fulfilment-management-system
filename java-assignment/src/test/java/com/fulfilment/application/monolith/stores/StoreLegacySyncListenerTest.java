package com.fulfilment.application.monolith.stores;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies the core guarantee of task 2: the legacy system is notified only for changes that were
 * effectively committed to our database.
 */
@QuarkusTest
class StoreLegacySyncListenerTest {

  private static final String PATH = "store";

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @BeforeEach
  void resetMock() {
    reset(legacyStoreManagerGateway);
  }

  @Test
  void shouldNotifyLegacySystemAfterASuccessfulCreate() {
    given()
        .body("{\"name\":\"SYNC-CREATE\",\"quantityProductsInStock\":3}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(captor.capture());
    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());

    Store synced = captor.getValue();
    assertEquals("SYNC-CREATE", synced.name);
    assertEquals(3, synced.quantityProductsInStock);
    // the generated id proves the entity was already flushed when the legacy system was notified
    assertNotNull(synced.id);
  }

  @Test
  void shouldNotifyLegacySystemWithThePersistedStateOnUpdate() {
    Integer id =
        given()
            .body("{\"name\":\"SYNC-UPDATE\",\"quantityProductsInStock\":1}")
            .contentType("application/json")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    reset(legacyStoreManagerGateway);

    given()
        .body("{\"name\":\"SYNC-UPDATED\",\"quantityProductsInStock\":8}")
        .contentType("application/json")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200);

    ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(captor.capture());

    // the previous implementation forwarded the detached request payload, which carried no id
    assertEquals(id.longValue(), captor.getValue().id);
    assertEquals("SYNC-UPDATED", captor.getValue().name);
    assertEquals(8, captor.getValue().quantityProductsInStock);
  }

  @Test
  void shouldNotifyLegacySystemAfterAPatch() {
    Integer id =
        given()
            .body("{\"name\":\"SYNC-PATCH\",\"quantityProductsInStock\":1}")
            .contentType("application/json")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    reset(legacyStoreManagerGateway);

    given()
        .body("{\"quantityProductsInStock\":42}")
        .contentType("application/json")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200);

    ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(captor.capture());
    assertEquals(42, captor.getValue().quantityProductsInStock);
    assertEquals("SYNC-PATCH", captor.getValue().name);
  }

  @Test
  void shouldNotNotifyLegacySystemWhenTheTransactionIsRolledBack() {
    given()
        .body("{\"name\":\"DUPLICATED\",\"quantityProductsInStock\":1}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201);
    reset(legacyStoreManagerGateway);

    // the store name is unique, so this second insert fails at commit time and is rolled back
    given()
        .body("{\"name\":\"DUPLICATED\",\"quantityProductsInStock\":2}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(500);

    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any());
    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());
  }

  @Test
  void shouldNotNotifyLegacySystemWhenValidationRejectsTheRequest() {
    given()
        .body("{\"quantityProductsInStock\":2}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);

    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any());
  }

  @Test
  void shouldNotFailTheRequestWhenTheLegacySystemIsUnavailable() {
    doThrow(new IllegalStateException("legacy system down"))
        .when(legacyStoreManagerGateway)
        .createStoreOnLegacySystem(any());

    // our transaction is already committed, so the caller must still see a success
    given()
        .body("{\"name\":\"RESILIENT\",\"quantityProductsInStock\":1}")
        .contentType("application/json")
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    verify(legacyStoreManagerGateway, timeout(1000)).createStoreOnLegacySystem(any());

    // and the store remains readable, proving the commit was not undone
    given().when().get(PATH).then().statusCode(200).body(containsString("RESILIENT"));
  }
}
