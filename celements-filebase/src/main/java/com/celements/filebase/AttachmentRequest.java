package com.celements.filebase;

import org.xwiki.model.reference.DocumentReference;

record AttachmentRequest(DocumentReference docRef, String dirPath) {
}
