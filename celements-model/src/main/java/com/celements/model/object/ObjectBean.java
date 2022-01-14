package com.celements.model.object;

import java.util.Objects;

import javax.annotation.concurrent.NotThreadSafe;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

@NotThreadSafe
public class ObjectBean {

  private DocumentReference documentReference;
  private ClassReference classReference;
  private Integer number;

  public DocumentReference getDocumentReference() {
    return documentReference;
  }

  public void setDocumentReference(DocumentReference documentReference) {
    this.documentReference = documentReference;
  }

  public ClassReference getClassReference() {
    return classReference;
  }

  public void setClassReference(ClassReference classReference) {
    this.classReference = classReference;
  }

  public Integer getNumber() {
    return number;
  }

  public void setNumber(Integer number) {
    this.number = number;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ObjectBean)
        && Objects.equals(((ObjectBean) obj).documentReference, this.documentReference)
        && Objects.equals(((ObjectBean) obj).classReference, this.classReference)
        && Objects.equals(((ObjectBean) obj).number, this.number);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentReference, classReference, number);
  }

  @Override
  public String toString() {
    return "ObjectBean [documentReference=" + documentReference + ", classReference="
        + classReference + ", number=" + number + "]";
  }
}
