package com.celements.filebase.dto;

import java.util.Map;

public record TagDto(
    String id,
    String prettyName,
    Map<String, String> prettyNames) {
}
