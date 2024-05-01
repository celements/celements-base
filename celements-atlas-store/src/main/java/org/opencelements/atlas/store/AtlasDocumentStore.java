package org.opencelements.atlas.store;

import org.springframework.stereotype.Component;
import com.celements.store.DelegateStore;

@Component("AtlasStore")
public class AtlasDocumentStore extends DelegateStore {

    @Override
    protected String getName() {
        return "atlas";
    }

}
