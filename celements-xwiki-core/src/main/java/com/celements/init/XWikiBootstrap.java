package com.celements.init;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.slf4j.MDC;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.velocity.VelocityManager;

import com.celements.servlet.CelementsLifecycleEvent;
import com.xpn.xwiki.ServerUrlUtilsRole;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiForm;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiServletResponseStub;

@Component
public class XWikiBootstrap implements ApplicationListener<CelementsLifecycleEvent> {

  private final ServletContext servletContext;

  public XWikiBootstrap(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @Override
  public void onApplicationEvent(CelementsLifecycleEvent event) {
    if ((event.getType() == CelementsLifecycleEvent.State.STARTED)) {
      try {
        String wikiName = XWikiConstant.MAIN_WIKI.getName();
        XWikiContext context = new XWikiContext();
        context.setMode(XWikiContext.MODE_SERVLET);
        context.setEngineContext(new XWikiServletContext(servletContext));
        context.setMainXWiki(wikiName);
        context.setDatabase(wikiName);
        XWiki xwiki = createXWiki(context);
        context.setWiki(xwiki);
        context.setURL(Utils.getComponent(ServerUrlUtilsRole.class)
            .getServerURL(wikiName, context)); // TODO "main" instead of "xwiki" here?
        context.setURLFactory(xwiki.getURLFactoryService().createURLFactory(context));
        Utils.getComponent(XWikiStubContextProvider.class).initialize(context); // TODO needed ?
        // make XWiki available to all requests, see XWiki#getXWiki
        context.getEngineContext().setAttribute(wikiName, xwiki);
      } catch (Exception exc) {
        throw new RuntimeException(exc); // TODO does this actually fail entire servlet?
      }
    }
  }

  private XWiki createXWiki(XWikiContext context) throws XWikiException {
    try (InputStream is = servletContext.getResourceAsStream("/WEB-INF/xwiki.cfg")) {
      // TODO dive into this and move it here
      XWiki xwiki = new XWiki(new XWikiConfig(is), context, context.getEngineContext(), true);
      xwiki.setDatabase(context.getDatabase());
      return xwiki;
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_INIT_FAILED,
          "Could not initialize main XWiki context", e);
    }
  }

  protected XWikiContext initializeXWikiContext(ActionMapping mapping, ActionForm form,
      HttpServletRequest request, HttpServletResponse response)
      throws MalformedURLException, ServletContainerException {
    String action = mapping.getName();
    XWikiContext context = Utils.prepareContext(action,
        new XWikiServletRequest(request),
        new XWikiServletResponse(response),
        new XWikiServletContext(servletContext));
    // This code is already called by struts. However struts will also set all the parameters of the
    // form data directly from the request objects. However because of bug
    // http://jira.xwiki.org/jira/browse/XWIKI-2422 we need to perform encoding of windows-1252
    // chars in ISO mode so we need to make sure this code is called
    // TODO: completely get rid of struts so that we control this part of the code and can reduce
    // drastically the number of calls
    if (form != null) {
      form.reset(mapping, context.getRequest());
    }
    // Add the form to the context
    context.setForm((XWikiForm) form);
    ServletContainerInitializer initializer = Utils.getComponent(ServletContainerInitializer.class);
    initializer.initializeRequest(request, context);
    initializer.initializeResponse(response);
    initializer.initializeSession(request);
    return context;
  }

  Execution initExecutionContext(XWikiContext xwikiContext) throws ExecutionContextException,
      MalformedURLException {
    // Init execution context
    ExecutionContextManager ecim = Utils.getComponent(ExecutionContextManager.class);
    Execution execution = Utils.getComponent(Execution.class);

    ExecutionContext ec = new ExecutionContext();
    XWikiContext scontext = createJobContext(xwikiContext);
    // Bridge with old XWiki Context, required for old code.
    ec.setProperty("xwikicontext", scontext);

    ecim.initialize(ec);
    execution.setContext(ec);

    setupServerUrlAndFactory(scontext, xwikiContext);
    VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
    velocityManager.getVelocityContext();
    return execution;
  }

  void setupServerUrlAndFactory(XWikiContext scontext, XWikiContext xwikiContext)
      throws MalformedURLException {
    // scontext.setDoc(getModelAccess().getDocument(xwikiContext.getDoc().getDocumentReference()));

    final URL url = xwikiContext.getWiki().getServerURL(xwikiContext.getDatabase(), scontext);
    // Push the URL into the slf4j MDC context so that we can display it in the generated logs
    // using the %X{url} syntax.
    MDC.put("url", url.toString());
    scontext.setURL(url);

    com.xpn.xwiki.web.XWikiURLFactory xurf = xwikiContext.getURLFactory();
    if (xurf == null) {
      xurf = scontext.getWiki().getURLFactoryService().createURLFactory(scontext.getMode(),
          scontext);
    }
    scontext.setURLFactory(xurf);
  }

  // TODO vs com.xpn.xwiki.internal.DefaultXWikiStubContextProvider ?
  // TODO vs com.xpn.xwiki.plugin.scheduler.SchedulerPlugin.prepareJobStubContext ?
  public static XWikiContext createJobContext(XWikiContext xwikiContext)
      throws MalformedURLException {
    final XWiki xwiki = xwikiContext.getWiki();
    final String database = xwikiContext.getDatabase();

    // We are sure the context request is a real servlet request
    // So we force the dummy request with the current host
    XWikiServletRequestStub dummy = new XWikiServletRequestStub();
    dummy.setHost(xwikiContext.getRequest().getHeader("x-forwarded-host"));
    dummy.setScheme(xwikiContext.getRequest().getScheme());
    XWikiServletRequest request = new XWikiServletRequest(dummy);

    // Force forged context response to a stub response, since the current context response
    // will not mean anything anymore when running in the scheduler's thread, and can cause
    // errors.
    XWikiResponse response = new XWikiServletResponseStub();

    // IMPORTANT: do NOT clone xwikiContext. You would need to ensure that no reference or
    // unwanted value leaks in the new context.
    // IMPORTANT: following lines base on XWikiRequestInitializer.prepareContext
    XWikiContext scontext = new XWikiContext();
    scontext.setEngineContext(xwikiContext.getEngineContext());
    scontext.setRequest(request);
    scontext.setResponse(response);
    scontext.setAction("view");
    scontext.setDatabase(database);

    // feed the job context
    scontext.setUser(xwikiContext.getUser());
    scontext.setLanguage(xwikiContext.getLanguage());
    scontext.setMainXWiki(xwikiContext.getMainXWiki());
    scontext.setMode(XWikiContext.MODE_SERVLET);

    scontext.setWiki(xwiki);
    scontext.getWiki().getStore().cleanUp(scontext);

    scontext.flushClassCache();
    scontext.flushArchiveCache();

    // scontext.setURLFactory(xurf); TODO

    return scontext;
  }

}
