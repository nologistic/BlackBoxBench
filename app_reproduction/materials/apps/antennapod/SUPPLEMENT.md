# AntennaPod reproduction supplementary information (non-entity materials)

## Podcast RSS (RSS 2.0 + iTunes extension)

Channel level: `title`, `description`, `itunes:author`, `itunes:image`,
`language`、`itunes:category`。

Episode level:

| Field | Source |
|---|---|
| Title | `item/title` |
| Published date | `item/pubDate` (RFC 822) |
| Duration | `itunes:duration` (`32:15` or seconds) |
| Description | `item/description` |
| Media URL | `enclosure@url` + `@type` (audio/mpeg) |
| Unique id | `item/guid` |

- No real audio URLs exist in the sandbox; playback maps to the sample WAVs in `/materials/mobile/audio/`
  (duration shown from metadata; exact decoding is not required).

## Episodes and chapters

- Episode states: unplayed / downloaded / playing (progress seconds) / finished.
- Chapters: optional episode segments (start second + title); the player can jump chapters;
  episodes without chapters show no chapter button.

## Playback and queue

- The queue is an ordered list independent of subscriptions; "add to queue" appends, drag to reorder,
  and items can be removed from the queue.
- Continuous playback: when the current episode finishes → marked finished → the next in queue starts automatically.
- Player: play/pause, ±15 seconds, speed, progress bar; after an interruption (leaving the page) it resumes
  from the last position.
- Download: episodes download locally; the download list shows progress; downloaded episodes play offline.

## Subscription management

- Add podcast: enter an RSS URL → parse the channel and episode list → appears in subscriptions.
- Unsubscribe: removed from the list, with a checkbox to also delete downloaded episodes.
- OPML import/export round-trips leave the subscription set unchanged.
- New-episode detection: after re-fetching the feed, episodes with unseen `guid` appear at the top
  of the episode list marked "new".

## Key reproduction behaviors

- Three complete core screens: subscription list (cover + title), episode list (title+date+duration+state),
  and the queue page.
- Playback state survives page switches; leaving and re-entering keeps it.
