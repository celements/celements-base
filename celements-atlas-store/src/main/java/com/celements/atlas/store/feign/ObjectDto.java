package com.celements.atlas.store.feign;

public class ObjectDto {

  private String id;

  public ObjectDto(String id) {
    setId(id);
  }

  public String id() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

}
