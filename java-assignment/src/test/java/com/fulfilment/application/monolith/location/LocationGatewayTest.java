package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LocationGatewayTest {

  private LocationGateway locationGateway;

  @BeforeEach
  void setUp() {
    locationGateway = new LocationGateway();
  }

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    // given
    String identifier = "ZWOLLE-001";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testWhenResolveAmsterdamLocationShouldReturn() {
    // given
    String identifier = "AMSTERDAM-001";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("AMSTERDAM-001", location.identification);
    assertEquals(5, location.maxNumberOfWarehouses);
    assertEquals(100, location.maxCapacity);
  }

  @Test
  public void testWhenResolveAmsterdam002LocationShouldReturn() {
    // given
    String identifier = "AMSTERDAM-002";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("AMSTERDAM-002", location.identification);
    assertEquals(3, location.maxNumberOfWarehouses);
    assertEquals(75, location.maxCapacity);
  }

  @Test
  public void testWhenResolveZwolle002LocationShouldReturn() {
    // given
    String identifier = "ZWOLLE-002";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("ZWOLLE-002", location.identification);
    assertEquals(2, location.maxNumberOfWarehouses);
    assertEquals(50, location.maxCapacity);
  }

  @Test
  public void testWhenResolveTilburgLocationShouldReturn() {
    // given
    String identifier = "TILBURG-001";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("TILBURG-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testWhenResolveHelmondLocationShouldReturn() {
    // given
    String identifier = "HELMOND-001";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("HELMOND-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(45, location.maxCapacity);
  }

  @Test
  public void testWhenResolveEindhovenLocationShouldReturn() {
    // given
    String identifier = "EINDHOVEN-001";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("EINDHOVEN-001", location.identification);
    assertEquals(2, location.maxNumberOfWarehouses);
    assertEquals(70, location.maxCapacity);
  }

  @Test
  public void testWhenResolveVetsbyLocationShouldReturn() {
    // given
    String identifier = "VETSBY-001";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("VETSBY-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(90, location.maxCapacity);
  }

  @Test
  public void testWhenResolveLowerCaseIdentifierShouldReturn() {
    // given
    String identifier = "zwolle-001";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
  }

  @Test
  public void testWhenResolveIdentifierWithWhitespaceShouldReturn() {
    // given
    String identifier = "  ZWOLLE-001  ";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
  }

  @Test
  public void testWhenResolveNonExistingLocationShouldReturnNull() {
    // given
    String identifier = "NONEXISTENT-001";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNull(location);
  }

  @Test
  public void testWhenResolveNullIdentifierShouldReturnNull() {
    // given
    String identifier = null;

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNull(location);
  }

  @Test
  public void testWhenResolveEmptyIdentifierShouldReturnNull() {
    // given
    String identifier = "";

    // when
    Location location = locationGateway.resolveByIdentifier(identifier);

    // then
    assertNull(location);
  }
}
