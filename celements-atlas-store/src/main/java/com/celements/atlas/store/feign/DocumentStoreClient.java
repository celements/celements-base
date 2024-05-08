package com.celements.atlas.store.feign;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface DocumentStoreClient {

    @RequestLine("POST /api/documents/create")
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON_VALUE)
    String create(List<ObjectDto> objectData);

    @RequestLine("PUT /api/documents/{id}")
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON_VALUE)
    void update(@Param("id") String id,
            @RequestBody List<ObjectDto> objectData);

    @RequestLine("GET /api/documents/{id}")
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON_VALUE)
    DocumentDto get(@Param("id") String id);

}
