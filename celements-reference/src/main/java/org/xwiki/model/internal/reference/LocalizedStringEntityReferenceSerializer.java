package org.xwiki.model.internal.reference;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Extends the default representation with the locale of document references.
 */
public class LocalizedStringEntityReferenceSerializer
    extends DefaultStringEntityReferenceSerializer {

  @Override
  protected void serializeEntityReference(EntityReference currentReference,
      StringBuilder representation, boolean isLastReference, Object... parameters) {
    super.serializeEntityReference(currentReference, representation, isLastReference, parameters);
    if (currentReference instanceof DocumentReference docRef) {
      docRef.getLocale().ifPresent(x -> representation.append('(').append(x).append(')'));
    }
  }

}
