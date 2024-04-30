package org.opencelements.atlas.store;

import javax.inject.Named;

import org.springframework.stereotype.Component;

import com.celements.store.DelegateStore;

@Component
@Named("org.opencelements.atlas.store.AtlasDocumentStore")
public class AtlasDocumentStore extends DelegateStore {

    @Override
    protected String getName() {
        return "atlas";
    }

}
