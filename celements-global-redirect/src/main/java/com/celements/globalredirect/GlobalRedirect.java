package com.celements.globalredirect;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.xpn.xwiki.web.XWikiResponse;

@Immutable
public class GlobalRedirect {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalRedirect.class);

  private final Predicate<String> predicate;
  private final String destination;
  private final Pattern pattern;

  public GlobalRedirect(String regex, String destination) {
    this.destination = destination;
    Pattern thePattern = null;
    Predicate<String> thePredicate = null;
    try {
      thePattern = Pattern.compile(regex);
      thePredicate = thePattern.asMatchPredicate();
    } catch (PatternSyntaxException exp) {
      LOGGER.info("failed to compile regex {} with dest {}", regex, destination, exp);
    }
    this.pattern = thePattern;
    this.predicate = thePredicate;
  }

  public void sendRedirect(XWikiResponse response, String url) throws IOException {
    Matcher matcher = pattern.matcher(url);
    response.sendRedirect(matcher.replaceAll(destination));
  }

  public boolean test(String url) {
    return predicate.test(url);
  }

  public boolean isValid() {
    return !Strings.isNullOrEmpty(destination) && (pattern != null) && (predicate != null);
  }

}
