package com.celements.store;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import com.celements.health.AsyncHealthCheck;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Reads through the Hibernate store without tenant context. Connection acquisition and socket
 * waits retain the provider/driver's configured timeouts.
 */
@Component
public class DatabaseHealthCheck extends AsyncHealthCheck {

  private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(1);

  private final XWikiHibernateBaseStore store;

  @Inject
  public DatabaseHealthCheck(@Named("hibernate") @NotNull XWikiStoreInterface store) {
    this.store = (XWikiHibernateBaseStore) store;
  }

  @Override
  @NotNull
  public String id() {
    return "database";
  }

  @Override
  @NotNull
  public Scope scope() {
    return Scope.READINESS;
  }

  @Override
  @NotNull
  protected CompletionStage<@NotNull Result> runCheck() {
    return CompletableFuture.completedFuture(checkDatabase());
  }

  @NotNull
  private Result checkDatabase() {
    try {
      store.checkConnection(QUERY_TIMEOUT);
      return new Result(Status.UP, "Database check succeeded");
    } catch (RuntimeException failure) {
      return new Result(Status.DOWN, "Database check failed: " + failure.getClass().getSimpleName());
    }
  }

}
