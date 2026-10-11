package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@Cacheable
public class Store extends PanacheEntity {

  @Column(length = 40, unique = true)
  public String name;

  /**
   * Wrapper type without a default on purpose: it allows a PATCH request to distinguish "property
   * not provided" (null) from an explicit zero. Defaulting happens in the resource layer.
   */
  public Integer quantityProductsInStock;

  public Store() {

  }

  public Store(String name) {
    this.name = name;
  }
}
