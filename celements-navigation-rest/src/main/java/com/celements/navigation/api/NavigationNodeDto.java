package com.celements.navigation.api;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record NavigationNodeDto(
    String docRef,
    String url,
    String title,
    boolean isLeaf,
    boolean isActive,
    boolean isOpen,
    @Schema(description = "Expanded rights-visible child nodes") List<NavigationNodeDto> children) {

  public NavigationNodeDto {
    children = List.copyOf(children);
  }

}
