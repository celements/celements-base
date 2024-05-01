package com.celements.atlas.store.feign;

import java.util.List;

public class DocumentDto {

  private String id;
  private List<ObjectDto> objects;

  DocumentDto(String id, List<ObjectDto> objects) {
    setId(id);
    setObjects(objects);
  }

  public String id() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<ObjectDto> objects() {
    return objects;
  }

  public void setObjects(List<ObjectDto> objects) {
    this.objects = List.copyOf(objects);
  }

}
