package com.celements.atlas.store.feign;

public class ObjectDto {

  private String id;
  private String data;

  public ObjectDto(String id) {
    setId(id);
  }

  public String id() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String data() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

}
