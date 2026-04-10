package com.celements.filebase;

import static com.celements.execution.XWikiExecutionProp.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.xwiki.context.Execution;
import org.xwiki.model.reference.AttachmentReference;

import com.celements.auth.user.User;
import com.celements.auth.user.UserInstantiationException;
import com.celements.auth.user.UserService;
import com.celements.filebase.exceptions.FileBaseAddFileException;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.xpn.xwiki.doc.XWikiDocument;
import com.celements.filebase.exceptions.FileBaseLoadException;
import com.celements.filebase.exceptions.FileNotExistsException;
import com.celements.filebase.matcher.AllAttachmentMatcher;
import com.celements.model.reference.RefBuilder;
import com.celements.spring.security.AuthenticatedBaseController;
import com.celements.url.UrlService;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

@RestController
@RestControllerAdvice
@RequestMapping("/files")
public class VueFinderFilesController extends AuthenticatedBaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(VueFinderFilesController.class);

  private static final String STORAGE = "local";
  private static final int MAX_WIDTH = 800;
  private static final int MAX_HEIGHT = 800;

  private final IFileBaseServiceRole fileBaseService;
  private final UrlService urlService;
  private final IModelAccessFacade modelAccess;
  private final IRightsAccessFacadeRole rightsAccess;
  private final ModelUtils modelUtils;
  private final Execution execution;
  private final UserService userService;

  @Inject
  public VueFinderFilesController(
      IFileBaseServiceRole fileBaseService,
      UrlService urlService,
      IModelAccessFacade modelAccess,
      IRightsAccessFacadeRole rightsAccess,
      ModelUtils modelUtils,
      Execution execution,
      UserService userService) {
    this.fileBaseService = fileBaseService;
    this.urlService = urlService;
    this.modelAccess = modelAccess;
    this.rightsAccess = rightsAccess;
    this.modelUtils = modelUtils;
    this.execution = execution;
    this.userService = userService;
  }

  @GetMapping("/helloFinder")
  @PreAuthorize("permitAll()")
  public String helloFinder() {
    return "VueFinder Backend comming here!";
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
   * Search for files matching a query.
   */
  @GetMapping("/search")
  @PreAuthorize("permitAll()")
  public ListResponse search(@RequestParam("q") String query,
                             @RequestParam(name="path", required=false) String path) {
    String dirPath = normalizeDirPath(path);
    String lower = query.toLowerCase(Locale.ROOT);
    try {
      List<FileItem> files = fileBaseService.getFilesNameMatch(
              att -> att.getFilename().toLowerCase(Locale.ROOT).contains(lower))
          .stream()
          .map(att -> toFileItem(dirPath, att))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      return new ListResponse(List.of(STORAGE), dirPath, false, files);
    } catch (FileBaseLoadException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Search failed", ex);
    }
  }

  /**
   * List all tags.
   */
  @GetMapping("/tags")
  @PreAuthorize("permitAll()")
  public ResponseEntity<?> listTags() {
    if (checkAuth().isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    List<TagDto> tags = fileBaseService.getFileTags().stream()
        .map(t -> {
          TagDto dto = new TagDto();
          dto.id = modelUtils.serializeRefLocal(t.getTagRef());
          dto.prettyName = t.getPrettyName();
          dto.prettyNames = t.getPrettyNames();
          return dto;
        })
        .collect(Collectors.toList());
    return ResponseEntity.ok(tags);
  }


  /**
   * Get files (attachments) belonging to a tag.
   */
  @GetMapping("/tags/files")
  @PreAuthorize("permitAll()")
  public ResponseEntity<?> filesForTag(@RequestParam("tagId") String tagId) {
    if (checkAuth().isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    List<String> files = fileBaseService.getFileTags().stream()
        .filter(t -> modelUtils.serializeRefLocal(t.getTagRef()).equals(tagId))
        .findFirst()
        .map(FileBaseTag::getTagFileList)
        .orElse(java.util.Collections.emptyList())
        .stream()
        .map(ref -> normalizeFileName(ref.getName()))
        .collect(Collectors.toList());
    return ResponseEntity.ok(files);
  }

  /**
   * Assign tag to files.
   */
  @PostMapping("/tags/assign")
  @PreAuthorize("permitAll()")
  public ResponseEntity<?> assignTag(@RequestBody TagAssignRequest body) {
    if (checkAuth().isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    FileBaseTag tag = findTag(body.tagId);
    if (!rightsAccess.hasAccessLevel(tag.getTagRef(), EAccessLevel.EDIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No edit rights on tag document");
    }
    XWikiDocument tagDoc = modelAccess.getOrCreateDocument(tag.getTagRef());
    for (String filePath : body.filePaths) {
      String attKey = toAttachmentKey(filePath);
      var editor = XWikiObjectEditor.on(tagDoc);
      editor.filter(FileBaseTag.FILEBASE_TAG_CLASS_REF)
            .filter(FileBaseTag.ATTACHMENT_FIELD, attKey)
            .createFirstIfNotExists();
    }
    try {
      modelAccess.saveDocument(tagDoc);
    } catch (com.celements.model.access.exception.DocumentSaveException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save tag", e);
    }
    return ResponseEntity.ok(Map.of());
  }

  /**
   * Remove tag from files.
   */
  @PostMapping("/tags/remove")
  @PreAuthorize("permitAll()")
  public ResponseEntity<?> removeTag(@RequestBody TagAssignRequest body) {
    if (checkAuth().isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    FileBaseTag tag = findTag(body.tagId);
    if (!rightsAccess.hasAccessLevel(tag.getTagRef(), EAccessLevel.EDIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No edit rights on tag document");
    }
    XWikiDocument tagDoc = modelAccess.getOrCreateDocument(tag.getTagRef());
    for (String filePath : body.filePaths) {
      String attKey = toAttachmentKey(filePath);
      var editor = XWikiObjectEditor.on(tagDoc);
      editor.filter(FileBaseTag.FILEBASE_TAG_CLASS_REF)
            .filter(FileBaseTag.ATTACHMENT_FIELD, attKey)
            .delete();
    }
    try {
      modelAccess.saveDocument(tagDoc);
    } catch (com.celements.model.access.exception.DocumentSaveException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save tag", e);
    }
    return ResponseEntity.ok(Map.of());
  }

  private FileBaseTag findTag(String tagId) {
    return fileBaseService.getFileTags().stream()
        .filter(t -> modelUtils.serializeRefLocal(t.getTagRef()).equals(tagId))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
  }

  /**
   * Checks authentication via the legacy Struts auth mechanism.
   * This is necessary because the system supports Struts-only sessions that have
   * no Spring Security principal, so Spring's isAuthenticated() alone is insufficient.
   */
  private Optional<User> checkAuth() {
    try {
      var eCtx = execution.getContext();
      var ctx = eCtx.get(XWIKI_CONTEXT).orElseThrow();
      var xuser = eCtx.get(XWIKI).orElseThrow().checkAuth(ctx);
      if (xuser != null) {
        ctx.setUser(xuser.getUser());
        return Optional.of(userService.getUser(xuser.getUser()));
      }
    } catch (XWikiException | UserInstantiationException e) {
      LOGGER.warn("Failed to check auth", e);
    }
    return Optional.empty();
  }

  private String toAttachmentKey(String filePath) {
    String filename = normalizeFileName(filePath);
    try {
      XWikiAttachment att = fileBaseService.getFileNameEqual(filename);
      return modelUtils.serializeRefLocal(att.getDoc().getDocumentReference()) + "/" + filename;
    } catch (FileNotExistsException | FileBaseLoadException exp) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + filename, exp);
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
    AttachmentReference attachmentRef = RefBuilder.from(att.getDoc().getDocumentReference())
        .att(name).build(AttachmentReference.class);
    item.url = urlService.getURL(attachmentRef, "download");
    String query = "celwidth=" + MAX_WIDTH + "&celheight=" + MAX_HEIGHT;
    item.previewUrl = urlService.getURL(attachmentRef, "download", query);
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
    public String url;
    public String previewUrl;
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

  public static class TagDto {
    public String id;
    public String prettyName;
    public Map<String, String> prettyNames;
  }

  public static class TagAssignRequest {
    public String tagId;
    public List<String> filePaths;
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> handle(ResponseStatusException ex) {
    return ResponseEntity
        .status(ex.getStatus())
        .body(java.util.Map.of("message",
            ex.getReason() != null ? ex.getReason() : "Request failed"));
  }
}
