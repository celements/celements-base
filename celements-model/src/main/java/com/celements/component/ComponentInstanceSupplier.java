package com.celements.component;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.component.manager.ComponentLookupException;

import com.google.common.base.Supplier;
import com.xpn.xwiki.web.Utils;

public class ComponentInstanceSupplier<T> implements Supplier<T> {

  private final Class<T> role;
  private final String hint;

  public ComponentInstanceSupplier(@NotNull Class<T> role) {
    this(role, null);
  }

  public ComponentInstanceSupplier(@NotNull Class<T> role, @Nullable String hint) {
    this.role = checkNotNull(role);
    checkArgument(role.isAnnotationPresent(ComponentRole.class),
        "ComponentRole annotation required for {}", role);
    this.hint = firstNonNull(emptyToNull(hint), "default");
    checkState(Utils.getComponentManager() != null);
  }

  @Override
  public T get() {
    try {
      return Utils.getComponentManager().lookup(role, hint);
    } catch (ComponentLookupException exc) {
      throw new IllegalArgumentException(exc);
    }
  }

  public <S extends T> S getAndCast(Class<S> type) throws ClassCastException {
    return type.cast(get());
  }

}
