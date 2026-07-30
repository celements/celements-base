package com.celements.filebase.dto;

import java.util.List;

public record TagAssignRequest(
    String tagId,
    List<String> filePaths) {
}
