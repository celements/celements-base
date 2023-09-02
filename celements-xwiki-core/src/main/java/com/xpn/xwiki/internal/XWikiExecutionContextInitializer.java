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

@Component
public class XWikiExecutionContextInitializer implements ExecutionContextInitializer {

  public static final Property<Boolean> NO_AWAIT = new Property<>(
      "XWikiExecutionContextInitializer.noAwait", Boolean.class);

  @Inject
  private XWikiProvider wikiProvider;

  @Override
  public void initialize(ExecutionContext context) throws ExecutionContextException {
    try {
      context.computeIfAbsent(XWIKI, rethrow(() -> context.get(NO_AWAIT).orElse(false)
          ? wikiProvider.get().orElse(null)
          : wikiProvider.await(Duration.ofHours(1))));
    } catch (ExecutionException xwe) {
      throw new ExecutionContextException("failed initializing XWiki", xwe);
    }
  }

}
