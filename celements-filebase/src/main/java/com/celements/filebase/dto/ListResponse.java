package com.celements.filebase.dto;

import java.util.List;

public record ListResponse(
    List<String> storages,
    String dirname,
    boolean read_only,
    List<FileItem> files) {
}
