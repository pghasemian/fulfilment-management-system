# Implementation notes

This document explains what was implemented, the decisions and trade-offs behind it, and
how to run and verify everything.

## How to run

```bash
cd java-assignment

./mvnw quarkus:dev            # dev mode on http://localhost:8080 (in memory H2, no Docker needed)
./mvnw verify                 # compile, run all tests, enforce the 80% coverage gate
./mvnw verify -DskipITs=false # also run the @QuarkusIntegrationTest against PostgreSQL
docker compose up -d          # PostgreSQL on port 15432, required by the prod profile and the ITs
```

Useful endpoints:

| Endpoint | Purpose |
| --- | --- |
| `/warehouse` | Warehouse API (generated from `warehouse-openapi.yaml`) |
| `/fulfilment` | fulfilment associations (bonus task) |
| `/store`, `/product` | pre-existing APIs |
| `/q/health`, `/q/health/ready`, `/q/health/live` | health checks |
| `/q/openapi`, `/q/swagger-ui` | published API contract |

Coverage report: `target/jacoco-report/index.html`.

## Results

- **109 tests**, all passing.
- **93.0% line coverage** (426/458), against a build-breaking gate of 80%.
- Generated OpenAPI classes (`com.warehouse.api.**`) are excluded from the analysis, since
  they are not hand-written code.

| Package | Coverage |
| --- | --- |
| `warehouses.domain.usecases` | 88.5% |
| `warehouses.adapters.restapi` | 97.4% |
| `warehouses.adapters.database` | 84.5% |
| `warehouses.domain.models` / `.exceptions` | 100% |
| `fulfilment` | 100% |
| `stores` | 92.7% |
| `products` | 100% |
| `location` | 88.9% |
| `exceptions` | 100% |

## Task 1 - Location

`LocationGateway.resolveByIdentifier` is implemented as a constant-time lookup over an
immutable map built once at class initialisation.

- The class was **not a CDI bean**; it had no scope annotation, so injecting the
  `LocationResolver` port would have failed. It is now `@ApplicationScoped`.
- The mutable `ArrayList` populated in a static block was replaced by immutable collections.
- **Contract decision:** an unknown identifier returns `null` rather than throwing. The
  domain layer decides what absence means, which keeps the gateway a pure lookup. The
  use case turns it into a validation error.
- Lookup tolerates casing and surrounding whitespace.

## Task 2 - Store, commit-then-notify

The requirement is that the legacy system is only told about changes that were actually
committed. The original code called `LegacyStoreManagerGateway` *inside* the
`@Transactional` method, so a failure at commit time (for example the unique constraint on
`Store.name`) left the legacy system holding data that does not exist on our side.

**Solution: CDI transactional events.** `StoreResource` fires `StoreEvent.StoreCreated` /
`StoreEvent.StoreUpdated`, and `StoreLegacySyncListener` observes them with
`@Observes(during = TransactionPhase.AFTER_SUCCESS)`. The container only delivers the event
once the JTA transaction has committed.

Why this over the alternatives:

- It keeps the resource free of synchronisation concerns and makes the guarantee
  declarative rather than a matter of statement ordering.
- Adding more downstream consumers later requires no change to the resource.
- Compared to "call the gateway after the transactional method returns", it cannot be
  broken by someone later wrapping the caller in another transaction.

Supporting decisions:

- **The event carries a detached snapshot**, not the managed entity, so the observer never
  touches a closed persistence context after the transaction ends.
- **Failures of the legacy call are logged and swallowed.** Our transaction is already
  committed and the user's request succeeded; failing the response would be a lie. This is
  verified by a test that makes the gateway throw and asserts the request still returns 201
  and the store is readable.
- **Known limitation, deliberately not solved:** if the JVM dies between the commit and the
  notification, the notification is lost. The production answer is the transactional outbox
  pattern (persist the intent in the same transaction, deliver it with a retrying poller).
  That is over-engineering for a gateway that writes a temp file, but it is the right next
  step and is discussed in the case study.

