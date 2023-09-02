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

package com.xpn.xwiki.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oro.text.PatternCache;
import org.apache.oro.text.PatternCacheLRU;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Matcher;
import org.springframework.core.io.ResourceLoader;
import org.xwiki.xml.internal.XMLScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.WikiSubstitution;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

public class Util {

  /**
   * Encoding used for URL encoding/decoding. UTF-8 is the default encoding in RFC 3986, and is
   * recommended by the W3
   * consortium.
   */
  private static final String URL_ENCODING = "UTF-8";

  private static final Log LOG = LogFactory.getLog(Util.class);

  private static PatternCache patterns = new PatternCacheLRU(200);

  private Perl5Matcher matcher = new Perl5Matcher();

  private Perl5Util p5util = new Perl5Util(getPatterns());

  public String substitute(String pattern, String text) {
    return getP5util().substitute(pattern, text);
  }

  public boolean match(String pattern, String text) {
    return getP5util().match(pattern, text);
  }

  public boolean matched() {
    return (getP5util().getMatch() != null);
  }

  public String substitute(String pattern, String substitution, String text) {
    WikiSubstitution subst = new WikiSubstitution(this, pattern);
    subst.setSubstitution(substitution);
    return subst.substitute(text);
  }

  public Perl5Matcher getMatcher() {
    return this.matcher;
  }

  public Perl5Util getP5util() {
    return this.p5util;
  }

  public List<String> getAllMatches(String content, String spattern, int group)
      throws MalformedPatternException {
    List<String> list = new ArrayList<>();
    PatternMatcherInput input = new PatternMatcherInput(content);
    Pattern pattern = patterns.addPattern(spattern);
    while (this.matcher.contains(input, pattern)) {
      MatchResult result = this.matcher.getMatch();
      String smatch = result.group(group);
      list.add(smatch);
    }

    return list;
  }

  public List<String> getUniqueMatches(String content, String spattern, int group)
      throws MalformedPatternException {
    // Remove duplicate entries
    Set<String> uniqueMatches = new HashSet<>(getAllMatches(content, spattern, group));
    List<String> matches = new ArrayList<>(uniqueMatches);
    return matches;
  }

  /**
   * @deprecated use {@link #getUniqueMatches(String, String, int)} instead
   */
  @Deprecated
  public List<String> getMatches(String content, String spattern, int group)
      throws MalformedPatternException {
    return getUniqueMatches(content, spattern, group);
  }

  public static String cleanValue(String value) {
    value = StringUtils.replace(value, "\r\r\n", "%_N_%");
    value = StringUtils.replace(value, "\r\n", "%_N_%");
    value = StringUtils.replace(value, "\n\r", "%_N_%");
    value = StringUtils.replace(value, "\r", "\n");
    value = StringUtils.replace(value, "\n", "%_N_%");
    return StringUtils.replace(value, "\"", "%_Q_%");
  }

  public static String restoreValue(String value) {
    value = StringUtils.replace(value, "%_N_%", "\n");
    return StringUtils.replace(value, "%_Q_%", "\"");
  }

  public static PatternCache getPatterns() {
    return patterns;
  }

  public static Map<String, String[]> getObject(XWikiRequest request, String prefix) {
    @SuppressWarnings("unchecked")
    Map<String, String[]> parameters = request.getParameterMap();
    return getSubMap(parameters, prefix);
  }

  public static <T> Map<String, T> getSubMap(Map<String, T> map, String prefix) {
    Map<String, T> result = new HashMap<>();
    for (String name : map.keySet()) {
      if (name.startsWith(prefix + "_")) {
        String newname = name.substring(prefix.length() + 1);
        result.put(newname, map.get(name));
      } else if (name.equals(prefix)) {
        result.put("", map.get(name));
      }
    }

    return result;
  }

  public static String getFileContent(File file) throws IOException {
    return FileUtils.readFileToString(file);
  }

  public static String getFileContent(Reader reader) throws IOException {
    return IOUtils.toString(reader);
  }

  public static byte[] getFileContentAsBytes(File file) throws IOException {
    return FileUtils.readFileToByteArray(file);
  }

  public static byte[] getFileContentAsBytes(InputStream is) throws IOException {
    return IOUtils.toByteArray(is);
  }

  public static boolean contains(String name, String list, String sep) {
    String[] sarray = StringUtils.split(list, sep);

    return ArrayUtils.contains(sarray, name);
  }

