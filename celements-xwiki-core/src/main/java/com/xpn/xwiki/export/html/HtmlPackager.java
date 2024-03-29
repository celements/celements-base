package com.xpn.xwiki.export.html;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.ExportURLFactory;
import com.xpn.xwiki.web.Utils;

/**
 * Create a ZIP package containing a range of HTML pages with skin and attachment dependencies.
 *
 * @version $Id$
 * @since XWiki Platform 1.3M1
 */
public class HtmlPackager {

  private static final Log LOG = LogFactory.getLog(HtmlPackager.class);

  /**
   * A point.
   */
  private static final String POINT = ".";

  /**
   * Name of the context property containing the document.
   */
  private static final String CONTEXT_TDOC = "tdoc";

  /**
   * Name of the Velocity context property containing the document.
   */
  private static final String VCONTEXT_DOC = "doc";

  /**
   * Name of the Velocity context property containing the document.
   */
  private static final String VCONTEXT_CDOC = "cdoc";

  /**
   * Name of the Velocity context property containing the document.
   */
  private static final String VCONTEXT_TDOC = CONTEXT_TDOC;

  /**
   * The separator in an internal zip path.
   */
  private static final String ZIPPATH_SEPARATOR = "/";

  /**
   * The name of the package for which packager append ".zip".
   */
  private String name = "html.export";

  /**
   * A description of the package.
   */
  private String description = "";

  /**
   * The pages to export. A {@link Set} of page name.
   */
  private Set<String> pages = new HashSet<>();

  /**
   * Modify the name of the package for which packager append ".zip".
   *
   * @param name
   *          the name of the page.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the name of the package for which packager append ".zip".
   */
  public String getName() {
    return name;
  }

  /**
   * Modify the description of the package.
   *
   * @param description
   *          the description of the package.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the description of the package.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Add a page to export.
   *
   * @param page
   *          the name of the page to export.
   */
  public void addPage(String page) {
    this.pages.add(page);
  }

  /**
   * Add a range of pages to export.
   *
   * @param pages
   *          a range of pages to export.
   */
  public void addPages(Collection<String> pages) {
    this.pages.addAll(pages);
  }

  /**
   * Add rendered document to ZIP stream.
   *
   * @param pageName
   *          the name (used with {@link com.xpn.xwiki.XWiki#getDocument(String, XWikiContext)}) of
   *          the page to
   *          render.
   * @param zos
   *          the ZIP output stream.
   * @param context
   *          the XWiki context.
   * @param vcontext
   *          the Velocity context.
   * @throws XWikiException
   *           error when rendering document.
   * @throws IOException
   *           error when rendering document.
   */
  private void renderDocument(String pageName, ZipOutputStream zos, XWikiContext context,
      VelocityContext vcontext)
      throws XWikiException, IOException {
    XWikiDocument doc = context.getWiki().getDocument(pageName, context);

    String zipname = doc.getDatabase() + POINT + doc.getSpace() + POINT + doc.getName();
    String language = doc.getLanguage();
    if ((language != null) && (language.length() != 0)) {
      zipname += POINT + language;
    }

    zipname += ".html";

    ZipEntry zipentry = new ZipEntry(zipname);
    zos.putNextEntry(zipentry);

    context.setDatabase(doc.getDatabase());
    context.setDoc(doc);
    vcontext.put(VCONTEXT_DOC, doc.newDocument(context));
    vcontext.put(VCONTEXT_CDOC, vcontext.get(VCONTEXT_DOC));

    XWikiDocument tdoc = doc.getTranslatedDocument(context);
    context.put(CONTEXT_TDOC, tdoc);
    vcontext.put(VCONTEXT_TDOC, tdoc.newDocument(context));

    String content = context.getWiki().parseTemplate("view.vm", context);

    zos.write(content.getBytes(context.getWiki().getEncoding()));
    zos.closeEntry();
  }

