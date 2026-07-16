package com.xpn.xwiki.internal;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.celements.execution.XWikiExecutionProp.*;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContext.Property;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

import com.celements.init.XWikiProvider;
import com.xpn.xwiki.XWikiContext;

@Component
public class XWikiExecutionContextInitializer implements ExecutionContextInitializer {

  public static final Property<Boolean> NO_AWAIT = new Property<>(
      "XWikiExecutionContextInitializer.noAwait", Boolean.class);

  private final XWikiProvider wikiProvider;

  @Inject
  public XWikiExecutionContextInitializer(XWikiProvider wikiProvider) {
    this.wikiProvider = wikiProvider;
  }

  @Override
  public void initialize(ExecutionContext context, ExecutionContext source)
      throws ExecutionContextException {
    copyProps(context, source);
    try {
      context.computeIfAbsent(XWIKI, rethrow(() -> context.get(NO_AWAIT).orElse(false)
          ? wikiProvider.get().orElse(null)
          : wikiProvider.await(Duration.ofHours(1))));
    } catch (ExecutionException xwe) {
      throw new ExecutionContextException("failed initializing XWiki", xwe);
    }
  }

  private void copyProps(ExecutionContext context, ExecutionContext source) {
    if (source == null) {
      return;
    }
    copyProp(WIKI, source, context);
    copyProp(DOC, source, context);
    copyProp(XWIKI, source, context);
    copyProp(XWIKI_USER, source, context);
    copyProp(XWIKI_REQUEST_URI, source, context);
    copyProp(XWIKI_REQUEST_ACTION, source, context);
    copyProp(XWIKI_REQUEST, source, context);
    copyProp(XWIKI_RESPONSE, source, context);
    source.get(XWIKI_CONTEXT)
        .map(XWikiContext::clone)
        .ifPresent(xCtx -> context.set(XWIKI_CONTEXT, xCtx));
  }

  private <T> void copyProp(Property<T> property, ExecutionContext source,
      ExecutionContext target) {
    source.get(property).ifPresent(value -> target.set(property, value));
  }

}
