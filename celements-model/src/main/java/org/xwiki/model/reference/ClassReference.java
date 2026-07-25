package org.xwiki.model.reference;

import static com.google.common.base.Preconditions.*;

import java.util.List;
import java.util.Optional;

import javax.annotation.concurrent.Immutable;

import org.xwiki.component.manager.ComponentLookupException;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.PseudoClassDefinition;
import com.celements.model.context.ModelContext;
import com.google.common.base.Splitter;
import com.xpn.xwiki.web.Utils;

@Immutable
public class ClassReference extends LocalDocumentReference implements ClassIdentity {

  private static final long serialVersionUID = 2L;

  public ClassReference(EntityReference reference) {
    super(reference);
  }

  public ClassReference(String spaceName, String className) {
    super(spaceName, className);
  }

  public ClassReference(String fullName) {
    this(extractPart(fullName, 0), extractPart(fullName, 1));
  }

  @Override
  public ClassReference clone() {
    return this;
  }

  @Override
  public ClassReference getClassReference() {
    return this;
  }

  @Override
  public Optional<ClassDefinition> getClassDefinition() {
    try {
      return Optional.of(Utils.getComponentManager().lookup(ClassDefinition.class, serialize()));
    } catch (ComponentLookupException exc) {
      return Optional.empty();
    }
  }

  @Override
  public DocumentReference getDocRef() {
    return getDocRef(getModelContext().getWikiRef());
  }

  @Override
  public boolean isValidObjectClass() {
    return !PseudoClassDefinition.CLASS_SPACE.equals(getSpaceName());
  }

  @Override
  public String serialize() {
    return getSpaceName() + "." + getName();
  }

  // is called in static context, do not use ModelUtils
  private static String extractPart(String fullName, int i) {
    List<String> parts = Splitter.on('.').omitEmptyStrings()
        .splitToList(fullName.substring(fullName.indexOf(':') + 1));
    checkArgument(parts.size() > i, "illegal class fullName [{0}]", fullName);
    return parts.get(i);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClassDefinition) {
      obj = ((ClassDefinition) obj).getClassReference();
    }
    return super.equals(obj);
  }

  private static ModelContext getModelContext() {
    return Utils.getComponent(ModelContext.class);
  }

}
