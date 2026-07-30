package com.celements.navigation.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One rights-visible navigation node.")
public record NavigationNodeDto(
    @Schema(description = "Canonical local document reference",
        example = "Content.Home") String docRef,
    @Schema(description = "Relative view URL containing the resolved language",
        example = "/Content/Home?language=de") String url,
    @Schema(description = "Localized navigation title", example = "Startseite") String title,
    @JsonProperty("isLeaf") @Schema(
        description = "Whether the node has no valid, rights-visible children") boolean isLeaf,
    @JsonProperty("isActive") @Schema(
        description = "Whether this node is the requested current node") boolean isActive,
    @JsonProperty("isOpen") @Schema(
        description = "Whether this node's children are expanded") boolean isOpen,
    @Schema(
        description = "Expanded child nodes in effective navigation position order") List<NavigationNodeDto> children) {

  public NavigationNodeDto {
    children = List.copyOf(children);
  }

}
