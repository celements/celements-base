package com.celements.atlas.store;

import org.springframework.stereotype.Component;

import com.celements.store.DelegateStore;

@Component("AtlasStore")
public class AtlasDocumentStore extends DelegateStore {

    public static final String NAME = "AtlasStore";

    @Override
    protected String getName() {
        return NAME;
    }

}
