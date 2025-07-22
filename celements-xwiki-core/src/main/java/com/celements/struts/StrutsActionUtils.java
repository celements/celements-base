package com.celements.struts;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.util.ModuleUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.xpn.xwiki.web.ViewAction;

@Service
public class StrutsActionUtils {

  /**
   * @param request
   *          current HttpServletRequest
   * @param actionPath
   *          the logical path e.g. "/inline" (no ".do", no context path)
   * @return true if there is a matching <action path="inline"> in this module
   */
  public boolean isActionDefined(@NotNull HttpServletRequest request, @NotEmpty String actionPath) {
    ServletContext context = request.getServletContext();
    // pick up the ModuleConfig for this request
    ModuleConfig moduleConfig = ModuleUtils.getInstance().getModuleConfig(request, context);
    if (moduleConfig == null) {
      return false;
    }
    ActionConfig cfg = moduleConfig.findActionConfig(actionPath);
    return (cfg != null);
  }

  public @NotEmpty String getActionForRequest(@NotNull HttpServletRequest request) {
    String[] urlParts = StringUtils.tokenizeToStringArray(request.getRequestURI(), "/");
    if ((urlParts.length > 2) && isActionDefined(request, urlParts[0])) {
      return urlParts[0];
    }
    return ViewAction.VIEW_ACTION;
  }
}