Bugs found and fixed in the same class, because they were directly coupled to this change:

- `update` and `patch` passed the **detached request payload** to the legacy gateway instead
  of the persisted entity, so the legacy system received an object with a `null` id. A
  regression test now asserts the synchronised object carries the persisted id.
- `patch` had inverted logic: it checked `entity.name != null` and
  `entity.quantityProductsInStock != 0` (properties of the *existing* row) to decide whether
  to apply the *incoming* values, and it rejected any payload without a name, which defeats
  the purpose of PATCH. It now applies only the properties present in the request.
- To make partial updates expressible at all, `Store.quantityProductsInStock` became an
  `Integer`. With a primitive, "not provided" and "explicitly zero" are indistinguishable.
  A default value on the field would also defeat it, so defaulting happens in the resource.
- `LegacyStoreManagerGateway` used `System.out.println` and `printStackTrace`, and leaked
  the temp file whenever an exception occurred before the delete. It now uses the JBoss
  logger and cleans up in a `finally` block.

## Task 3 - Warehouse

### Architecture

The existing hexagonal structure was kept and completed: REST adapter -> use case -> port ->
persistence adapter. The domain layer has no JAX-RS or JPA dependency.

### Persistence (`WarehouseRepository`)

- **Archiving is a soft delete.** `archivedAt` is stamped and the row is kept. Every lookup
  filters on `archivedAt is null`. This is what allows the business unit code to be reused by
  a replacement while the history of the previous unit is preserved, which is exactly the
  concern raised by scenario 5 of the case study.
- `getAll()` returns only active units, which is also what the location-capacity rules need.
- `create` stamps `createdAt` and propagates the generated id back to the domain object, so
  the REST layer can return it.
- The port method is named `getById` rather than `findById` because `PanacheRepository`
  already declares `findById(Long)` with a different return type, which would not compile.

### Business rules

`CreateWarehouseUseCase`:

1. Mandatory fields: business unit code, location, capacity > 0, stock >= 0.
2. Stock must not exceed the warehouse's own capacity.
3. The business unit code must not already be held by an **active** warehouse.
4. The location must exist in the catalogue.
5. The number of active warehouses at the location must be below `maxNumberOfWarehouses`.
6. The sum of active capacities at the location, plus the new one, must not exceed
   `maxCapacity`.

