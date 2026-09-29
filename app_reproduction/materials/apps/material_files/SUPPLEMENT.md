# Material Files reproduction supplementary information (non-entity materials)

## Browsing and storage

- Storage switch: internal storage / SD card (the SD card may be empty in the sandbox). A breadcrumb or
  path bar shows the current location; the go-up button is always available.
- Directory entry animation / the list updates instantly; empty directories show an empty state.

## Sorting and views

- Sort: name / path / size / modified date / taken date / random, ascending or descending.
- **Random sort**: the order changes on each refresh (a deterministic seed is optional).
- Views: list / grid toggle; the choice persists.
- File types are distinguished by icon: directory / image / archive / document / APK / other.

## Archives

- zip as a drill-down directory: entering lists its files, browsable, and single files can be extracted to the current directory;
  extracted files appear in the list.
- "Extract all" creates a same-named directory.

## Hidden files

- Files/directories starting with `.` are hidden by default per the rules; the menu "show hidden files"
  makes them appear, switching back hides them, and **the toggle state persists**.
- `.nomedia`: when a directory contains this file, album-style scans skip it (still visible inside this app).

## Write-operation semantics

| Operation | Behavior |
|---|---|
| New folder | A new directory appears in the current one immediately and is still there on re-entry |
| Rename | The name updates; ownership unchanged |
| Delete | Disappears from the list (with a confirm dialog); other files' order and state are unaffected |
| Bookmarks | Add/remove directory bookmarks, jump quickly from the bookmark bar, persisted |

- The system storage permission is unavailable (sandbox limitation): all operations act on app-private/virtual storage
  and persist — an **intentional difference** from a real device, with the functionality itself complete.
