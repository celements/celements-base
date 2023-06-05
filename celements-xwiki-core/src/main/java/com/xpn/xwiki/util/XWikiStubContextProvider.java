package com.xpn.xwiki.util;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWikiContext;

/**
 * Tool to create a stub XWikiContext. We call it a stub because it doesn't contain actual request
 * or response data. It's created from scratch on every call to avoid deep cloning or memory
 * poisoning via uncloned context content.
 *
 */
@ComponentRole
public interface XWikiStubContextProvider {

  @NotNull
  XWikiContext createStubContext(@NotNull ExecutionContext execContext);

}
