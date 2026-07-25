package com.xpn.xwiki.doc;

import static java.util.Objects.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

public record CelObject(
    DocumentReference documentReference,
    LocalDocumentReference classReference,
    int number,
    String guid,
    long id,
    IdVersion idVersion,
    List<CelProperty> properties) {

  public CelObject {
    requireNonNull(documentReference);
    requireNonNull(classReference);
    properties = List.copyOf(properties);
  }

  private CelObject(BaseObject object) {
    this(object, requireNonNull(object).getXClassReference());
  }

  private CelObject(BaseObject object, DocumentReference classReference) {
    this(requireNonNull(object).getDocumentReference(),
        new LocalDocumentReference(classReference), object.getNumber(), object.getGuid(),
        object.hasValidId() ? object.getId() : 0,
        object.hasValidId() ? object.getIdVersion() : null, toCelProperties(object));
  }

  public static CelObject from(BaseObject object) {
    return new CelObject(object);
  }

  static CelObject from(BaseObject object, DocumentReference classReference) {
    return new CelObject(object, classReference);
  }

  public DocumentReference getDocumentReference() {
    return documentReference();
  }

  public LocalDocumentReference getClassReference() {
    return classReference();
  }

  public int getNumber() {
    return number();
  }

  public String getGuid() {
    return guid();
  }

  public long getId() {
    return id();
  }

  public IdVersion getIdVersion() {
    return idVersion();
  }

  public List<CelProperty> getProperties() {
    return properties();
  }

  public Optional<CelProperty> getProperty(String name) {
    return properties.stream().filter(property -> property.getName().equals(name)).findFirst();
  }

  public String getStringValue(String name) {
    return getProperty(name).map(CelProperty::getStringValue).orElse("");
  }

  public int getIntValue(String name) {
    return getProperty(name).map(CelProperty::getIntValue).orElse(0);
  }

  public Instant getDateValue(String name) {
    return getProperty(name).map(CelProperty::getDateValue).orElse(null);
  }

  private static List<CelProperty> toCelProperties(BaseObject object) {
    return Arrays.stream(object.getProperties())
        .map(BaseProperty.class::cast)
        .map(CelProperty::from)
        .toList();
  }
}
