package com.celements.filebase;

import java.net.URLConnection;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xwiki.model.reference.AttachmentReference;

import com.celements.filebase.dto.FileItem;
import com.celements.model.reference.RefBuilder;
import com.celements.url.UrlService;
import com.xpn.xwiki.doc.XWikiAttachment;

@Component
public class FileItemHelper {

  private static final int MAX_WIDTH = 800;
  private static final int MAX_HEIGHT = 800;

  private final UrlService urlService;

  public FileItemHelper(UrlService urlService) {
    this.urlService = Objects.requireNonNull(urlService);
  }

  public FileItem toFileItem(String dirPath, XWikiAttachment att, String storage) {
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
        urlService.getURL(attachmentRef, "viewattachrev"),
        storage,
        "file",
        (long) att.getFilesize(),
        toUnixSeconds(att.getDate()),
        guessMimeType(name),
        "public");
  }

  public String guessMimeType(String filename) {
    String mime = URLConnection.guessContentTypeFromName(filename);
    return mime != null ? mime : "application/octet-stream";
  }

  public String extensionOf(String name) {
    int i = name.lastIndexOf('.');
    return (i > 0) && (i < (name.length() - 1)) ? name.substring(i + 1).toLowerCase(Locale.ROOT)
        : "";
  }

  public long toUnixSeconds(Date date) {
    if (date == null) {
      return Instant.now().getEpochSecond();
    }
    return date.toInstant().getEpochSecond();
  }

  public String normalizeFileName(String path) {
    String p = StringUtils.hasText(path) ? path.trim() : "";
    int lastSlash = p.lastIndexOf('/');
    if (lastSlash >= 0) {
      return p.substring(lastSlash + 1);
    }
    return p;
  }
}
