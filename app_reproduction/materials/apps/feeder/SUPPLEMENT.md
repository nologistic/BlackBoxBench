# Feeder reproduction supplementary information (non-entity materials)

## Three feed formats → one unified data model

Feeder's core capability is converting the three formats into the same Feed/Article model,
behaving identically in the list and the reader. Corresponding fields:

| Model field | RSS 2.0 | Atom | JSON Feed 1.1 |
|---|---|---|---|
| Feed title | `channel/title` | `feed/title` | `title` |
| Feed description | `channel/description` | `feed/subtitle` | `description` |
| Article title | `item/title` | `entry/title` | `items[].title` |
| Article link | `item/link` | `entry/link@href` | `items[].url` |
| Published date | `item/pubDate` (RFC 822) | `entry/updated` (RFC 3339) | `items[].date_published` (RFC 3339) |
| Body | `item/description` (HTML) | `entry/content` or `summary` | `items[].content_html` |
| Unique id | `item/guid` | `entry/id` | `items[].id` |

- Date parsing must handle both formats: RFC 822 (`Mon, 07 Sep 2026 08:00:00 GMT`) and
  RFC 3339（`2026-09-07T08:00:00Z`）。
- The body renders as HTML (headings/paragraphs/images/links); unrecognized tags degrade to plain text.

## OPML subscription lists

- Import: parse `<outline type="rss" text="display name" xmlUrl="feed URL"/>`,
  creating one subscription per outline.
- Export: write all current subscriptions back in the same structure; import/export round-trips leave the set unchanged.

## Sync and the "new article" rule

- Manual refresh: fetch all feeds immediately; triggered by pull-to-refresh or a button.
- Scheduled sync: hourly/daily periods, combinable with "Wi-Fi only" and "charging only" conditions;
  a round is skipped when the conditions are unmet.
- **New-article rule**: an article's unique id (guid/id) never seen before → new; ids already seen
  do not count as "new" even if the content updates; disabling notifications for a feed still syncs articles,
  it just produces no notifications.
- Read/unread: entering the reader marks it read; the list can filter by unread.

## Offline behavior

- Successfully synced feeds and articles are cached locally and remain fully readable offline.
- A manual refresh while offline should show a clear failure notice without clearing the existing cache.

## Key reproduction behaviors

- Add subscription: enter a URL → fetch and parse → appears in the left subscription list (title + unread count).
- The three formats coexist in one subscription list with identical list/reader interactions.
- The article list is ordered newest-first; unread items carry a visual marker; tapping opens the reader and can return.
- Importing OPML adds the corresponding subscription entries; the exported file can be imported back unchanged.
