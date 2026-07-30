package com.celements.navigation.api;

import java.util.List;

import javax.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A case-sensitive root navigation part and its nodes.")
public record NavigationSegmentDto(
    @Nullable @Schema(description = "Root part name; null represents the unnamed part",
        example = "main", nullable = true) String partName,
    @Schema(
        description = "Root nodes in effective navigation position order") List<NavigationNodeDto> nodes) {

  public NavigationSegmentDto {
    nodes = List.copyOf(nodes);
  }

}
