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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.auth.user.User;
import com.celements.filebase.dto.DeleteItem;
import com.celements.filebase.dto.DeleteRequest;
import com.celements.filebase.dto.FileItem;
import com.celements.filebase.dto.ListResponse;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.model.reference.RefBuilder;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.spring.security.AuthenticatedBaseController;
import com.celements.url.UrlService;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

@RestController
@RestControllerAdvice
@RequestMapping("/attachments/{spaceName}/{docName}")
public class PageAttachmentsController extends AuthenticatedBaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageAttachmentsController.class);

  private static final String STORAGE = "attachments";
  private static final int MAX_WIDTH = 800;
  private static final int MAX_HEIGHT = 800;

  private final IAttachmentServiceRole attService;
  private final UrlService urlService;
  private final IModelAccessFacade modelAccess;
  private final IRightsAccessFacadeRole rightsAccess;
  private final ModelContext modelContext;

  @Inject
  public PageAttachmentsController(
      IAttachmentServiceRole attService,
      UrlService urlService,
      IModelAccessFacade modelAccess,
      IRightsAccessFacadeRole rightsAccess,
      ModelContext modelContext) {
    this.attService = attService;
    this.urlService = urlService;
    this.modelAccess = modelAccess;
    this.rightsAccess = rightsAccess;
    this.modelContext = modelContext;
  }

  @GetMapping("")
  @PreAuthorize("permitAll()")
  public ListResponse list(
      @PathVariable String spaceName,
      @PathVariable String docName,
      @RequestParam(name = "path", required = false) String path) {
    AttachmentRequest request = prepareRequest(spaceName, docName, path, EAccessLevel.VIEW);
    try {
      XWikiDocument doc = modelAccess.getOrCreateDocument(request.docRef());
      List<FileItem> files = attService.getAttachmentsNameMatch(doc, att -> true)
          .stream()
          .map(att -> toFileItem(request.dirPath(), att))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      return new ListResponse(List.of(STORAGE), request.dirPath(), false, files);
    } catch (DocumentLoadException exp) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document load failed", exp);
    }
  }

  @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("permitAll()")
  public Object upload(
      @PathVariable String spaceName,
      @PathVariable String docName,
      @RequestParam(name = "path", required = false) String path,
      @RequestParam("file") List<MultipartFile> files) {
    AttachmentRequest request = prepareRequest(spaceName, docName, path, EAccessLevel.EDIT);
    try {
      XWikiDocument doc = modelAccess.getOrCreateDocument(request.docRef());
      for (MultipartFile file : files) {
        if ((file == null) || file.isEmpty()) {
          continue;
        }
        String original = file.getOriginalFilename();
        String fileName = StringUtils.hasText(original) ? original : "upload.bin";
        String safeName = attService.clearFileName(fileName);
        try (InputStream in = file.getInputStream()) {
          attService.addAttachment(doc, in, safeName, null, "Uploaded via VueFinder");
        } catch (IOException | AttachmentToBigException | AddingAttachmentContentFailedException exp) {
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fileupload failed.",
              exp);
        }
      }
      return java.util.Collections.emptyMap();
    } catch (DocumentLoadException | DocumentSaveException exp) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fileupload failed.", exp);
    }
  }

  @PostMapping("/delete")
  @PreAuthorize("permitAll()")
  public ListResponse delete(
      @PathVariable String spaceName,
      @PathVariable String docName,
      @RequestBody DeleteRequest body) {
    AttachmentRequest request = prepareRequest(spaceName, docName, body.path(), EAccessLevel.DELETE);
    try {
      XWikiDocument doc = modelAccess.getOrCreateDocument(request.docRef());
      List<AttachmentReference> attsToDelete = new ArrayList<>();
      if (body.items() != null) {
        for (DeleteItem item : body.items()) {
          if (item == null || !"file".equalsIgnoreCase(item.type())) {
            continue;
          }
          String delFileName = normalizeFileName(item.path());
          if (attService.existsAttachmentNameEqual(doc, delFileName)) {
            attsToDelete.add(new AttachmentReference(delFileName, request.docRef()));
          }
        }
      }
      if (!attsToDelete.isEmpty()) {
        attService.deleteAttachmentList(attsToDelete);
      }
      return list(spaceName, docName, body.path());
    } catch (DocumentLoadException exp) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delete failed.", exp);
    }
  }

  @GetMapping("/search")
  @PreAuthorize("permitAll()")
  public ListResponse search(
      @PathVariable String spaceName,
      @PathVariable String docName,
      @RequestParam("q") String query,
      @RequestParam(name = "path", required = false) String path) {
    AttachmentRequest request = prepareRequest(spaceName, docName, path, EAccessLevel.VIEW);
    String lower = query.toLowerCase(Locale.ROOT);
    try {
      XWikiDocument doc = modelAccess.getOrCreateDocument(request.docRef());
      List<FileItem> files = attService.getAttachmentsNameMatch(doc,
          att -> att.getFilename().toLowerCase(Locale.ROOT).contains(lower))
          .stream()
          .map(att -> toFileItem(request.dirPath(), att))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      return new ListResponse(List.of(STORAGE), request.dirPath(), false, files);
    } catch (DocumentLoadException exp) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Search failed", exp);
    }
  }

  private AttachmentRequest prepareRequest(
      String spaceName,
      String docName,
      String path,
      EAccessLevel accessLevel) {
    checkAuth();
    User user = modelContext.user().orElse(null);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    DocumentReference docRef = RefBuilder.create()
        .with(modelContext.getWikiRef())
        .space(spaceName)
        .doc(docName)
        .build(DocumentReference.class);
    if (!rightsAccess.hasAccessLevel(docRef, accessLevel, user)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return new AttachmentRequest(docRef, normalizeDirPath(spaceName, docName, path));
  }

  private String normalizeDirPath(String spaceName, String docName, String path) {
    String expectedPrefix = STORAGE + "://" + spaceName + "/" + docName;
    String p = StringUtils.hasText(path) ? path.trim() : expectedPrefix;
    if (!p.startsWith(STORAGE + "://")) {
      p = expectedPrefix + "/" + p;
    }
    if (p.endsWith("/") && !p.equals(expectedPrefix)) {
      p = p.substring(0, p.length() - 1);
    }
    return p;
  }

  String normalizeFileName(String path) {
    String p = StringUtils.hasText(path) ? path.trim() : "";
    int lastSlash = p.lastIndexOf('/');
    if (lastSlash >= 0) {
      return p.substring(lastSlash + 1);
    }
    return p;
  }

  private FileItem toFileItem(String dirPath, XWikiAttachment att) {
    String name = att.getFilename();
    AttachmentReference attachmentRef = RefBuilder.from(att.getDoc().getDocumentReference())
        .att(name).build(AttachmentReference.class);
    String query = "celwidth=" + MAX_WIDTH + "&celheight=" + MAX_HEIGHT;
    return new FileItem(
        dirPath,
        name,
        extensionOf(name),
        dirPath.endsWith("/") ? (dirPath + name) : (dirPath + "/" + name),
        urlService.getURL(attachmentRef, "download"),
        urlService.getURL(attachmentRef, "download", query),
        STORAGE,
        "file",
        (long) att.getFilesize(),
        toUnixSeconds(att.getDate()),
        guessMimeType(name),
        "public");
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

  @ExceptionHandler(ResponseStatusException.class)
  @PreAuthorize("permitAll()")
  public ResponseEntity<?> handle(ResponseStatusException ex) {
    return ResponseEntity
        .status(ex.getStatus())
        .body(java.util.Map.of("message",
            ex.getReason() != null ? ex.getReason() : "Request failed"));
  }

  @ExceptionHandler({
      org.springframework.web.bind.MissingServletRequestParameterException.class,
      org.springframework.web.multipart.support.MissingServletRequestPartException.class
  })
  @PreAuthorize("permitAll()")
  public ResponseEntity<?> handleMissingParams(
      Exception ex,
      javax.servlet.http.HttpServletRequest request) {
    LOGGER.warn("Missing parameter/part: {}, Request URI: {}, Content-Type: {}",
        ex.getMessage(), request.getRequestURI(), request.getContentType());
    java.util.Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      LOGGER.warn("Header {}: {}", headerName, request.getHeader(headerName));
    }
    try {
      if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
        org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest =
            (org.springframework.web.multipart.MultipartHttpServletRequest) request;
        LOGGER.warn("Multipart parameter names: {}", multipartRequest.getParameterMap().keySet());
        LOGGER.warn("Multipart file names: {}", multipartRequest.getFileMap().keySet());
      } else {
        LOGGER.warn("Request is not a MultipartHttpServletRequest. Parameter names: {}",
            request.getParameterMap().keySet());
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to log request details", e);
    }
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(java.util.Map.of("message", ex.getMessage()));
  }
}
