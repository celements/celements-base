package com.celements.atlas.store.feign;

import java.util.List;

public class AtlasDocumentDto {

  private String id;
  private List<AtlasObjectDto> objects;

  public AtlasDocumentDto(String id) {
    setId(id);
  }

  public String id() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<AtlasObjectDto> getObjects() {
    return objects;
  }

  public void setObjects(List<AtlasObjectDto> objects) {
    this.objects = objects;
  }

}
