package org.xwiki.model.reference;

import static com.google.common.base.Preconditions.*;

import java.util.List;
import java.util.Optional;

import javax.annotation.concurrent.Immutable;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.PseudoClassDefinition;
import com.celements.model.context.ModelContext;
import com.google.common.base.Splitter;
import com.xpn.xwiki.web.Utils;

@Immutable
public class ClassReference extends EntityReference implements ImmutableReference, ClassIdentity {

  private static final long serialVersionUID = 2L;

  public ClassReference(EntityReference reference) {
    super(reference.getName(), reference.getType(), reference.getParent());
  }

  public ClassReference(String spaceName, String className) {
    super(className, EntityType.DOCUMENT, new EntityReference(spaceName, EntityType.SPACE));
  }

  public ClassReference(String fullName) {
    this(extractPart(fullName, 0), extractPart(fullName, 1));
  }

  @Override
  protected void setParent(EntityReference parent) {
    checkArgument((parent != null) && (parent.getType() == EntityType.SPACE),
        "Invalid parent reference [" + parent + "] for a class reference");
    super.setParent(new EntityReference(parent.getName(), EntityType.SPACE));
  }

  private String getParentName() {
    return super.getParent().getName();
  }

  @Override
  protected void setType(EntityType type) {
    checkArgument(type == EntityType.DOCUMENT, "Invalid type [" + type + "] for a class reference");
    super.setType(EntityType.DOCUMENT);
  }

  @Override
  public ClassReference clone() {
    return this;
  }

  @Deprecated
  @Override
  public EntityReference getMutable() {
    return new EntityReference(getName(), getType(), getParent());
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
  public DocumentReference getDocRef(WikiReference wikiRef) {
    return new DocumentReference(getName(), new SpaceReference(getParentName(), wikiRef));
  }

  @Override
  public boolean isValidObjectClass() {
    return !PseudoClassDefinition.CLASS_SPACE.equals(getParentName());
  }

  @Override
  public String serialize() {
    return getParentName() + "." + getName();
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
