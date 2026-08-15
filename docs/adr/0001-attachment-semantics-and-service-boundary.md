# ADR 0001: Attachment semantics and service boundary

## Status

Accepted on 2026-08-15.

## Context

XWiki stores an attachment under the logical identity of its owning document and filename. Uploading
content with an existing filename does not create a second logical attachment. Saving the new content
increments the attachment version and appends the new state to the attachment archive while retaining
the older revisions.

The unversioned download URL resolves the current attachment revision. XWiki also provides a separate
revision URL for retrieving a specified historical revision. Deleting an attachment removes its
current content, metadata, and active revision archive. When the attachment recycle bin is enabled,
the deleted attachment and its archive can remain recoverable from the recycle bin.

`XWikiDocument.getAttachment(String)` does not implement exact filename lookup. It first looks for an
exact match and then returns the first attachment whose filename starts with the requested value plus
`.`. `XWikiDocument.addAttachment(...)` uses this lookup internally. Consequently, these APIs can
select or update a different logical attachment than application code requested.

Celements already provides `IAttachmentServiceRole` and `AttachmentService`. The service implements
explicit exact-name operations and is the appropriate boundary for insulating application code from
XWiki attachment behavior.

## Decision drivers

- Attachment identity must use the exact filename supplied by the caller.
- Existing attachment revisions must remain available after updating current content.
- Application code must not depend on ambiguous or legacy `XWikiDocument` lookup behavior.
- XWiki-specific storage and versioning details must remain behind a Celements-owned abstraction.
- Missing service operations should be added once at the shared boundary instead of reimplemented by
  each application.

## Considered options

### Use `XWikiDocument` attachment methods directly

This preserves the legacy API but exposes its non-exact lookup behavior and couples application code
to XWiki persistence details.

### Change `XWikiDocument.getAttachment(String)` to exact lookup

This would improve the method locally but could break legacy callers that rely on its extension
fallback. It would also leave application code coupled to XWiki APIs.

### Require the Celements attachment service

This provides exact filename semantics, one place for persistence behavior, and a boundary that can
be extended without exposing XWiki implementation details.

## Decision

Application and domain code must access and manipulate attachments through
`IAttachmentServiceRole`. In particular:

1. Attachment identity is the owning `DocumentReference` plus an exact filename.
2. Creating an attachment requires that the exact filename does not already exist.
3. Updating an existing attachment stores new current content under the same logical identity and
   creates a new attachment revision.
4. Deleting an attachment removes the logical attachment and all revisions from the active document.
   A configured recycle bin may retain a recoverable copy.
5. An unversioned content URL means "the latest revision". A revision-qualified URL means "this exact
   revision".
6. Application and domain code must not call `XWikiDocument.getAttachment(String)`,
   `XWikiDocument.addAttachment(...)`, or `XWikiDocument.deleteAttachment(...)` directly.
7. Exact lookup must use `IAttachmentServiceRole.getAttachmentNameEqual(...)` or another explicitly
   named service operation with documented matching semantics.
8. If `IAttachmentServiceRole` does not yet provide a required operation or error contract, it must be
   extended rather than bypassed.

Direct use of `XWikiDocument` primitives remains permitted inside the attachment-service and XWiki
storage implementations, where those primitives are encapsulated. Existing application-level direct
calls are legacy code and should be migrated when touched.

## Consequences

### Positive

- Exact filename identity is consistent across listing, creation, update, and deletion.
- XWiki's extension fallback cannot silently select the wrong attachment.
- Revision creation and deletion semantics are centralized and testable.
- Domain APIs can expose logical attachment identifiers without exposing physical storage details.
- Future authorization, validation, event, and concurrency behavior can be added at one boundary.

### Negative

- Some existing code bypasses `IAttachmentServiceRole` and will require incremental migration.
- New use cases may require extending the service before application work can proceed.
- Callers must handle explicit not-found and conflict outcomes instead of relying on fallback lookup.

## API implications

A REST API built on this model should distinguish the following operations:

- `POST` creates a new exact filename and returns a conflict when it already exists.
- `PUT` on an existing filename uploads a new revision and returns not found when it does not exist.
- `DELETE` removes the logical attachment and all active revisions.

The current attachment revision is suitable as an opaque ETag. Conditional `PUT` and `DELETE`
requests can use `If-Match` to avoid updating or deleting a logical attachment whose current revision
changed after the client read it.

## Source references

- [`XWikiDocument.getAttachment(String)` and `addAttachment(...)`](../../celements-xwiki-core/src/main/java/com/xpn/xwiki/doc/XWikiDocument.java)
- [`XWikiAttachmentArchive.updateArchive(...)`](../../celements-xwiki-core/src/main/java/com/xpn/xwiki/doc/XWikiAttachmentArchive.java)
- [`XWikiHibernateAttachmentStore`](../../celements-xwiki-core/src/main/java/com/xpn/xwiki/store/XWikiHibernateAttachmentStore.java)
- [`IAttachmentServiceRole`](../../celements-model/src/main/java/com/celements/filebase/IAttachmentServiceRole.java)
- [`AttachmentService`](../../celements-model/src/main/java/com/celements/filebase/AttachmentService.java)
