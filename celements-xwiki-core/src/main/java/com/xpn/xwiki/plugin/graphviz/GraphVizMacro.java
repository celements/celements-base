/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package com.xpn.xwiki.plugin.graphviz;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;

/**
 * <p>
 * Macro used to invoke GraphViz and render its output on a XWiki page.
 * </p>
 * <p>
 * For it to work properly, you must have the GraphViz plugin enabled and configured. See the
 * <tt>xwiki.plugin.graphviz.dotpath</tt> and <tt>xwiki.plugin.graphviz.neatopath</tt> parameters in
 * <tt>xwiki.cfg</tt>.
 * </p>
 * <p>
 * The content of the macro is the source code for the graph, using the <a
 * href="http://www.graphviz.org/doc/info/lang.html">GraphViz DOT language</a>.
 * <p>
 * The parameters are as follows:
 * <ol>
 * <li><b>type</b>: [dot] or neato. Specifies which engine will be used to produce the graph.</li>
 * <li><b>title</b>: Title attribute for the image (floating text box when you hover it). <b>Do not
 * specify it if you
 * want to use GraphViz tooltip attribute</b>. With the tooltip attributes you can generate
 * different text values for
 * different regions of the image.</li>
 * <li><b>height</b>: Image height in pixels.</li>
 * <li><b>width</b>: Image width in pixels.</li>
 * <li><b>alt</b>: Alternative text (alt attribute) for the image, in case it cannot be rendered by
 * the browser.</li>
 * <li><b>format</b>: This attribute specifies what will be GraphViz output and also how it will be
 * rendered in the
 * HTML. <br/>
 * The HTML output of the macro will be one of the following, depending on this attribute:
 * <ul>
 * <li><tt>object</tt> html tag for: svg, svgz.</li>
 * <li>The text as output by GraphViz for: canon, dot, xdot, imap, cmapx, imap_np, cmpax_np, plain,
 * plain-ext. Remember
 * that for some of these formats, like cmpax and cmapx_np, the text will actually be HTML
 * code.</li>
 * <li><tt>img</tt> html tag for anything else. <b>Caution</b>: Not <a
 * href="http://www.graphviz.org/doc/info/output.html">all formats supported by GraphViz</a> can be
 * referenced by an IMG
 * tag. Depending on the format you choose this macro may not produce a valid result.</li>
 * </ul>
 * </li>
 * <li><b>useMap</b>: true or [false]. If present and true, and if the output is an image, indicates
 * that an image link
 * map must be produced by GraphViz ("cmapx" format), and the IMG tag must reference it. Anonymous
 * graphs will provoke
 * errors if this attribute is true, so don't forget to name them. This parameter will have no
 * effect if <i>format</i>
 * specifies anything that doesn't produce an IMG tag to be rendered. SVG images can contain
 * embedded links in the file
 * itself, there's no need for an extra map tag.</li>
 * </ol>
 * </p>
 */
public class GraphVizMacro extends BaseLocaleMacro {

  /**
   * {@inheritDoc}
   *
   * @see org.radeox.macro.LocaleMacro#getLocaleKey()
   */
  @Override
  public String getLocaleKey() {
    return "macro.graphviz";
  }

