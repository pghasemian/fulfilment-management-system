package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory implementation of the {@link LocationResolver} port.
 *
 * <p>The catalogue of locations is static reference data in this simplified system. It is indexed
 * by identifier so that lookups are constant time, and exposed as immutable collections so that it
 * cannot be mutated by callers.
 */
@ApplicationScoped
public class LocationGateway implements LocationResolver {

  private static final List<Location> LOCATIONS =
          List.of(
                  new Location("ZWOLLE-001", 1, 40),
                  new Location("ZWOLLE-002", 2, 50),
                  new Location("AMSTERDAM-001", 5, 100),
                  new Location("AMSTERDAM-002", 3, 75),
                  new Location("TILBURG-001", 1, 40),
                  new Location("HELMOND-001", 1, 45),
                  new Location("EINDHOVEN-001", 2, 70),
                  new Location("VETSBY-001", 1, 90));

  private static final Map<String, Location> LOCATIONS_BY_IDENTIFIER =
          LOCATIONS.stream()
                  .collect(Collectors.toUnmodifiableMap(l -> l.identification, Function.identity()));

  /**
   * Resolves a location by its identifier.
   *
   * @param identifier the location identification, e.g. {@code ZWOLLE-001}. Matching ignores
   *     surrounding whitespace and casing.
   * @return the matching {@link Location}, or {@code null} when no location matches. Translating
   *     the absence into a business error is the responsibility of the domain layer.
   */
  @Override
  public Location resolveByIdentifier(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      return null;
    }

    return LOCATIONS_BY_IDENTIFIER.get(identifier.trim().toUpperCase());
  }
}