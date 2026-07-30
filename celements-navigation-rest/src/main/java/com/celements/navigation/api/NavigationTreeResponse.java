package com.celements.navigation.api;

import java.util.List;

import javax.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Caller-relative, localized, segmented navigation tree.")
public record NavigationTreeResponse(
    @Schema(description = "Canonical local navigation root", example = "Content") String nodeSpace,
    @Nullable @Schema(description = "Canonical local active document reference",
        example = "Content.MyPage", nullable = true) String currentNode,
    @Schema(description = "Resolved language", example = "de") String language,
    @Nullable @Schema(description = "Requested case-sensitive root part filter", example = "main",
        nullable = true) String partName,
    @Schema(description = "Inactive expansion threshold", minimum = "0", maximum = "100",
        example = "2") int showInactiveToLevel,
    @Schema(
        description = "Navigation parts; the unnamed part is first") List<NavigationSegmentDto> segments) {

  public NavigationTreeResponse {
    segments = List.copyOf(segments);
  }

}