  /**
   * {@inheritDoc}
   *
   * @see org.radeox.macro.BaseMacro#execute(Writer, MacroParameter)
   */
  @Override
  public void execute(Writer writer, MacroParameter params)
      throws IllegalArgumentException, IOException {
    RenderContext context = params.getContext();
    RenderEngine engine = context.getRenderEngine();

    XWikiContext xcontext = ((XWikiRadeoxRenderEngine) engine).getXWikiContext();
    XWiki xwiki = xcontext.getWiki();

    GraphVizPlugin plugin = (GraphVizPlugin) xwiki.getPlugin("graphviz", xcontext);
    // If the plugin is not loaded
    if (plugin == null) {
      writer.write("Graphviz plugin not loaded");
      return;
    }

    boolean dot = !("neato").equals(params.get("type"));
    String title = nullifyBadParameter(params.get("title", 0));
    String height = nullifyBadParameter(params.get("height", 1));
    String width = nullifyBadParameter(params.get("width", 2));
    String alt = nullifyBadParameter(params.get("alt", 3));
    String format = nullifyBadParameter(params.get("format", 4));
    if (StringUtils.isBlank(format)) {
      format = "png";
    }
    String useMapStr = nullifyBadParameter(params.get("useMap", 5));
    boolean useMap = BooleanUtils.toBoolean(useMapStr);
    String dottext = params.getContent();

    // please KEEP THE ARRAYS SORTED
    final String[] embedTagFormats = { "svg", "svgz" };
    final String[] plainTextFormats = { "canon", "cmapx", "cmapx_np", "dot", "imap",
        "imap_np", "plain", "plain-ext", "xdot" };

    if (Arrays.binarySearch(plainTextFormats, format) >= 0) {
      // Producing plain text output
      byte[] graphvizOutput = plugin.getDotImage(dottext, format, dot);
      writer.write(new String(graphvizOutput));
    } else if (Arrays.binarySearch(embedTagFormats, format) >= 0) {
      // Producing object tag output
      final String resultURL = plugin.getDotResultURL(dottext, dot, format, xcontext);
      writer.write("<object data=\"");
      writer.write(resultURL);
      writer.write("\" type=\"image/svg+xml\" ");
      if (alt != null) {
        writer.write("standby=\"");
        writer.write(alt);
        writer.write("\" ");
      }
      writeDimensionsAntTitle(writer, title, height, width);
      writer.write(">");
      if (alt != null) {
        writer.write(alt);
      }
      writer.write("</object>");
    } else {
      // Producing img tag output
      if (useMap) {
        /* creates the map */
        byte[] graphvizOutput = plugin.getDotImage(dottext, "cmapx", dot);
        writer.write(new String(graphvizOutput));
      }
      final String resultURL = plugin.getDotResultURL(dottext, dot, format, xcontext);
      writer.write("<img ");
      writeImgContent(writer, resultURL, title, height, width, alt);
      if (useMap) {
        /*
         * to know the name of the map we need to extract it from the graph source
         */
        final Matcher matcher = Pattern.compile("(di)?graph\\s+(\\w+)\\s*\\{").matcher(dottext);
        if (!matcher.find()) {
          // closing the img tag so the error can be properly rendered
          writer.write("/>");
          throw new IllegalArgumentException("Macro content is not a DOT graph definition or is an "
              + "anonymous graph (not supported). Content: " + dottext);
        }
        final String graphName = matcher.group(2);
        writer.write(" usemap=\"#");
        writer.write(graphName);
        writer.write('"');
      }
      writer.write("/>");
    }
  }

  private void writeImgContent(Writer writer, String resultURL, String title, String height,
      String width, String alt)
      throws IOException {
    writer.write("src=\"");
    writer.write(resultURL);
    writer.write("\" ");
    writeDimensionsAntTitle(writer, title, height, width);
    if (alt != null) {
      writer.write("alt=\"");
      writer.write(alt);
      writer.write('"');
    }
  }

  private void writeDimensionsAntTitle(Writer writer, String title, String height, String width)
      throws IOException {
    if ((!"none".equals(height)) && (height != null)) {
      writer.write("height=\"");
      writer.write(height);
      writer.write("\" ");
    }
    if ((!"none".equals(width)) && (width != null)) {
      writer.write("width=\"");
      writer.write(width);
      writer.write("\" ");
    }
    if (title != null) {
      writer.write("title=\"");
      writer.write(title);
      writer.write("\" ");
    }
  }

  /**
   * Checks if a parameter came with a '=' sign and return null in the case. Else, returns the
   * parameter itself.
   *
   * @param radeoxParam
   * @return String
   */
  private String nullifyBadParameter(String radeoxParam) {
    return (radeoxParam != null) && (radeoxParam.indexOf('=') >= 0) ? null : radeoxParam;
  }
}
