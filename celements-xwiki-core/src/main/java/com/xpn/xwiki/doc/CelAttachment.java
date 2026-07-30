package com.xpn.xwiki.doc;

import static java.util.Objects.*;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public record CelAttachment(
    String filename,
    int filesize,
    String author,
    String version,
    String comment,
    Instant date) {

  private CelAttachment(XWikiAttachment attachment) {
    this(requireNonNull(attachment).getFilename(), attachment.getFilesize(),
        attachment.getAuthor(), attachment.getVersion(), attachment.getComment(),
        Optional.ofNullable(attachment.getDate()).map(Date::toInstant).orElse(null));
  }

  public static CelAttachment from(XWikiAttachment attachment) {
    return new CelAttachment(attachment);
  }

  public String getFilename() {
    return filename();
  }

  public int getFilesize() {
    return filesize();
  }

  public String getAuthor() {
    return author();
  }

  public String getVersion() {
    return version();
  }

  public String getComment() {
    return comment();
  }

  public Instant getDate() {
    return date();
  }
}
