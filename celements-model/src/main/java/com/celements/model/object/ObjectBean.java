package com.celements.model.object;

import java.util.Objects;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotEmpty;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

@NotThreadSafe
public class ObjectBean {

  private DocumentReference documentReference;
  private Integer objNum;
  private ClassReference classReference;

  public DocumentReference getDocumentReference() {
    return documentReference;
  }

  public void setDocumentReference(DocumentReference documentReference) {
    this.documentReference = documentReference;
  }

  public Integer getNumber() {
    return objNum;
  }

  public void setNumber(Integer number) {
    this.objNum = number;
  }

  public ClassReference getClassReference() {
    return classReference;
  }

  public void setClassReference(ClassReference classReference) {
    this.classReference = classReference;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ObjectBean)
        && Objects.equals(((ObjectBean) obj).documentReference, this.documentReference)
        && Objects.equals(((ObjectBean) obj).objNum, this.objNum)
        && Objects.equals(((ObjectBean) obj).classReference, this.classReference);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(documentReference)
        .append(objNum)
        .append(classReference)
        .toHashCode();
  }

  @Override
  @NotEmpty
  public String toString() {
    return "docRef [" + documentReference + "], classReference [" + classReference
        + "] from objNum [" + objNum + "]";
  }
}
