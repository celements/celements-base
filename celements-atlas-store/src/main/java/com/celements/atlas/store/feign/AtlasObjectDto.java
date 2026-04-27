package com.celements.atlas.store.feign;

import java.util.Date;
import java.util.Map;

public class AtlasObjectDto {

  private String id;
  private String className;
  private String language;

  private String creator;
  private Date creationDate;
  private String author;
  private Date changeDate;

  private Map<String, Object> properties;

  public String id() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Date getChangeDate() {
    return changeDate;
  }

  public void setChangeDate(Date changeDate) {
    this.changeDate = changeDate;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }
}
