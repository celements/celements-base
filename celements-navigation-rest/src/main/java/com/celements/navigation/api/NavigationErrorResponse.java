package com.celements.navigation.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stable, safe navigation API error.")
public record NavigationErrorResponse(
    @Schema(description = "Stable machine-readable error code",
        example = "invalid_reference") String code,
    @Schema(description = "Safe error message",
        example = "The reference is invalid.") String message) {

}
