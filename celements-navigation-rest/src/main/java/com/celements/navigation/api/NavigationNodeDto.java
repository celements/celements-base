package com.celements.navigation.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

public record NavigationNodeDto(
    String docRef,
    String url,
    String title,
    @JsonProperty("isLeaf") boolean isLeaf,
    @JsonProperty("isActive") boolean isActive,
    @JsonProperty("isOpen") boolean isOpen,
    @Schema(description = "Expanded rights-visible child nodes") List<NavigationNodeDto> children) {

  public NavigationNodeDto {
    children = List.copyOf(children);
  }

}
