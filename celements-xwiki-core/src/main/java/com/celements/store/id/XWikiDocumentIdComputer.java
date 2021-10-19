package com.celements.store.id;

import java.util.Arrays;
import java.util.Iterator;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

@Component("xwiki")
public class XWikiDocumentIdComputer implements DocumentIdComputer {

  @Requirement("local")
  private EntityReferenceSerializer<String> localEntityReferenceSerializer;

  @Override
  public IdVersion getIdVersion() {
    return IdVersion.XWIKI_2;
  }

  @Override
  public long compute(DocumentReference docRef, String lang) {
    String uniqueName = localEntityReferenceSerializer.serialize(docRef);
    if ((lang != null) && !lang.trim().isEmpty()) {
      uniqueName += ":" + lang;
    }
    return uniqueName.hashCode();
  }

  @Override
  public Iterator<Long> getDocumentIdIterator(DocumentReference docRef, String lang) {
    return Arrays.asList(compute(docRef, lang)).iterator();
  }

}
