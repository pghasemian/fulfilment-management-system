# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Today the code base mixes three strategies:

  - Store: active record (PanacheEntity), queried statically from the REST resource.
  - Product: repository (PanacheRepository) injected into the REST resource, but the
    JPA entity is still returned directly as the API payload.
  - Warehouse: hexagonal. A WarehouseStore port, a WarehouseRepository adapter, a
    DbWarehouse persistence entity and a separate Warehouse domain model.

I would standardise on the Warehouse approach, and I did so for everything I touched.
The reason is not purity, it is testability and blast radius.

With active record the persistence call is a static method on the entity, so the surrounding
logic is harder to test independently. With a port, the business rules can be unit tested
against a mock without booting Quarkus or a database. This keeps tests fast and focused on
business logic.

The second reason is coupling. Store and Product expose the JPA entity as the API
contract, so a column or field change can become an unannounced breaking change for every
client. The Warehouse slice separates the wire bean, the domain model and the database row,
which lets me implement archiving as a soft delete (archivedAt) without clients needing to
know that rows are no longer physically removed.

I would not do a big-bang rewrite. Active record is genuinely faster to write and for a
truly CRUD-only resource it is defensible. My order would be:

  1. Stop leaking entities over the API: introduce DTOs for Store and Product first.
     This buys real freedom and is relatively mechanical.
  2. Move Store's persistence calls behind a repository, so the resource stops making
     static calls and the service layer becomes unit testable.
  3. Only introduce a domain model for Store/Product once they grow real invariants.
     Product has none today, so a port there would be ceremony without payoff.

One concrete defect this mix already caused, which I fixed: StoreResource.update and
.patch forwarded the detached request payload to the legacy gateway instead of the
managed entity, so the legacy system received an object with a null id and whatever the
client happened to send. With an explicit mapping step, that class of bug is harder to
write because the "what we persisted" and "what was requested" objects stop being
interchangeable.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Spec-first (Warehouse)

  + The contract is reviewable and diffable on its own, before any code exists, and
    consumers can generate clients and start working in parallel.
  + The spec cannot silently drift from the handlers: the compiler fails when they
    disagree. Implementing WarehouseResourceImpl, the generated interface forced me to
    handle every operation declared in the yaml, including the ones I might have
    postponed.
  + It makes the contract a deliberate decision instead of a by-product of a refactor.
  - You inherit the generator's opinions. A concrete example from this assignment: the
    generated interface returns the Warehouse bean instead of a Response, which pins the
    success status of POST /warehouse to 200 even though the spec declares 201, and the
    interface is generated so it cannot be annotated. I had to add a small
    ContainerResponseFilter to honour our own specification.
  - The generated beans are anaemic and need mapping code to and from the domain model.
  - A generator cannot warn you about an ambiguous contract. GET /warehouse/{id} does not
    say whether {id} is the technical id or the business unit code, while the sibling
    endpoint keys on businessUnitCode. That ambiguity had to be resolved by a human.

Code-first (Product, Store)

  + Fastest path for a small or internal endpoint: no build plumbing, no mapping layer.
  + Full control over status codes, headers and validation.
  - The published contract is whatever the code happens to serialise, which is how JPA
    entities ended up as the public payload of Store and Product.
  - Changes are silent. Making Store.quantityProductsInStock an Integer, which I needed
    for correct PATCH semantics, is a visible contract change that nothing flagged.

My choice: spec-first for anything that crosses a team or company boundary, which is the
Warehouse API and would also be the Store API, since a legacy system consumes it.
Code-first stays fine for internal or admin endpoints. What I would not keep is the
current split being accidental. Whichever is chosen should be consistent and enforced.
If code-first is kept for Product, I would at least generate the spec from the code with
quarkus-smallrye-openapi (which I enabled, so /q/openapi now publishes the whole API) and
commit the result, so that a contract change still shows up in a pull request diff.

```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I prioritised by risk and by cost per test. In the reported test run, all 109 tests passed,
with 0 failures and 0 skipped tests.

1. Use case unit tests first (25 tests on the warehouse domain, plain JUnit + Mockito).
   This is where the value is: every rule from the assignment, tested at its boundary.
   Not only "capacity too large", but also the warehouse that exactly fills the remaining
   capacity of a location, and the replacement whose capacity exactly equals the stock
   being carried over. Off-by-one errors live on those boundaries. These tests need no
   database, so they stay fast enough to run on every save.

2. Endpoint tests against a real (in memory) database, roughly 50 tests.
   They cover what mocks cannot: serialisation, status codes, transaction boundaries and
   the exception mapper. The most valuable one here is StoreLegacySyncListenerTest, which
   asserts the actual requirement of task 2 by forcing a unique-constraint violation and
   verifying that the legacy gateway is never called. A unit test could not prove that,
   because the guarantee is a property of the transaction, not of a code path.

3. A thin integration test (@QuarkusIntegrationTest) against the packaged artifact on a
   real PostgreSQL in CI: list, create, replace, archive. Its job is to catch packaging,
   wiring and dialect problems, not business rules, so it deliberately stays small.

What I deliberately did not write: tests for getters, for the generated OpenAPI beans
(excluded from the coverage analysis) or one test per branch of a trivial mapper. They
inflate the coverage number without reducing risk, and they are a common reason suites rot.

Keeping it effective over time:

  - The JaCoCo HTML coverage report is available at target/site/jacoco/index.html after
    generating the report. I use it to review coverage at package and class level, identify
    gaps, and focus on important business logic. Coverage should help identify missing tests
    rather than become a target to optimise.
  - Coverage alone is a weak signal, so each test asserts a specific error message or
    state transition. A test that stops testing anything tends to fail rather than pass
    vacuously. If the project grew I would add mutation testing (Pitest) on the domain
    package only, since that measures whether the assertions are real.
  - Tests are named after the rule they protect (shouldRejectWhenTheStockDoesNotMatch...),
    so a failure names the broken business rule rather than a method.
  - Keep the fast layer fast. The moment the unit tests need a container, people stop
    running them locally and the feedback loop collapses. That is also why I configured H2
    for dev and test: the whole suite runs without a Docker daemon.


```