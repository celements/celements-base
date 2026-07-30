package com.celements.filebase.dto;

public record TagRenameRequest(
    String tagId,
    String newLabel) {
}
