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

package com.xpn.xwiki.user.impl.xwiki;

import static com.google.common.base.Strings.*;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.securityfilter.authenticator.Authenticator;
import org.securityfilter.authenticator.FormAuthenticator;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.filter.URLPatternMatcher;
import org.securityfilter.realm.SimplePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.xwiki.container.servlet.filters.SavedRequestManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class MyFormAuthenticator extends FormAuthenticator implements XWikiAuthenticator {

  private static final Logger LOGGER = LoggerFactory.getLogger(MyFormAuthenticator.class);

  /**
   * Show the login page.
   *
   * @param request
   *          the current request
   * @param response
   *          the current response
   */
  @Override
  public void showLogin(HttpServletRequest request, HttpServletResponse response,
      XWikiContext context)
      throws IOException {
    if ("1".equals(request.getParameter("basicauth"))) {
      String realmName = context.getWiki().Param("xwiki.authentication.realmname");
      if (realmName == null) {
        realmName = "XWiki";
      }
      MyBasicAuthenticator.showLogin(request, response, realmName);
    } else {
      showLogin(request, response);
    }
  }

  @Override
  public void showLogin(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String savedRequestKey = SavedRequestManager.getSavedRequestIdentifier();
    String savedRequestId = request.getParameter(savedRequestKey);
    if (nullToEmpty(savedRequestId).isBlank()) {
      savedRequestId = SavedRequestManager.saveRequest(request);
    }
    var redirectUriBuilder = UriComponentsBuilder.fromUriString(request.getRequestURI())
        .query(nullToEmpty(request.getQueryString())); // null clears the query string
    if (!request.getParameterMap().containsKey(savedRequestKey)) {
      redirectUriBuilder.queryParam(savedRequestKey, savedRequestId);
    }
    String redirectUrl = UriComponentsBuilder.fromUriString(loginPage)
        .queryParam(savedRequestKey, savedRequestId)
        .queryParam("xredirect", redirectUriBuilder.toUriString())
        .build().toUriString();
    LOGGER.trace("showLogin - redirect to {}", redirectUrl);
    response.sendRedirect(redirectUrl);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.securityfilter.authenticator.FormAuthenticator#processLogin(org.securityfilter.filter.SecurityRequestWrapper,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response)
      throws Exception {
    return processLogin(request, response, null);
  }

  private String convertUsername(String username, XWikiContext context) {
    return context.getWiki().convertUsername(username, context);
  }

  /**
   * Process any login information that was included in the request, if any. Returns true if
   * SecurityFilter should
   * abort further processing after the method completes (for example, if a redirect was sent as
   * part of the login
   * processing).
   *
   * @param request
   * @param response
   * @return true if the filter should return after this method ends, false otherwise
   */
  @Override
  public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response,
      XWikiContext context)
      throws Exception {
    try {
      Principal principal = MyBasicAuthenticator.checkLogin(request, response, context);
      LOGGER.trace("processLogin - {}", principal);
      if (principal != null) {
        return false;
      }
      if ("1".equals(request.getParameter("basicauth"))) {
        return true;
      }
    } catch (Exception e) {
      // in case of exception we continue on Form Auth.
      // we don't want this to interfere with the most common behavior
    }

    // process any persistent login information, if user is not already logged in,
    // persistent logins are enabled, and the persistent login info is present in this request
    if (this.persistentLoginManager != null) {
      String username = convertUsername(
          this.persistentLoginManager.getRememberedUsername(request, response), context);
      String password = this.persistentLoginManager.getRememberedPassword(request, response);

      Principal principal = request.getUserPrincipal();

      // 1) if user is not already authenticated, authenticate
      // 2) if authenticated user for this session does not have the same name, authenticate
      // 3) if xwiki.authentication.always is set to 1 in xwiki.cfg file, authenticate
      if ((principal == null) || !StringUtils.endsWith(principal.getName(), "XWiki." + username)
          || (context.getWiki().ParamAsLong("xwiki.authentication.always", 0) == 1)) {
        principal = authenticate(username, password, context);

        if (principal != null) {
          LOGGER.debug("User {} has been authentified from cookie", principal.getName());

          // make sure the Principal contains wiki name information
          if (!StringUtils.contains(principal.getName(), ':')) {
            principal = new SimplePrincipal(context.getDatabase() + ":" + principal.getName());
          }

          request.setUserPrincipal(principal);
        } else {
          // Failed to authenticate, better cleanup the user stored in the session
          request.setUserPrincipal(null);
          if ((username != null) || (password != null)) {
            // Failed authentication with remembered login, better forget login now
            this.persistentLoginManager.forgetLogin(request, response);
          }
        }
      }
    }

    // process login form submittal
    if ((this.loginSubmitPattern != null)
        && request.getMatchableURL().endsWith(this.loginSubmitPattern)) {
      String username = convertUsername(request.getParameter(FORM_USERNAME), context);
      String password = request.getParameter(FORM_PASSWORD);
      String rememberme = request.getParameter(FORM_REMEMBERME);
      rememberme = (rememberme == null) ? "false" : rememberme;
      return processLogin(username, password, rememberme, request, response, context);
    }
    return false;
  }

  /**
   * Process any login information passed in parameter (username, password). Returns true if
   * SecurityFilter should
   * abort further processing after the method completes (for example, if a redirect was sent as
   * part of the login
   * processing).
   *
   * @param request
   * @param response
   * @return true if the filter should return after this method ends, false otherwise
   */
  @Override
  public boolean processLogin(String username, String password, String rememberme,
      SecurityRequestWrapper request,
      HttpServletResponse response, XWikiContext context) throws Exception {
    Principal principal = authenticate(username, password, context);
    if (principal != null) {
      // login successful
      LOGGER.info("User {} has been logged-in", principal.getName());

      // invalidate old session if the user was already authenticated, and they logged in as a
      // different user
      if ((request.getUserPrincipal() != null) && !username.equals(request.getRemoteUser())) {
        request.getSession().invalidate();
      }

      // manage persistent login info, if persistent login management is enabled
      if (this.persistentLoginManager != null) {
        // did the user request that their login be persistent?
        if (rememberme != null) {
          // remember login
          this.persistentLoginManager.rememberLogin(request, response, username, password);
        } else {
          // forget login
          this.persistentLoginManager.forgetLogin(request, response);
        }
      }

      // make sure the Principal contains wiki name information
      if (!StringUtils.contains(principal.getName(), ':')) {
        principal = new SimplePrincipal(context.getDatabase() + ":" + principal.getName());
      }

      request.setUserPrincipal(principal);
      Boolean bAjax = (Boolean) context.get("ajax");
      if ((bAjax == null) || (!bAjax.booleanValue())) {
        String continueToURL = getContinueToURL(request);
        // This is the url that the user was initially accessing before being prompted for login.
        response.sendRedirect(response.encodeRedirectURL(continueToURL));
      }
    } else {
      // login failed
      // set response status and forward to error page
      LOGGER.info("User {} login has failed", username);

      String returnCode = context.getWiki().Param("xwiki.authentication.unauthorized_code");
      int rCode = HttpServletResponse.SC_UNAUTHORIZED;
      if ((returnCode != null) && (!returnCode.equals(""))) {
        try {
          rCode = Integer.parseInt(returnCode);
        } catch (Exception e) {
          rCode = HttpServletResponse.SC_UNAUTHORIZED;
        }
      }
      response.setStatus(rCode); // TODO: Does this work? (200 in case of error)
    }

    return true;
  }

  /**
   * FormAuthenticator has a special case where the user should be sent to a default page if the
   * user spontaneously
   * submits a login request.
   *
   * @param request
   * @return a URL to send the user to after logging in
   */
  private String getContinueToURL(HttpServletRequest request) {
    String savedURL = request.getParameter("xredirect");
    if (StringUtils.isEmpty(savedURL)) {
      savedURL = SavedRequestManager.getOriginalUrl(request);
    }

    if (!StringUtils.isEmpty(savedURL)) {
      return savedURL;
    }
    return this.defaultPage;
  }

  public static Principal authenticate(String username, String password, XWikiContext context)
      throws XWikiException {
    return context.getWiki().getAuthService().authenticate(username, password, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see Authenticator#processLogout(SecurityRequestWrapper, HttpServletResponse,
   *      URLPatternMatcher)
   */
  @Override
  public boolean processLogout(SecurityRequestWrapper securityRequestWrapper,
      HttpServletResponse httpServletResponse, URLPatternMatcher urlPatternMatcher)
      throws Exception {
    boolean result = super.processLogout(securityRequestWrapper, httpServletResponse,
        urlPatternMatcher);
    if (result && (this.persistentLoginManager != null)) {
      this.persistentLoginManager.forgetLogin(securityRequestWrapper, httpServletResponse);
    }
    return result;
  }
}