`ArchiveWarehouseUseCase`: resolves the **persisted** state by business unit code (rather
than trusting the caller's object), rejects unknown or already-archived units, stamps
`archivedAt`.

`ReplaceWarehouseUseCase`: validates the payload, loads the warehouse being replaced (404 if
there is no active one), then checks the two replacement-specific rules - the new capacity
must accommodate the previous stock, and the stock must match - and finally **archives the
old unit before creating the new one**.

**Why archive-then-create is the right order:** archiving frees both the business unit code
and the location slot, so the replacement then passes through the ordinary creation rules
unchanged, including the location capacity check which now correctly excludes the outgoing
warehouse. The alternative (special-casing the exclusion inside the creation rules) would
duplicate the logic and drift. The whole operation is `@Transactional`, so a rejected
creation rolls the archiving back; there is a test asserting that the original warehouse is
still active after a rejected replacement.

**An observation worth recording:** the "capacity must accommodate the previous stock" rule
is, strictly, implied by the combination of "stock must match the previous stock" and "stock
must not exceed capacity". It is kept as an explicit, separately-tested check because it is
a named business rule in the assignment and deserves its own error message. The test for it
uses an internally-consistent payload, which is the only way the rule is reachable.

### REST adapter

- **`{id}` is the technical database id.** The specification is ambiguous (the sibling
  replacement endpoint keys on `businessUnitCode`), and the existing integration test
  archives `/warehouse/1`, so the numeric id is the intended reading. A non-numeric id
  returns 400 with an explicit message.
- **Status codes follow the specification**: 201 on create, 200 on replace, 204 on archive,
  404 for a missing unit, 400 for a business rule violation. Business rule violations map to
  400 rather than 409 because 400 is what the spec documents for these endpoints; spec-first
  means honouring the spec even where 409 would be more idiomatic.
- **`WarehouseCreatedStatusFilter`**: the generated `WarehouseResource` interface is the
  JAX-RS resource and returns the bean rather than a `Response`, which pins POST to 200.
  Annotations on the implementation are ignored, and the interface is generated so it cannot
  be annotated. A narrowly scoped `ContainerResponseFilter` restores the 201 declared in the
  yaml. This is documented in the class itself, as it is otherwise surprising.
- **`RestExceptionMapper`** replaces the two *identical* `ErrorMapper` classes that were
  nested in `ProductResource` and `StoreResource`. Both were registered as
  `@Provider ExceptionMapper<Exception>`, so which one handled a given failure was
  undefined. The single mapper preserves the original JSON shape, translates domain
  exceptions, and logs 5xx with a stack trace but 4xx without, since caller errors are not
  system faults.

## Bonus task - fulfilment associations

`POST /fulfilment` associates a warehouse as a fulfilment unit of a product for a store;
`GET /fulfilment`, `GET /fulfilment/store/{storeId}` and `DELETE /fulfilment/{id}` complete
the resource. The code lives in `com.fulfilment.application.monolith.fulfilment`.

**Model.** `FulfilmentAssociation` is a join entity over the `(store, product, warehouse)`
triple rather than a pair of many-to-many relations, because the association *is* the
business concept the constraints are expressed in, and it gives each link an identity that
can be deleted and audited (`createdAt`). The triple carries a unique database constraint:
it is the last line of defence behind the explicit checks, and it closes the race where two
concurrent requests both pass validation.

**The three constraints** are enforced in `FulfilmentService`:

| Rule | Check |
| --- | --- |
| ≤ 2 warehouses per product per store | `countWarehousesFulfilling(store, product)` |
| ≤ 3 warehouses per store | `countDistinctWarehousesOfStore(store)` |
| ≤ 5 product types per warehouse | `countDistinctProductsOfWarehouse(warehouse)` |

The subtlety is that limits 2 and 3 count **distinct** participants, so they must not be
applied when the new row does not actually add one. Associating a fourth product with a
warehouse that already serves the store consumes no new warehouse slot for that store, and
associating a sixth store with a product the warehouse already stocks consumes no new
product-type slot. Both of those are covered by their own tests
(`shouldNotConsumeANewStoreSlotWhenTheWarehouseAlreadyFulfilsThatStore` and its product-side
counterpart); a naive `count(*) >= limit` implementation passes the violation tests and
fails these.

**Other decisions.**

- The warehouse is addressed by **business unit code**, not by surrogate id: that is the
  identifier the business uses, and it is what the rest of the warehouse API exposes.
- Only **active** warehouses can be associated. Lookups go through
  `findByBusinessUnitCode`, which filters out archived rows, so an archived warehouse is
  reported as not found rather than silently accepted.
- Reads return a `FulfilmentView` record rather than the entity. Serialising the entity
  would touch its lazy associations outside the persistence context; the view is built
  inside the transaction.
- Counts are evaluated per request rather than maintained as denormalised counters. At this
  cardinality (≤ 5 rows per warehouse) the queries are trivially cheap, and a cached counter
  would be a correctness risk for no measurable gain.

## Cross-cutting work

**Build (`pom.xml`)**

- The compiler was configured with `source/target 11` while the project declares
  `maven.compiler.release=17`. Removed the stale override.
- `maven-failsafe-plugin` existed **only in the `native` profile**, so `*IT` tests never ran
  in a normal build. It is now in the default build.
- JaCoCo with a build-breaking 80% line gate. **The plain `jacoco-maven-plugin` agent does
  not work here**: `@QuarkusTest` classes are bytecode-enhanced and loaded by the Quarkus
  classloader, which made whole packages report 0% despite being fully exercised (total was
  47%). Switching to the `quarkus-jacoco` extension, with the Maven plugin kept only for the
  `check` goal reading `jacoco-quarkus.exec`, reports the real figure of 93%.
- Added `hibernate-validator`, `smallrye-health`, `smallrye-openapi` and the Mockito test
  support.

**Running without Docker.** The project only declared a PostgreSQL datasource, so every
`@QuarkusTest` depended on Quarkus Dev Services starting a container; on a machine without a
Docker daemon the suite could not run at all. H2 is now configured for the `dev` and `test`
profiles, with PostgreSQL untouched for `prod`. The H2 dependency is **`provided`** scope so
it is available to dev mode and tests but excluded from the packaged production artifact.

**Integration tests.** `@QuarkusIntegrationTest` runs the packaged application in the `prod`
profile, which genuinely requires PostgreSQL. They are skipped by default (`-DskipITs=true`)
so that `./mvnw verify` works anywhere, and CI runs them with `-DskipITs=false` against a
PostgreSQL service container. `docker-compose.yml` provides the same database locally. The
previously commented-out test body is now active and extended with a create/replace cycle.

**Health checks.** `/q/health/ready` includes a custom readiness probe that reads the
warehouse register, exercising the datasource and the mapping layer end to end rather than
merely reporting that the process is alive.

**CI** (`.github/workflows/ci.yml`): JDK 17, `./mvnw verify -DskipITs=false` with a
PostgreSQL service, JaCoCo report published as an artifact, test reports published on
failure, and a second job building the JVM container image.

**Logging and error handling.** JBoss `Logger` throughout, no `System.out` or
`printStackTrace` left in the code paths touched. State changes are logged at INFO with the
identifiers needed to trace them; rejected business rules are logged at WARN without a stack
trace.

## Testing approach

| Layer | Tests | What it protects |
| --- | --- | --- |
| Warehouse use cases (plain JUnit + Mockito) | 25 | every business rule, at its boundary |
| Location gateway (plain JUnit) | 9 | lookup contract, including null and blank input |
| Fulfilment rules (`@QuarkusTest` + `@TestTransaction`) | 20 | the 2/3/5 constraints, at and over the limit |
| Endpoints (`@QuarkusTest`) | 53 | status codes, serialisation, transactions, mapper |
| Health (`@QuarkusTest`) | 2 | readiness wiring |
| Packaged app (`@QuarkusIntegrationTest`, CI only) | 3 | packaging, wiring, real PostgreSQL |

Boundary cases are covered explicitly, not only the failures: a warehouse that exactly fills
the remaining capacity of a location is accepted, and a replacement whose capacity exactly
equals the stock carried over is accepted. Off-by-one errors live on those boundaries.

The most important single test is `StoreLegacySyncListenerTest.shouldNotNotifyLegacySystem
WhenTheTransactionIsRolledBack`, which proves the requirement of task 2 by forcing a real
unique-constraint violation and asserting the gateway was never called. A mock-only test
could not prove it, because the guarantee is a property of the transaction rather than of a
code path.

## Things I noticed but deliberately did not change

- **`import.sql` violates the location constraints it seeds.** `MWH.001` has capacity 100 at
  `ZWOLLE-001`, whose `maxCapacity` is 40, and `MWH.023` has 30 at `TILBURG-001` whose max is
  also 40 with `maxNumberOfWarehouses = 1`. The rules are enforced on write, so existing rows
  are grandfathered in. Changing the fixture would have altered the data the provided
  integration test asserts on. In a real system this needs a decision: either a migration that
  reconciles the data, or an explicit "legacy, exempt" flag.
- **`DELETE /store/{id}` does not notify the legacy system**, since
  `LegacyStoreManagerGateway` exposes no delete operation. This is an inconsistency in the
  synchronisation, but adding a method to the simulated legacy contract was outside the task.
