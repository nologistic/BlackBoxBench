# Vinyl Music Player reproduction supplementary information (non-entity materials)

## Library aggregation model

- Song fields: title, artist, album, track number, duration (seconds), year, genre.
- Album page: the album's tracks sorted by track number; covers use cover images (different
  albums may use differently colored synthetic covers).
- Artist page → album list → album details (track table).
- Derived views:
  - The "Albums" page aggregates by album automatically (same album name = one album).
  - "Genres" and "Years" pages group-browse by field.
  - The "Songs" page tiles all songs and supports search (title/artist/album name).
- After editing a song's tags (metadata), all aggregate views regroup by the new values immediately.

## Playback and queue

- Tap a song → added to "Now playing" and starts playback; the player page shows cover, title,
  artist, progress bar, play/pause, previous/next.
- Queue: two layers, "Now playing" and "Up next"; playing from any list rebuilds the queue.
- Shuffle: play the whole queue in random order; Repeat: off / repeat-all / repeat-one, three states.
- Finishing the current track advances automatically; leaving the page keeps playing in the background and the progress is still there on return.
- Sleep timer: playback stops when it fires.

## Playlists

- From a song's menu, "add to playlist" → create or pick an existing list; several songs can be added in a row.
- The playlist page shows a cover collage and the track count; entering plays in order.
- M3U import: match the library by in-file track titles (round-trip keeps the set unchanged).

## Vinyl animation

- While playing, a spinning vinyl-disc view is shown (cover centered, disc rotating); it stops when paused
  — this is the app's signature visual and worth reproducing.

## Key reproduction behaviors

- Five bottom-bar entries: Songs / Albums / Artists / Playlists / Settings (order may follow what
  the exploration found).
- Switching pages does not lose the playing context (mini player bar + progress kept).
- Write operations (playlists, tag editing, settings) survive a restart.
