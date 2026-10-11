package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
