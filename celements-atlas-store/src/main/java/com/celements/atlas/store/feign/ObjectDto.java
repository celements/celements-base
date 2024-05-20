package com.celements.atlas.store.feign;

import java.util.Map;

public class ObjectDto {

  private String id;
  private Map<String, Object> data;

  public ObjectDto(String id) {
    setId(id);
  }

  public String id() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Map<String, Object> data() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

}
