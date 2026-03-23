package com.celements.filebase;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.celements.filebase.exceptions.FileBaseAddFileException;
import com.celements.filebase.exceptions.FileBaseLoadException;
import com.celements.filebase.matcher.AllAttachmentMatcher;
import com.celements.model.context.ModelContext;
import com.celements.spring.security.AuthenticatedBaseController;
import com.xpn.xwiki.doc.XWikiAttachment;

@RestController
@RestControllerAdvice
@RequestMapping("/files")
public class VueFinderFilesController extends AuthenticatedBaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(VueFinderFilesController.class);

  private static final String STORAGE = "local";

  private final IFileBaseServiceRole fileBaseService;
  private final ModelContext context;

  @Inject
  public VueFinderFilesController(
      IFileBaseServiceRole fileBaseService,
      ModelContext context) {
    this.fileBaseService = fileBaseService;
    this.context = context;
  }

  @GetMapping("/helloFinder")
  @PreAuthorize("permitAll()")
  public String helloFinder() {
    return "VueFinder Backend comming here!";
  }

  /**
   * Preview a file (attachment).
   * GET /api/files/preview?path=local://public/FileRepo/DALLE.png
   */
  @GetMapping(path = "/preview")
  @PreAuthorize("permitAll()")
  public ResponseEntity<byte[]> preview(@RequestParam("path") String path) {
    String fileName = normalizeFileName(path);
    try {
      XWikiAttachment att = fileBaseService.getFileNameEqual(fileName);
      byte[] content;
      try (InputStream in = att.getContentInputStream(context.getXWikiContext())) {
        content = in.readAllBytes();
      }
      String mimeType = guessMimeType(fileName);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(mimeType))
          .body(content);
    } catch (Exception exp) {
      LOGGER.warn("Failed to load file for preview: {}", fileName, exp);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found.", exp);
    }
  }

  /**
   * List files (attachments) in a "directory" (XWiki document).
   * GET /api/files?path=local://public/FileRepo
   */
  @GetMapping(path = "")
  @PreAuthorize("permitAll()")
  public ListResponse list(@RequestParam(name = "path", required = false) String path) {
    String dirPath = normalizeDirPath(path);
    // TODO extend filebase for directory support
    try {
      List<FileItem> files = fileBaseService.getFilesNameMatch(new AllAttachmentMatcher()).stream()
          .map(att -> toFileItem(dirPath, att))
          // enforce XWiki rights (skip what user cannot access)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      return new ListResponse(List.of(STORAGE), dirPath, false, files);
    } catch (FileBaseLoadException exp) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found.", exp);
    }
  }

  /**
   * Upload files as attachments to the directory document.
   * POST /api/files/upload?path=local://public/FileRepo
   * multipart/form-data: file=<binary> (can be repeated)
   * Response: {} on success.
   */
  @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("permitAll()")
  public Object upload(@RequestParam("path") String path,
      @RequestParam("file") List<MultipartFile> files) {
    String dirPath = normalizeDirPath(path);
    // TODO extend filebase for directory support
    for (MultipartFile file : files) {
      if ((file == null) || file.isEmpty()) {
        continue;
      }
      String original = file.getOriginalFilename();
      String fileName = StringUtils.hasText(original) ? original : "upload.bin";

      try (InputStream in = file.getInputStream()) {
        fileBaseService.addFile(in, fileName, "Uploaded via VueFinder");
      } catch (IOException | FileBaseAddFileException exp) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fileupload failed.",
            exp);
      }
    }
    return java.util.Collections.emptyMap();
  }

  /**
   * Delete files (attachments).
   * POST /api/files/delete?path=local://public/FileRepo
   * body: { "items": [ { "path": "local://public/FileRepo/a.png", "type":"file" } ] }
   * Response: updated list (same structure as GET /). :contentReference[oaicite:5]{index=5}
   */
  @PostMapping(path = "/delete")
  @PreAuthorize("permitAll()")
  public ListResponse delete(@RequestBody DeleteRequest body) {
    String dirPath = normalizeDirPath(body.path);
    List<String> refs = new ArrayList<>();
    if ((body.items != null)) {
      for (DeleteItem item : body.items) {
        if ((item == null) || !"file".equalsIgnoreCase(item.type)) {
          continue;
        }
        String delFileName = normalizeFileName(item.path);
        LOGGER.debug("add filename '{}' to delete list", delFileName);
        refs.add(delFileName);
      }
    }
    if (!refs.isEmpty()) {
      fileBaseService.deleteFileList(refs);
    }
    return list(dirPath);
  }

  /**
   * Directory path normalization:
   * - null/empty -> local://
   * - ensure scheme
   * - strip trailing slash (except local://)
   */
  private String normalizeDirPath(String path) {
    String p = StringUtils.hasText(path) ? path.trim() : (STORAGE + "://");
    if (!p.contains("://")) {
      p = STORAGE + "://" + p;
    }
    if (p.endsWith("/") && !p.equals(STORAGE + "://")) {
      p = p.substring(0, p.length() - 1);
    }
    return p;
  }

  String normalizeFileName(String path) {
    String p = StringUtils.hasText(path) ? path.trim() : (STORAGE + "://");
    var parts = p.split("://|/");
    return parts[Math.max(0, parts.length - 1)];
  }

  private FileItem toFileItem(String dirPath, XWikiAttachment att) {
    String name = att.getFilename();
    String ext = extensionOf(name);
    long size = att.getFilesize();
    long lm = toUnixSeconds(att.getDate());
    String filePath = dirPath.endsWith("/") ? (dirPath + name) : (dirPath + "/" + name);

    FileItem item = new FileItem();
    item.dir = dirPath;
    item.basename = name;
    item.extension = ext;
    item.path = filePath;
    item.storage = STORAGE;
    item.type = "file";
    item.file_size = size;
    item.last_modified = lm;
    item.mime_type = guessMimeType(name);
    item.visibility = "public";
    return item;
  }

  private String guessMimeType(String filename) {
    String mime = URLConnection.guessContentTypeFromName(filename);
    return (mime != null) ? mime : "application/octet-stream";
  }

  private String extensionOf(String name) {
    int i = name.lastIndexOf('.');
    return ((i > 0) && (i < (name.length() - 1))) ? name.substring(i + 1).toLowerCase(Locale.ROOT)
        : "";
  }

  private long toUnixSeconds(Date date) {
    if (date == null) {
      return Instant.now().getEpochSecond();
    }
    return date.toInstant().getEpochSecond();
  }

  // ----------------------------
  // DTOs matching VueFinder’s expected JSON shape
  // ----------------------------

  public static class ListResponse {

    public List<String> storages;
    public String dirname;
    public boolean read_only;
    public List<FileItem> files;

    public ListResponse(List<String> storages, String dirname, boolean read_only,
        List<FileItem> files) {
      this.storages = storages;
      this.dirname = dirname;
      this.read_only = read_only;
      this.files = files;
    }
  }

  public static class FileItem {

    public String dir;
    public String basename;
    public String extension;
    public String path;
    public String storage;
    public String type; // "file" or "dir"
    public long file_size;
    public long last_modified; // unix seconds
    public String mime_type;
    public String visibility; // "public"/"private"
  }

  public static class DeleteRequest {

    public String path;
    public List<DeleteItem> items;
  }

  public static class DeleteItem {

    public String path;
    public String type;
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> handle(ResponseStatusException ex) {
    return ResponseEntity
        .status(ex.getStatus())
        .body(java.util.Map.of("message",
            ex.getReason() != null ? ex.getReason() : "Request failed"));
  }
}
