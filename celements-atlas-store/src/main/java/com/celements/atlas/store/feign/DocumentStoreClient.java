package com.celements.atlas.store.feign;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface DocumentStoreClient {

  @RequestLine("POST /api/documents/create")
  @Headers("Content-Type: " + MediaType.APPLICATION_JSON_VALUE)
  String create(AtlasDocumentDto data);

  @RequestLine("PUT /api/documents/{id}")
  @Headers("Content-Type: " + MediaType.APPLICATION_JSON_VALUE)
  void update(@RequestBody AtlasDocumentDto data);

  @RequestLine("GET /api/documents/{id}")
  @Headers("Content-Type: " + MediaType.APPLICATION_JSON_VALUE)
  AtlasDocumentDto get(@Param("id") String id);

}
