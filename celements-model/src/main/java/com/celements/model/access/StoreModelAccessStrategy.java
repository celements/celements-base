package com.celements.model.access;

import static com.celements.common.MoreObjectsCel.*;
import static com.celements.common.lambda.LambdaExceptionUtil.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.Contextualiser;
import com.celements.model.context.ModelContext;
import com.celements.store.ModelAccessStore;
import com.celements.store.StoreFactory;
import com.google.common.base.Suppliers;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Implementation of {@link ModelAccessStrategy} only accessing {@link XWikiStoreInterface}.
 *
 * @author Marc Sladek
 */
@Component
public class StoreModelAccessStrategy implements ModelAccessStrategy {

  @Requirement
  protected ModelContext context;

  @Requirement
  protected XWikiDocumentCreator docCreator;

  @Requirement
  protected ConfigurationSource cfgSrc;

  private final Supplier<XWikiStoreInterface> mainStore = Suppliers
      .memoize(StoreFactory::getMainStore);

  private final Supplier<Optional<XWikiRecycleBinStoreInterface>> recycleBinStore = Suppliers
      .memoize(StoreFactory::getRecycleBinStore);

  private XWikiStoreInterface getStore() {
    return tryCast(mainStore.get(), ModelAccessStore.class)
        .map(ModelAccessStore::getBackingStore)
        .orElse(mainStore.get());
  }

  @Override
  public boolean exists(final DocumentReference docRef) {
    try {
      XWikiDocument doc = docCreator.createWithoutDefaults(docRef);
      return new Contextualiser()
          .withWiki(docRef.getWikiReference())
          .execute(rethrow(() -> getStore().exists(doc, getXContext())));
    } catch (XWikiException xwe) {
      throw new DocumentLoadException(docRef, xwe);
    }
  }

  @Override
  public XWikiDocument getDocument(final DocumentReference docRef, final String lang) {
    try {
      XWikiDocument doc = docCreator.createWithoutDefaults(docRef, lang);
      return new Contextualiser()
          .withWiki(docRef.getWikiReference())
          .execute(rethrow(() -> getStore().loadXWikiDoc(doc, getXContext())));
    } catch (XWikiException xwe) {
      throw new DocumentLoadException(docRef, xwe);
    }
  }

  @Override
  public void saveDocument(final XWikiDocument doc) throws DocumentSaveException {
    DocumentReference docRef = doc.getDocumentReference();
    try {
      new Contextualiser()
          .withWiki(docRef.getWikiReference())
          .execute(rethrow(() -> getStore().saveXWikiDoc(doc, getXContext())));
    } catch (XWikiException xwe) {
      throw new DocumentSaveException(docRef, xwe);
    }
  }

  @Override
  public void deleteDocument(final XWikiDocument doc, final boolean totrash)
      throws DocumentDeleteException {
    DocumentReference docRef = doc.getDocumentReference();
    try {
      new Contextualiser()
          .withWiki(docRef.getWikiReference())
          .execute(rethrow(() -> {
            recycleBinStore.get()
                .filter(store -> totrash)
                .ifPresent(rethrowConsumer(store -> store.saveToRecycleBin(
                    doc, context.getUserName(), new Date(), getXContext(), true)));
            getStore().deleteXWikiDoc(doc, getXContext());
          }));
    } catch (XWikiException xwe) {
      throw new DocumentDeleteException(docRef, xwe);
    }
  }

  @Override
  public List<String> getTranslations(final DocumentReference docRef) {
    try {
      XWikiDocument doc = docCreator.createWithoutDefaults(docRef);
      return new Contextualiser()
          .withWiki(docRef.getWikiReference())
          .execute(rethrow(() -> getStore().getTranslationList(doc, getXContext())));
    } catch (XWikiException xwe) {
      throw new DocumentLoadException(docRef, xwe);
    }
  }

  private XWikiContext getXContext() {
    return context.getXWikiContext();
  }
}