  public static String noaccents(String text) {
    String temp = text;
    temp = temp.replace("\u00c0", "A");
    temp = temp.replace("\u00c1", "A");
    temp = temp.replace("\u00c2", "A");
    temp = temp.replace("\u00c3", "A");
    temp = temp.replace("\u00c4", "A");
    temp = temp.replace("\u00c5", "A");
    temp = temp.replace("\u0100", "A");
    temp = temp.replace("\u0102", "A");
    temp = temp.replace("\u0104", "A");
    temp = temp.replace("\u01cd", "A");
    temp = temp.replace("\u01de", "A");
    temp = temp.replace("\u01e0", "A");
    temp = temp.replace("\u01fa", "A");
    temp = temp.replace("\u0200", "A");
    temp = temp.replace("\u0202", "A");
    temp = temp.replace("\u0226", "A");
    temp = temp.replace("\u00e0", "a");
    temp = temp.replace("\u00e1", "a");
    temp = temp.replace("\u00e2", "a");
    temp = temp.replace("\u00e3", "a");
    temp = temp.replace("\u00e4", "a");
    temp = temp.replace("\u00e5", "a");
    temp = temp.replace("\u0101", "a");
    temp = temp.replace("\u0103", "a");
    temp = temp.replace("\u0105", "a");
    temp = temp.replace("\u01ce", "a");
    temp = temp.replace("\u01df", "a");
    temp = temp.replace("\u01e1", "a");
    temp = temp.replace("\u01fb", "a");
    temp = temp.replace("\u0201", "a");
    temp = temp.replace("\u0203", "a");
    temp = temp.replace("\u0227", "a");
    temp = temp.replace("\u00c6", "AE");
    temp = temp.replace("\u01e2", "AE");
    temp = temp.replace("\u01fc", "AE");
    temp = temp.replace("\u00e6", "ae");
    temp = temp.replace("\u01e3", "ae");
    temp = temp.replace("\u01fd", "ae");
    temp = temp.replace("\u008c", "OE");
    temp = temp.replace("\u0152", "OE");
    temp = temp.replace("\u009c", "oe");
    temp = temp.replace("\u0153", "oe");
    temp = temp.replace("\u00c7", "C");
    temp = temp.replace("\u0106", "C");
    temp = temp.replace("\u0108", "C");
    temp = temp.replace("\u010a", "C");
    temp = temp.replace("\u010c", "C");
    temp = temp.replace("\u00e7", "c");
    temp = temp.replace("\u0107", "c");
    temp = temp.replace("\u0109", "c");
    temp = temp.replace("\u010b", "c");
    temp = temp.replace("\u010d", "c");
    temp = temp.replace("\u00d0", "D");
    temp = temp.replace("\u010e", "D");
    temp = temp.replace("\u0110", "D");
    temp = temp.replace("\u00f0", "d");
    temp = temp.replace("\u010f", "d");
    temp = temp.replace("\u0111", "d");
    temp = temp.replace("\u00c8", "E");
    temp = temp.replace("\u00c9", "E");
    temp = temp.replace("\u00ca", "E");
    temp = temp.replace("\u00cb", "E");
    temp = temp.replace("\u0112", "E");
    temp = temp.replace("\u0114", "E");
    temp = temp.replace("\u0116", "E");
    temp = temp.replace("\u0118", "E");
    temp = temp.replace("\u011a", "E");
    temp = temp.replace("\u0204", "E");
    temp = temp.replace("\u0206", "E");
    temp = temp.replace("\u0228", "E");
    temp = temp.replace("\u00e8", "e");
    temp = temp.replace("\u00e9", "e");
    temp = temp.replace("\u00ea", "e");
    temp = temp.replace("\u00eb", "e");
    temp = temp.replace("\u0113", "e");
    temp = temp.replace("\u0115", "e");
    temp = temp.replace("\u0117", "e");
    temp = temp.replace("\u0119", "e");
    temp = temp.replace("\u011b", "e");
    temp = temp.replace("\u01dd", "e");
    temp = temp.replace("\u0205", "e");
    temp = temp.replace("\u0207", "e");
    temp = temp.replace("\u0229", "e");
    temp = temp.replace("\u011c", "G");
    temp = temp.replace("\u011e", "G");
    temp = temp.replace("\u0120", "G");
    temp = temp.replace("\u0122", "G");
    temp = temp.replace("\u01e4", "G");
    temp = temp.replace("\u01e6", "G");
    temp = temp.replace("\u01f4", "G");
    temp = temp.replace("\u011d", "g");
    temp = temp.replace("\u011f", "g");
    temp = temp.replace("\u0121", "g");
    temp = temp.replace("\u0123", "g");
    temp = temp.replace("\u01e5", "g");
    temp = temp.replace("\u01e7", "g");
    temp = temp.replace("\u01f5", "g");
    temp = temp.replace("\u0124", "H");
    temp = temp.replace("\u0126", "H");
    temp = temp.replace("\u021e", "H");
    temp = temp.replace("\u0125", "h");
    temp = temp.replace("\u0127", "h");
    temp = temp.replace("\u021f", "h");
    temp = temp.replace("\u00cc", "I");
    temp = temp.replace("\u00cd", "I");
    temp = temp.replace("\u00ce", "I");
    temp = temp.replace("\u00cf", "I");
    temp = temp.replace("\u0128", "I");
    temp = temp.replace("\u012a", "I");
    temp = temp.replace("\u012c", "I");
    temp = temp.replace("\u012e", "I");
    temp = temp.replace("\u0130", "I");
    temp = temp.replace("\u01cf", "I");
    temp = temp.replace("\u0208", "I");
    temp = temp.replace("\u020a", "I");
    temp = temp.replace("\u00ec", "i");
    temp = temp.replace("\u00ed", "i");
    temp = temp.replace("\u00ee", "i");
    temp = temp.replace("\u00ef", "i");
    temp = temp.replace("\u0129", "i");
    temp = temp.replace("\u012b", "i");
    temp = temp.replace("\u012d", "i");
    temp = temp.replace("\u012f", "i");
    temp = temp.replace("\u0131", "i");
    temp = temp.replace("\u01d0", "i");
    temp = temp.replace("\u0209", "i");
    temp = temp.replace("\u020b", "i");
    temp = temp.replace("\u0132", "IJ");
    temp = temp.replace("\u0133", "ij");
    temp = temp.replace("\u0134", "J");
    temp = temp.replace("\u0135", "j");
    temp = temp.replace("\u0136", "K");
    temp = temp.replace("\u01e8", "K");
    temp = temp.replace("\u0137", "k");
    temp = temp.replace("\u0138", "k");
    temp = temp.replace("\u01e9", "k");
    temp = temp.replace("\u0139", "L");
    temp = temp.replace("\u013b", "L");
    temp = temp.replace("\u013d", "L");
    temp = temp.replace("\u013f", "L");
    temp = temp.replace("\u0141", "L");
    temp = temp.replace("\u013a", "l");
    temp = temp.replace("\u013c", "l");
    temp = temp.replace("\u013e", "l");
    temp = temp.replace("\u0140", "l");
    temp = temp.replace("\u0142", "l");
    temp = temp.replace("\u0234", "l");
    temp = temp.replace("\u00d1", "N");
    temp = temp.replace("\u0143", "N");
    temp = temp.replace("\u0145", "N");
    temp = temp.replace("\u0147", "N");
    temp = temp.replace("\u014a", "N");
    temp = temp.replace("\u01f8", "N");
    temp = temp.replace("\u00f1", "n");
    temp = temp.replace("\u0144", "n");
    temp = temp.replace("\u0146", "n");
    temp = temp.replace("\u0148", "n");
    temp = temp.replace("\u0149", "n");
    temp = temp.replace("\u014b", "n");
    temp = temp.replace("\u01f9", "n");
    temp = temp.replace("\u0235", "n");
    temp = temp.replace("\u00d2", "O");
    temp = temp.replace("\u00d3", "O");
    temp = temp.replace("\u00d4", "O");
    temp = temp.replace("\u00d5", "O");
    temp = temp.replace("\u00d6", "O");
    temp = temp.replace("\u00d8", "O");
    temp = temp.replace("\u014c", "O");
    temp = temp.replace("\u014e", "O");
    temp = temp.replace("\u0150", "O");
    temp = temp.replace("\u01d1", "O");
    temp = temp.replace("\u01ea", "O");
    temp = temp.replace("\u01ec", "O");
    temp = temp.replace("\u01fe", "O");
    temp = temp.replace("\u020c", "O");
    temp = temp.replace("\u020e", "O");
    temp = temp.replace("\u022a", "O");
    temp = temp.replace("\u022c", "O");
    temp = temp.replace("\u022e", "O");
    temp = temp.replace("\u0230", "O");
    temp = temp.replace("\u00f2", "o");
    temp = temp.replace("\u00f3", "o");
    temp = temp.replace("\u00f4", "o");
    temp = temp.replace("\u00f5", "o");
    temp = temp.replace("\u00f6", "o");
    temp = temp.replace("\u00f8", "o");
    temp = temp.replace("\u014d", "o");
    temp = temp.replace("\u014f", "o");
    temp = temp.replace("\u0151", "o");
    temp = temp.replace("\u01d2", "o");
    temp = temp.replace("\u01eb", "o");
    temp = temp.replace("\u01ed", "o");
    temp = temp.replace("\u01ff", "o");
    temp = temp.replace("\u020d", "o");
    temp = temp.replace("\u020f", "o");
    temp = temp.replace("\u022b", "o");
    temp = temp.replace("\u022d", "o");
    temp = temp.replace("\u022f", "o");
    temp = temp.replace("\u0231", "o");
    temp = temp.replace("\u0156", "R");
    temp = temp.replace("\u0158", "R");
    temp = temp.replace("\u0210", "R");
    temp = temp.replace("\u0212", "R");
    temp = temp.replace("\u0157", "r");
    temp = temp.replace("\u0159", "r");
    temp = temp.replace("\u0211", "r");
    temp = temp.replace("\u0213", "r");
    temp = temp.replace("\u015a", "S");
    temp = temp.replace("\u015c", "S");
    temp = temp.replace("\u015e", "S");
    temp = temp.replace("\u0160", "S");
    temp = temp.replace("\u0218", "S");
    temp = temp.replace("\u015b", "s");
    temp = temp.replace("\u015d", "s");
    temp = temp.replace("\u015f", "s");
    temp = temp.replace("\u0161", "s");
    temp = temp.replace("\u0219", "s");
    temp = temp.replace("\u00de", "T");
    temp = temp.replace("\u0162", "T");
    temp = temp.replace("\u0164", "T");
    temp = temp.replace("\u0166", "T");
    temp = temp.replace("\u021a", "T");
    temp = temp.replace("\u00fe", "t");
    temp = temp.replace("\u0163", "t");
    temp = temp.replace("\u0165", "t");
    temp = temp.replace("\u0167", "t");
    temp = temp.replace("\u021b", "t");
    temp = temp.replace("\u0236", "t");
    temp = temp.replace("\u00d9", "U");
    temp = temp.replace("\u00da", "U");
    temp = temp.replace("\u00db", "U");
    temp = temp.replace("\u00dc", "U");
    temp = temp.replace("\u0168", "U");
    temp = temp.replace("\u016a", "U");
    temp = temp.replace("\u016c", "U");
    temp = temp.replace("\u016e", "U");
    temp = temp.replace("\u0170", "U");
    temp = temp.replace("\u0172", "U");
    temp = temp.replace("\u01d3", "U");
    temp = temp.replace("\u01d5", "U");
    temp = temp.replace("\u01d7", "U");
    temp = temp.replace("\u01d9", "U");
    temp = temp.replace("\u01db", "U");
    temp = temp.replace("\u0214", "U");
    temp = temp.replace("\u0216", "U");
    temp = temp.replace("\u00f9", "u");
    temp = temp.replace("\u00fa", "u");
    temp = temp.replace("\u00fb", "u");
    temp = temp.replace("\u00fc", "u");
    temp = temp.replace("\u0169", "u");
    temp = temp.replace("\u016b", "u");
    temp = temp.replace("\u016d", "u");
    temp = temp.replace("\u016f", "u");
    temp = temp.replace("\u0171", "u");
    temp = temp.replace("\u0173", "u");
    temp = temp.replace("\u01d4", "u");
    temp = temp.replace("\u01d6", "u");
    temp = temp.replace("\u01d8", "u");
    temp = temp.replace("\u01da", "u");
    temp = temp.replace("\u01dc", "u");
    temp = temp.replace("\u0215", "u");
    temp = temp.replace("\u0217", "u");
    temp = temp.replace("\u0174", "W");
    temp = temp.replace("\u0175", "w");
    temp = temp.replace("\u00dd", "Y");
    temp = temp.replace("\u0176", "Y");
    temp = temp.replace("\u0178", "Y");
    temp = temp.replace("\u0232", "Y");
    temp = temp.replace("\u00fd", "y");
    temp = temp.replace("\u00ff", "y");
    temp = temp.replace("\u0177", "y");
    temp = temp.replace("\u0233", "y");
    temp = temp.replace("\u0179", "Z");
    temp = temp.replace("\u017b", "Z");
    temp = temp.replace("\u017d", "Z");
    temp = temp.replace("\u017a", "z");
    temp = temp.replace("\u017c", "z");
    temp = temp.replace("\u017e", "z");
    return temp.replace("\u00df", "ss");
  }

