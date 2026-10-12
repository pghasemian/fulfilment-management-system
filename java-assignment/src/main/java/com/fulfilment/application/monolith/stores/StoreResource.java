package com.fulfilment.application.monolith.stores;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  @Inject Event<StoreEvent.StoreCreated> storeCreatedEvent;

  @Inject Event<StoreEvent.StoreUpdated> storeUpdatedEvent;

  private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

  @GET
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }
    if (store.name == null || store.name.isBlank()) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }
    if (store.quantityProductsInStock == null) {
      store.quantityProductsInStock = 0;
    }

    store.persist();
    LOGGER.infof("Created store '%s' with id %d", store.name, store.id);

    // Only delivered once this transaction commits, so the legacy system never sees phantom data.
    storeCreatedEvent.fire(StoreEvent.StoreCreated.of(store));

    return Response.ok(store).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Store update(Long id, Store updatedStore) {
    if (updatedStore.name == null || updatedStore.name.isBlank()) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    entity.name = updatedStore.name;
    entity.quantityProductsInStock =
        updatedStore.quantityProductsInStock == null ? 0 : updatedStore.quantityProductsInStock;
    LOGGER.infof("Updated store with id %d", id);

    storeUpdatedEvent.fire(StoreEvent.StoreUpdated.of(entity));

    return entity;
  }

  @PATCH
  @Path("{id}")
  @Transactional
  public Store patch(Long id, Store updatedStore) {
    if (updatedStore.name == null && updatedStore.quantityProductsInStock == null) {
      throw new WebApplicationException("No property was set on request to be patched.", 422);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    // A PATCH is a partial update: only the properties present in the payload are applied.
    if (updatedStore.name != null) {
      if (updatedStore.name.isBlank()) {
        throw new WebApplicationException("Store Name cannot be blank.", 422);
      }
      entity.name = updatedStore.name;
    }

    if (updatedStore.quantityProductsInStock != null) {
      entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
    }
    LOGGER.infof("Patched store with id %d", id);

    storeUpdatedEvent.fire(StoreEvent.StoreUpdated.of(entity));

    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    entity.delete();
    LOGGER.infof("Deleted store with id %d", id);
    return Response.status(204).build();
  }
}
