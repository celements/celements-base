package org.xwiki.model.reference;

import javax.annotation.concurrent.Immutable;

import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.xpn.xwiki.web.Utils;

/**
 * @deprecated since 5.4, DocumentReference is inherently immutable
 */
@Deprecated
@Immutable
public class ImmutableDocumentReference extends DocumentReference implements ImmutableReference {

  private static final long serialVersionUID = 4196990820112451663L;

  public ImmutableDocumentReference(EntityReference reference) {
    super(reference);
  }

  public ImmutableDocumentReference(String wikiName, String spaceName, String docName) {
    super(wikiName, spaceName, docName);
  }

  public ImmutableDocumentReference(String docName, SpaceReference parent) {
    super(docName, parent);
  }

  @Deprecated
  @Override
  public EntityReference getMutable() {
    return new EntityReference(getName(), getType(), getParent());
  }

  public String serialize() {
    return serialize(ReferenceSerializationMode.COMPACT_WIKI);
  }

  public String serialize(ReferenceSerializationMode mode) {
    return getModelUtils().serializeRef(this, mode);
  }

  private static final ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
