package com.celements.filebase.dto;

import java.util.List;

public record DeleteRequest(
    String path,
    List<DeleteItem> items) {
}
