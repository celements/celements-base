package com.celements.filebase.dto;

public record FileItem(
    String dir,
    String basename,
    String extension,
    String path,
    String url,
    String previewUrl,
    String storage,
    String type,
    long file_size,
    long last_modified,
    String mime_type,
    String visibility) {
}
