package com.celements.init;

import static org.junit.Assert.*;

import org.junit.Test;

public class DiskDocumentImporterStoreVersionTest {

  @Test
  public void acceptsOnlyMariaDb101() {
    assertTrue(DiskDocumentImporterStoreIT.isSupportedDatabaseVersion("10.1.48-MariaDB"));
    assertTrue(DiskDocumentImporterStoreIT.isSupportedDatabaseVersion("10.1.48-MariaDB-1~bionic"));
    for (var version : new String[] { null, "10.11.19-MariaDB", "10.10.1-MariaDB",
        "10.2.1-MariaDB", "10.1.48-MySQL", "10.1-MariaDB", " 10.1.48-MariaDB" }) {
      assertFalse("unsupported server version: " + version,
          DiskDocumentImporterStoreIT.isSupportedDatabaseVersion(version));
    }
  }
}