  /**
   * Init provided {@link ExportURLFactory} and add rendered documents to ZIP stream.
   *
   * @param zos
   *          the ZIP output stream.
   * @param tempdir
   *          the directory where to copy attached files.
   * @param urlf
   *          the {@link com.xpn.xwiki.web.XWikiURLFactory} used to render the documents.
   * @param context
   *          the XWiki context.
   * @throws XWikiException
   *           error when render documents.
   * @throws IOException
   *           error when render documents.
   */
  private void renderDocuments(ZipOutputStream zos, File tempdir, ExportURLFactory urlf,
      XWikiContext context)
      throws XWikiException, IOException {
    ExecutionContextManager ecim = Utils.getComponent(ExecutionContextManager.class);
    Execution execution = Utils.getComponent(Execution.class);

    VelocityContext oldVelocityContext = (VelocityContext) context.get("vcontext");

    try {
      XWikiContext renderContext = (XWikiContext) context.clone();
      renderContext.put("action", "view");

      ExecutionContext ec = new ExecutionContext();

      // Bridge with old XWiki Context, required for old code.
      ec.setProperty("xwikicontext", renderContext);

      ecim.initialize(ec);

      // Push a clean new Execution Context since we don't want the main Execution Context to be
      // used for
      // rendering the HTML pages to export. It's cleaner to isolate it as we do. Note that the new
      // Execution Context automatically gets initialized with a new Velocity Context by
      // the VelocityRequestInitializer class.
      execution.pushContext(ec);

      VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);

      // At this stage we have a clean Velocity Context
      VelocityContext vcontext = velocityManager.getVelocityContext();

      urlf.init(this.pages, tempdir, renderContext);
      renderContext.setURLFactory(urlf);

      for (String pageName : this.pages) {
        renderDocument(pageName, zos, renderContext, vcontext);
      }
    } catch (ExecutionContextException e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_EXPORT,
          XWikiException.ERROR_XWIKI_INIT_FAILED,
          "Failed to initialize Execution Context", e);
    } finally {
      // We must ensure that the new request we've used is removed so that the current
      // thread can continue to use its original Execution Context.
      execution.popContext();

      context.put("vcontext", oldVelocityContext);
    }
  }

  /**
   * Apply export and create the ZIP package.
   *
   * @param context
   *          the XWiki context used to render pages.
   * @throws IOException
   *           error when creating the package.
   * @throws XWikiException
   *           error when render the pages.
   */
  public void export(XWikiContext context) throws IOException, XWikiException {
    context.getResponse().setContentType("application/zip");
    context.getResponse().addHeader("Content-disposition",
        "attachment; filename=" + Util.encodeURI(name, context) + ".zip");
    context.setFinished(true);

    ZipOutputStream zos = new ZipOutputStream(context.getResponse().getOutputStream());

    File dir = context.getWiki().getTempDirectory(context);
    File tempdir = new File(dir, RandomStringUtils.randomAlphanumeric(8));
    tempdir.mkdirs();
    File attachmentDir = new File(tempdir, "attachment");
    attachmentDir.mkdirs();

    // Create custom URL factory
    ExportURLFactory urlf = new ExportURLFactory();

    // Render pages to export
    renderDocuments(zos, tempdir, urlf, context);

    // Add required skins to ZIP file
    for (String skinName : urlf.getNeededSkins()) {
      addSkinToZip(skinName, zos, urlf.getExporteSkinFiles(), context);
    }

    // add "resources" folder
    File file = new File(context.getWiki().getEngineContext().getRealPath("/resources/"));
    addDirToZip(file, zos, "resources" + ZIPPATH_SEPARATOR, urlf.getExporteSkinFiles());

    // Add attachments and generated skin files files to ZIP file
    addDirToZip(tempdir, zos, "", null);

    zos.setComment(description);

    // Finish ZIP file
    zos.finish();
    zos.flush();

    // Delete temporary directory
    deleteDirectory(tempdir);
  }

  /**
   * Delete a directory and all with all it's content.
   *
   * @param directory
   *          the directory to delete.
   */
  private static void deleteDirectory(File directory) {
    if (!directory.isDirectory()) {
      return;
    }

    File[] files = directory.listFiles();

    if (files == null) {
      return;
    }

    for (File file : files) {
      if (file.isDirectory()) {
        deleteDirectory(file);
        continue;
      }

      file.delete();
    }

    directory.delete();
  }

  /**
   * Add skin to the package in sub-directory "skins".
   *
   * @param skinName
   *          the name of the skin.
   * @param out
   *          the ZIP output stream where to put the skin.
   * @param context
   *          the XWiki context.
   * @throws IOException
   *           error when adding the skin to package.
   */
  private static void addSkinToZip(String skinName, ZipOutputStream out,
      Collection<String> exportedSkinFiles,
      XWikiContext context) throws IOException {
    File file = new File(context.getWiki().getEngineContext().getRealPath("/skins/" + skinName));
    addDirToZip(file, out, "skins" + ZIPPATH_SEPARATOR + skinName + ZIPPATH_SEPARATOR,
        exportedSkinFiles);
  }

  /**
   * Add a directory and all its sub-directories to the package.
   *
   * @param directory
   *          the directory to add.
   * @param out
   *          the ZIP output stream where to put the skin.
   * @param basePath
   *          the path where to put the directory in the package.
   * @throws IOException
   *           error when adding the directory to package.
   */
  private static void addDirToZip(File directory, ZipOutputStream out, String basePath,
      Collection<String> exportedSkinFiles) throws IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding dir [" + directory.getPath() + "] to the Zip file being generated.");
    }

    if (!directory.isDirectory()) {
      return;
    }

    File[] files = directory.listFiles();

    if (files == null) {
      return;
    }

    byte[] tmpBuf = new byte[1024];

    for (File file : files) {
      if (file.isDirectory()) {
        addDirToZip(file, out, basePath + file.getName() + ZIPPATH_SEPARATOR, exportedSkinFiles);
      } else {
        String path = basePath + file.getName();

        if ((exportedSkinFiles != null) && exportedSkinFiles.contains(path)) {
          continue;
        }

        try (FileInputStream in = new FileInputStream(file)) {
          // Starts a new Zip entry. It automatically closes the previous entry if present.
          out.putNextEntry(new ZipEntry(path));

          try {
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
              out.write(tmpBuf, 0, len);
            }
          } finally {
            out.closeEntry();
          }
        }
      }
    }
  }
}
