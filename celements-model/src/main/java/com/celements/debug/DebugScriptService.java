package com.celements.debug;

import java.lang.reflect.Executable;
import java.util.Iterator;
import java.util.stream.Stream;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingFunction;
import com.celements.common.lambda.LambdaExceptionUtil.ThrowingSupplier;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.store.DocumentCacheStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * IMPORTANT: For debugging purposes only, never use in productive scripts!
 * Use with caution, it may be a security concern or cause performance issues.
 */
@Component("debug")
public class DebugScriptService implements ScriptService {

  @Requirement
  private IRightsAccessFacadeRole rightsAccess;

  @Requirement
  private ComponentManager componentManager;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement(DocumentCacheStore.COMPONENT_NAME)
  private XWikiStoreInterface docCacheStore;

  @Requirement
  private ConfigurationSource configSource;

  public Object getField(Object obj, String fieldName) {
    return guard(() -> obj.getClass().getField(fieldName).get(obj));
  }

  public Class<?> getClass(String className) {
    return guard(() -> Class.forName(className));
  }

  public Object getService(String roleClassName, String hint) {
    return getService(getClass(roleClassName), hint);
  }

  public Object getService(Class<?> role, String hint) {
    return guard(() -> componentManager.lookup(role, hint));
  }

  public Object newInstance(String className, Object... args) {
    return newInstance(getClass(className), args);
  }

  public Object newInstance(Class<?> type, Object... args) {
    return guard(() -> tryExecuteAny(Stream.of(type.getConstructors()),
        constructor -> constructor.newInstance(args)));
  }

  public Object callStaticMethod(String className, String methodName, Object... args) {
    return callStaticMethod(getClass(className), methodName, args);
  }

  public Object callStaticMethod(Class<?> type, String methodName, Object... args) {
    return guard(() -> tryExecuteAny(Stream.of(type.getMethods())
        .filter(method -> method.getName().equals(methodName)),
        method -> method.invoke(null, args)));
  }

  private <E extends Executable> Object tryExecuteAny(Stream<E> executables,
      ThrowingFunction<E, ?, ReflectiveOperationException> invoker)
      throws ReflectiveOperationException {
    ReflectiveOperationException fail = null;
    for (Iterator<E> iter = executables.iterator(); iter.hasNext();) {
      try {
        return invoker.apply(iter.next());
      } catch (ReflectiveOperationException exc) {
        fail = exc;
      } catch (Exception exc) {
        fail = new ReflectiveOperationException(exc);
      }
    }
    throw (fail != null) ? fail : new NoSuchMethodException();
  }

  public boolean removeDocFromCache(DocumentReference docRef) {
    return guard(() -> removeDocFromCacheInternal(docRef));
  }

  public long removeDocsInSpaceFromCache(SpaceReference spaceRef) {
    return guard(() -> modelUtils.getAllDocsForSpace(spaceRef)
        .map(this::removeDocFromCacheInternal)
        .count());
  }

  public long removeDocsInWikiFromCache(WikiReference wikiRef) {
    return guard(() -> modelUtils.getAllSpaces(wikiRef)
        .flatMap(spaceRef -> modelUtils.getAllDocsForSpace(spaceRef))
        .map(this::removeDocFromCacheInternal)
        .count());
  }

  private boolean removeDocFromCacheInternal(DocumentReference docRef) {
    return getDocCache().remove(modelAccess.getOrCreateDocument(docRef));
  }

  public boolean clearDocCache() {
    return guard(() -> {
      getDocCache().clearCache();
      return true;
    });
  }

  private DocumentCacheStore getDocCache() {
    return ((DocumentCacheStore) docCacheStore);
  }

  private <T, E extends Exception> T guard(ThrowingSupplier<T, E> toGuard) {
    if (!rightsAccess.isSuperAdmin()) {
      throw new SecurityException("DebugScriptService not allowed");
    } else if (!isEnabled()) {
      throw new IllegalStateException("DebugScriptService not enabled");
    }
    try {
      return toGuard.get();
    } catch (Exception exc) {
      throw new IllegalArgumentException(exc);
    }
  }

  private boolean isEnabled() {
    return Boolean.parseBoolean(String.valueOf(configSource
        .getProperty("celements.debug.enabled", "false")));
  }

}
