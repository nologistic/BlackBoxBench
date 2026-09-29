# Joplin reproduction supplementary information (non-entity materials)

## Data model

| Object | Fields | Description |
|---|---|---|
| Notebook | Name, parent notebook | Two or more levels of nesting; tree sidebar on the left |
| Note | Title, body (Markdown), tags, pin, to-do toggle | List ordered by update time |
| To-do | Same as note + checkbox | Distinguished by checkbox state in the note list |
| Tag | Name | Many-to-many onto notes; a tag bar groups across lists |
| Attachment | Image/file | Stored in the resource library, referenced in the body as `![title](resource id)` |

- The to-do checkbox state stays in sync with the `- [ ]` / `- [x]` list syntax in the body.

## Markdown rendering scope (must be supported)

- Headings `#`–`######`, paragraphs, **bold**, *italic*, `inline code`
- Unordered/ordered lists, task lists (checkboxes interactive and written back to the source)
- Links, blockquotes `>`, horizontal rules `---`
- Tables (GFM), fenced code blocks (language-tag coloring may be simplified)
- Images (in-app resources; external loading not required in the sandbox)

## Interaction highlights

- Editor ↔ preview switching (live rendering or a button toggle both fine, states consistent).
- Search: full-text match on titles and bodies, with keyword highlighting in the results.
- An "All notes" aggregated view + filtering by notebook/tag.
- Note sorting: update time/title/manual pinning.
- Recycle bin: deleted notes go there and can be restored or removed permanently.

## Sync and security (sandbox-simplified semantics)

- End-to-end encryption and WebDAV sync are unavailable in the sandbox (no network account);
  full local persistence is sufficient, and the settings page shows sync as unconfigured.

## Key reproduction behaviors

- New note → type Markdown → preview renders correctly → leaving and re-entering keeps content and rendering identical.
- To-do checkbox round trip (check it → still checked on re-entry).
- Tag filtering and notebook-tree navigation are the main browsing paths.
