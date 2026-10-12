package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simulates the integration with the legacy system that mirrors our {@link Store} register.
 *
 * <p>The "remote call" is emulated by writing a temporary file. Callers must invoke this gateway
 * only after the local transaction has been committed - see {@link StoreLegacySyncListener}.
 */
@ApplicationScoped
public class LegacyStoreManagerGateway {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreManagerGateway.class);

  public void createStoreOnLegacySystem(Store store) {
    writeToFile("Store created", store);
  }

  public void updateStoreOnLegacySystem(Store store) {
    writeToFile("Store updated", store);
  }

  private void writeToFile(String operation, Store store) {
    Path tempFile = null;
    try {
      tempFile = Files.createTempFile(store.name, ".txt");

      String content =
          operation
              + ". [ name ="
              + store.name
              + " ] [ items on stock ="
              + store.quantityProductsInStock
              + "]";
      Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));

      LOGGER.debugf("Synchronised store '%s' with the legacy system: %s", store.name, content);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Could not synchronise store '" + store.name + "' with the legacy system", e);
    } finally {
      deleteQuietly(tempFile);
    }
  }

  private void deleteQuietly(Path tempFile) {
    if (tempFile == null) {
      return;
    }
    try {
      Files.deleteIfExists(tempFile);
    } catch (IOException e) {
      LOGGER.warnf(e, "Could not delete temporary file %s", tempFile);
    }
  }
}
