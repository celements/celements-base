package org.xwiki.model.reference;

import static com.google.common.base.Preconditions.*;
import static java.util.Objects.*;

import org.xwiki.model.EntityType;

public class LocalDocumentReference extends EntityReference {

  private static final long serialVersionUID = 1L;

  public LocalDocumentReference(EntityReference reference) {
    this(getSpaceName(reference), reference.getName());
  }

  public LocalDocumentReference(String spaceName, String documentName) {
    super(documentName, EntityType.DOCUMENT, new EntityReference(spaceName, EntityType.SPACE));
  }

  public String getSpaceName() {
    return getParent().getName();
  }

  public String getDocumentName() {
    return getName();
  }

  public DocumentReference getDocRef(WikiReference wikiRef) {
    var spaceRef = new SpaceReference(getSpaceName(), wikiRef);
    return new DocumentReference(getDocumentName(), spaceRef);
  }

  @Override
  protected void setParent(EntityReference parent) {
    checkArgument((parent != null) && (parent.getType() == EntityType.SPACE),
        "Invalid parent reference [%s] for a local document reference", parent);
    super.setParent(new EntityReference(parent.getName(), EntityType.SPACE));
  }

  @Override
  protected void setType(EntityType type) {
    checkArgument(type == EntityType.DOCUMENT,
        "Invalid type [%s] for a local document reference", type);
    super.setType(EntityType.DOCUMENT);
  }

  private static String getSpaceName(EntityReference reference) {
    requireNonNull(reference);
    checkArgument(reference.getType() == EntityType.DOCUMENT,
        "Invalid type [%s] for a local document reference", reference.getType());
    EntityReference parent = reference.getParent();
    checkArgument((parent != null) && (parent.getType() == EntityType.SPACE),
        "Invalid parent reference [%s] for a local document reference", parent);
    return parent.getName();
  }
}
