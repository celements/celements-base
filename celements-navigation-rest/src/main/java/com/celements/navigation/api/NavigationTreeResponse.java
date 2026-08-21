package com.celements.navigation.api;

import java.util.List;

import javax.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

public record NavigationTreeResponse(
    String nodeSpace,
    @Nullable @Schema(nullable = true) String currentNode,
    String language,
    @Nullable @Schema(nullable = true) String partName,
    int showInactiveToLevel,
    List<NavigationSegmentDto> segments) {

  public NavigationTreeResponse {
    segments = List.copyOf(segments);
  }

}
