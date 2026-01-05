package com.celements.filebase;

import java.net.URLConnection;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.celements.filebase.exceptions.FileBaseLoadException;
import com.celements.filebase.matcher.AllAttachmentMatcher;
import com.celements.spring.security.AuthenticatedBaseController;
import com.xpn.xwiki.doc.XWikiAttachment;

@RestController
@RestControllerAdvice
@RequestMapping("/files")
public class VueFinderFilesController extends AuthenticatedBaseController {

  private static final String STORAGE = "local";

  private final IFileBaseServiceRole fileBaseService;

  @Inject
  public VueFinderFilesController(
      IFileBaseServiceRole fileBaseService) {
    this.fileBaseService = fileBaseService;
  }

  @GetMapping("/helloFinder")
  @PreAuthorize("permitAll()")
  public String helloFinder() {
    return "VueFinder Backend comming here!";
  }

  /**
   * List files (attachments) in a "directory" (XWiki document).
   * GET /api/files/?path=local://public/FileRepo
   */
  @GetMapping(path = "/")
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

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> handle(ResponseStatusException ex) {
    return ResponseEntity
        .status(ex.getStatus())
        .body(java.util.Map.of("message",
            ex.getReason() != null ? ex.getReason() : "Request failed"));
  }
}
