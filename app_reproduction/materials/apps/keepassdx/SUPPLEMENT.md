# KeePassDX reproduction supplementary information (non-entity materials)

## Database (kdbx) concepts

- A kdbx file = an encrypted container holding a group tree, entries, a recycle bin, and database settings.
- Versions: kdbx 3.x / 4.x (cipher AES or ChaCha20; key derivation
  Argon2 / AES-KDF by default for kdbx3).
- **Master key** = password and/or key file (.key); entered on the unlock page → verified →
  the vault opens. Changing the master key re-encrypts and saves.

## Groups and entries

| Object | Fields |
|---|---|
| Group | Name, icon, level (multi-level nesting), auto-type settings |
| Entry | Title, username, password, URL, notes, icon, expiry time (+enabled), custom fields, tags |
| Recycle bin | A special group; deleted entries move here by default and can be emptied |

- The password field is hidden by default (eye icon reveals); entry cards show title/username + icon.
- Copy username/password to the clipboard (with a countdown-clear notice).
- Expired entries carry a visual marker; expiry does not block opening.

## Unlock and lock

- Launch → database picker (recent vaults + create/import) → enter the master key → inside the vault.
- New-database wizard: set the master key (with a strength indicator) → generate the vault.
- Lock: manual or timeout on backgrounding; unlocking returns to the pre-lock group.
- Wrong master key: a clear error, no data cleared.

## Search and sorting

- Search: matches title/username/URL/notes; results listed across groups.
- Sort: natural / title / username / created / modified, ascending or descending.
- "Sort & group display rules" can be set per group.

## Security behavior (sandbox semantics)

- Auto-fill and fingerprint unlock depend on system capabilities (unavailable in the sandbox): showing an "unavailable"
  state is enough — not a defect.
- The database saves as an in-app file; all write operations (create group/entry, edit fields, recycle)
  persist.
- Password generation: length + charset (upper/lower/digits/symbols); the result can be regenerated.

## Key reproduction behaviors

- Unlock → group-tree navigation → entry details → edit and save → still there on re-entry; delete →
  recycle bin → restore/permanent delete; lock/unlock cycles lose nothing — these are the core loops.
