package com.celements.atlas.store;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.atlas.store.feign.DocumentDto;
import com.celements.atlas.store.feign.DocumentStoreClient;
import com.celements.store.DelegateStore;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;

@Component("AtlasStore")
public class AtlasDocumentStore extends DelegateStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtlasDocumentStore.class);

    public static final String NAME = "AtlasStore";

    private final ConfigurationSource cfgSource;

    @Inject
    public AtlasDocumentStore(ConfigurationSource cfgSource) {
        super();
        this.cfgSource = cfgSource;
    }

    @Override
    protected String getName() {
        return NAME;
    }

    public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context
        ) throws XWikiException {
        if ("AtlasTestDocs".equals(getSpaceName(doc))) {
            LOGGER.info("AtlasStore load for {}", doc.getDocRef());
            Optional<DocumentDto> atlasDocOpt = getAtlasDoc(doc.getDocRef().getName());
            // TODO convert to XWikiDocument
        } else {
            LOGGER.info("AtlasStore delegate load for {}", doc.getDocRef());
        }
        return this.getBackingStore().loadXWikiDoc(doc, context);
    }

    public boolean exists(XWikiDocument doc, XWikiContext context
    ) throws XWikiException {
        if ("AtlasTestDocs".equals(getSpaceName(doc))) {
            LOGGER.info("AtlasStore exists check for {}", doc.getDocRef());
            Optional<DocumentDto> atlasDocOpt = getAtlasDoc(doc.getDocRef().getName());
            LOGGER.debug("AtlasStore exists check for {} returning {}",
                doc.getDocRef(), atlasDocOpt.isPresent());
            return atlasDocOpt.isPresent();
        } else {
            LOGGER.info("AtlasStore delegate exists check for {}", doc.getDocRef());
        }
        return this.getBackingStore().exists(doc, context);
    }

    private Optional<DocumentDto> getAtlasDoc(String docId) {
        DocumentDto atlasDoc = getAtlasDocClient().get(docId);
        LOGGER.info("AtlasStore loaded {} and got {}", doc.getDocRef().getName(), atlasDoc);
        return Optional.ofNullable(atlasDoc);
    }

    private DocumentStoreClient getAtlasDocClient() {
        DocumentStoreClient atlasDocClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(DocumentStoreClient.class))
                .logLevel(feign.Logger.Level.FULL)
                .target(DocumentStoreClient.class,
                 cfgSource.getProperty("com.celements.atlas.store.url", "http://localhost:8081"));
        return atlasDocClient;
    }

    private String getSpaceName(XWikiDocument doc) {
        return Optional.ofNullable(doc.getDocRef())
                .map(DocumentReference::getLastSpaceReference)
                .map(SpaceReference::getName)
                .orElse(null);
    }

}
