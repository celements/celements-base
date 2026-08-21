package com.celements.navigation.api;

import java.util.List;

import javax.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

public record NavigationSegmentDto(
    @Nullable @Schema(nullable = true) String partName,
    List<NavigationNodeDto> nodes) {

  public NavigationSegmentDto {
    nodes = List.copyOf(nodes);
  }

}
