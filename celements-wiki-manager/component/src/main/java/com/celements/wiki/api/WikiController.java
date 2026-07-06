package com.celements.wiki.api;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static org.springframework.http.HttpStatus.*;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.xwiki.model.reference.WikiReference;

import com.celements.auth.user.User;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.spring.security.AuthenticatedBaseController;
import com.celements.wiki.WikiCacheRefresher;
import com.celements.wiki.WikiCreator;
import com.celements.wiki.WikiDescriptor;
import com.celements.wiki.WikiDescriptorService;
import com.celements.wiki.WikiService;
import com.celements.wiki.exception.WikiCreationException;
import com.celements.wiki.exception.WikiExistsException;
import com.celements.wiki.exception.WikiMissingException;

@RestController
@RequestMapping("/v1/wikis")
public class WikiController extends AuthenticatedBaseController {

  private final WikiService wikiService;
  private final WikiDescriptorService descriptorService;
  private final WikiCreator wikiCreator;
  private final WikiCacheRefresher wikiCacheRefresher;
  private final IRightsAccessFacadeRole rightsAccess;

  @Inject
  public WikiController(
      WikiService wikiService,
      WikiDescriptorService descriptorService,
      WikiCreator wikiCreator,
      WikiCacheRefresher wikiCacheRefresher,
      IRightsAccessFacadeRole rightsAccess) {
    this.wikiService = wikiService;
    this.descriptorService = descriptorService;
    this.wikiCreator = wikiCreator;
    this.wikiCacheRefresher = wikiCacheRefresher;
    this.rightsAccess = rightsAccess;
  }

  @GetMapping
  public List<WikiDescriptor> getWikis() throws WikiMissingException {
    requireSuperAdmin();
    return wikiService.streamAllWikis()
        .map(rethrowFunction(descriptorService::getDescriptors))
        .flatMap(Collection::stream)
        .toList();
  }

  @GetMapping("/{name}")
  public List<WikiDescriptor> getWiki(@PathVariable String name) throws WikiMissingException {
    requireSuperAdmin();
    var wikiRef = new WikiReference(name);
    return descriptorService.getDescriptors(wikiRef);
  }

  @PostMapping("/{name}")
  public ResponseEntity<List<WikiDescriptor>> createWiki(@PathVariable String name)
      throws WikiCreationException, WikiMissingException {
    requireSuperAdmin();
    var wikiRef = new WikiReference(name);
    wikiCreator.createWiki(wikiRef);
    wikiCacheRefresher.refresh();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(descriptorService.getDescriptors(wikiRef));
  }

  private User requireSuperAdmin() {
    User user = checkAuth().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (!rightsAccess.isSuperAdmin(user)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return user;
  }

  @ExceptionHandler(WikiMissingException.class)
  public ResponseEntity<String> handleException(WikiMissingException e) {
    return toErrorResponse(NOT_FOUND, e);
  }

  @ExceptionHandler(WikiExistsException.class)
  public ResponseEntity<String> handleException(WikiExistsException e) {
    return toErrorResponse(CONFLICT, e);
  }

  @ExceptionHandler(WikiCreationException.class)
  public ResponseEntity<String> handleException(WikiCreationException e) {
    return toErrorResponse(INTERNAL_SERVER_ERROR, e);
  }

}