  public static boolean isAlphaNumeric(String text) {
    return StringUtils.isAlphanumeric(text.replace("-", "a").replace(".", "a"));
  }

  public static String getName(String name) {
    int i0 = name.indexOf(":");
    if (i0 != -1) {
      return name.substring(i0 + 1);
    }

    if (name.indexOf(".") != -1) {
      return name;
    } else {
      return "XWiki." + name;
    }
  }

  public static String getName(String name, XWikiContext context) {
    String database = null;
    int i0 = name.indexOf(":");
    if (i0 != -1) {
      database = name.substring(0, i0);
      name = name.substring(i0 + 1);
      context.setDatabase(database);
      return name;
    }
    if (name.indexOf(".") != -1) {
      return name;
    } else {
      return "XWiki." + name;
    }
  }

  public static Cookie getCookie(String cookieName, XWikiContext context) {
    return getCookie(cookieName, context.getRequest());
  }

  public static Cookie getCookie(String cookieName, HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookieName.equals(cookie.getName())) {
          return (cookie);
        }
      }
    }

    return null;
  }

  public static String getHTMLExceptionMessage(XWikiException xe, XWikiContext context) {
    String title = XMLScriptService.escape(xe.getMessage());
    String text = XMLScriptService.escape(xe.getFullMessage());
    String id = (String) context.get("xwikierrorid");
    if (id == null) {
      id = "1";
    } else {
      id = "" + (Integer.parseInt(id) + 1);
    }

    return "<a href=\"\" onclick=\"document.getElementById('xwikierror" + id
        + "').style.display='block'; return false;\">" + title + "</a><div id=\"xwikierror" + id
        + "\" style=\"display: none;\"><pre class=\"xwikierror\">\n" + text + "</pre></div>";
  }

  /**
   * API to protect Text from Radeox transformation
   *
   * @param text
   * @return escaped text
   */
  public static String escapeText(String text) {
    text = text.replace("http://", "&#104;ttp://");
    text = text.replace("ftp://", "&#102;tp://");
    text = text.replace("-", "&#45;");
    text = text.replace("*", "&#42;");
    text = text.replace("~", "&#126;");
    text = text.replace("[", "&#91;");
    text = text.replace("]", "&#93;");
    text = text.replace("{", "&#123;");
    text = text.replace("}", "&#125;");
    return text.replace("1", "&#49;");
  }

  /**
   * API to protect URLs from Radeox transformation
   *
   * @param url
   * @return encoded URL
   */
  public static String escapeURL(String url) {
    url = url.replace("~", "%7E");
    url = url.replace("[", "%5B");
    url = url.replace("]", "%5D");
    url = url.replace("{", "%7B");
    url = url.replace("}", "%7D");
    // We should not encode the following char for non local urls
    // since this might not be handle correctly by FF
    if (url.indexOf("//") == -1) {
      url = url.replace("-", "%2D");
      url = url.replace("*", "%2A");
    }

    return url;
  }

  /**
   * Translates a string into <code>application/x-www-form-urlencoded</code> format, so that it can
   * be safely used in
   * URIs, as a parameter value in a query string or as a segment in the URI path. This uses the
   * UTF-8 encoding, the
   * default encoding for URIs, as stated in
   * <a href="http://tools.ietf.org/html/rfc3986#section-2.5">RFC 3986</a>.
   *
   * @param text
   *          the non encoded text
   * @param context
   *          the current context
   * @return encoded text
   * @see #decodeURI(String, XWikiContext)
   */
  public static String encodeURI(String text, XWikiContext context) {
    try {
      return URLEncoder.encode(text, URL_ENCODING);
    } catch (Exception e) {
      // Should not happen (UTF-8 is always available), but if so, fail securely
      return null;
    }
  }

  /**
   * Decodes a <code>application/x-www-form-urlencoded</code> string, the reverse of
   * {@link #encodeURI(String, XWikiContext)}. This uses the UTF-8 encoding, the default encoding
   * for URIs, as
   * stated in <a href="http://tools.ietf.org/html/rfc3986#section-2.5">RFC 3986</a>.
   *
   * @param text
   *          the encoded text
   * @param context
   *          the current context
   * @return decoded text
   * @see #encodeURI(String, XWikiContext)
   */
  public static String decodeURI(String text, XWikiContext context) {
    try {
      return URLDecoder.decode(text, URL_ENCODING);
    } catch (Exception e) {
      // Should not happen (UTF-8 is always available)
      return text;
    }
  }

  /**
   * Removes all non alpha numerical characters from the passed text. First tries to convert
   * accented chars to their
   * alpha numeric representation.
   *
   * @param text
   *          the text to convert
   * @return the alpha numeric equivalent
   */
  public static String convertToAlphaNumeric(String text) {
    // Start by removing accents
    String textNoAccents = Util.noaccents(text);

    // Now remove all non alphanumeric chars
    StringBuffer result = new StringBuffer(textNoAccents.length());
    char[] testChars = textNoAccents.toCharArray();
    for (char testChar : testChars) {
      if (Character.isLetterOrDigit(testChar) && (testChar < 128)) {
        result.append(testChar);
      }
    }

    return result.toString();
  }

  public static Date getFileLastModificationDate(String path) {
    try {
      File f = new File(path);

      return (new Date(f.lastModified()));
    } catch (Exception ex) {
      return new Date();
    }
  }

  /**
   * Validate a XML element name. XML elements must follow these naming rules :
   * <ul>
   * <li>Names can contain letters, numbers, and the following characters [., -, _, :].</li>
   * <li>Names must not start with a number or punctuation character.</li>
   * <li>Names must not start with the letters xml (or XML, or Xml, etc).</li>
   * <li>Names cannot contain spaces.</li>
   * </ul>
   *
   * @param elementName
   *          the XML element name to validate
   * @return true if the element name is valid, false if it is not
   */
  public static boolean isValidXMLElementName(String elementName) {
    if ((elementName == null) || elementName.equals("") || elementName.matches("(?i)^(xml).*")
        || !elementName.matches("(^[a-zA-Z\\-\\_]+[\\w\\.\\-\\_\\:]*$)")) {
      return false;
    }

    return true;
  }

  /**
   * Load resources from: 1. FileSystem 2. ServletContext 3. ClassPath in this order.
   *
   * @param resource
   *          resource path to load
   * @return InputStream of resource or null if not found
   */
  public static InputStream getResourceAsStream(String resource) {
    File file = new File(resource);
    try {
      if (file.exists()) {
        return new FileInputStream(file);
      }
    } catch (Exception e) {
      // Probably running under -security, which prevents calling File.exists()
      LOG.debug("Failed load resource [" + resource + "] using a file path");
    }
    try {
      return Utils.getComponent(ResourceLoader.class)
          .getResource(resource)
          .getInputStream();
    } catch (Exception e) {
      LOG.debug("Failed to load resource [" + resource + "] using the application context");
    }

    return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
  }

  /**
   * Normalize the given language code. Converts the given language code to lower case and checks
   * its validity (i.e.
   * whether it is an ISO 639 language code or the string "default").
   * <p>
   *
   * <pre>
   * Util.normalizeLanguage(null)      = null
   * Util.normalizeLanguage("")        = ""
   * Util.normalizeLanguage("  ")      = ""
   * Util.normalizeLanguage("default") = "default"
   * Util.normalizeLanguage("DeFault") = "default"
   * Util.normalizeLanguage("invalid") = "default"
   * Util.normalizeLanguage("en")      = "en"
   * Util.normalizeLanguage("DE_at")   = "de_AT"
   * </pre>
   * </p>
   *
   * @param languageCode
   *          the language code to normalize
   * @return normalized language code or the string "default" if the code is invalid
   */
  public static String normalizeLanguage(String languageCode) {
    if (languageCode == null) {
      return null;
    }
    if (StringUtils.isBlank(languageCode)) {
      return "";
    }
    // handle language_COUNTRY case
    final String separator = "_";
    String[] parts = languageCode.toLowerCase().split(separator);
    String result = parts[0];
    if (parts.length > 1) {
      parts[1] = parts[1].toUpperCase();
      // NOTE cannot use Locale#toString(), because it would change some language codes
      result = parts[0] + separator + parts[1];
    }
    // handle the "default" case
    final String defaultLanguage = "default";
    if (defaultLanguage.equals(result)) {
      return defaultLanguage;
    }
    try {
      Locale l = new Locale(parts[0], parts.length > 1 ? parts[1] : "");
      // Will throw an exception if the language code is not valid
      l.getISO3Language();
      return result;
    } catch (MissingResourceException ex) {
      LOG.warn("Invalid language: " + languageCode);
    }
    return defaultLanguage;
  }
}
