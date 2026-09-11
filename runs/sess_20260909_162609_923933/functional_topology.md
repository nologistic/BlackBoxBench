# Functional Topology — feeder

- session: `sess_20260909_162609_923933`
- generated: 2026-09-09T17:03:27.425104+00:00
- coverage: 9 states · 10 features · 2 data · 10 edges (confirmed ratio 100%, 70 actions)

## Graph
```
feeder
├─ States
│  ├─ [✓] All feeds empty state (0.98) `state_all_feeds_empty_state`
│  ├─ [✓] Navigation drawer (0.98) `state_navigation_drawer`
│  ├─ [✓] Saved articles empty state (0.97) `state_saved_articles_empty_state`
│  ├─ [✓] Edit feed settings (0.99) `state_edit_feed_settings`
│  ├─ [✓] Single feed empty state (0.99) `state_single_feed_empty_state`
│  ├─ [✓] Navigation drawer with tag group (0.99) `state_navigation_drawer_with_tag_group`
│  ├─ [✓] Add feed URL entry (0.99) `state_add_feed_url_entry`
│  ├─ [✓] Feed lookup failure (0.99) `state_feed_lookup_failure`
│  ├─ [✓] Tag article scope (0.99) `state_tag_article_scope`
├─ Features
│  ├─ [✓] Live article search (0.96) `feature_live_article_search`
│  ├─ [✓] Add feed from URL with fallback (0.99) `feature_add_feed_from_url_with_fallback`
│  │    ─MUTATES→ Feed subscription
│  │    ─TRANSITIONS_TO→ Single feed empty state
│  ├─ [✓] Configure feed properties (0.99) `feature_configure_feed_properties`
│  │    ─MUTATES→ Feed subscription
│  │    ─MUTATES→ Feed tag
│  ├─ [✓] Feed persists across restart (0.99) `feature_feed_persists_across_restart`
│  ├─ [✓] Expand and collapse tag groups (0.99) `feature_expand_and_collapse_tag_groups`
│  ├─ [✓] Cancel feed lookup (0.96) `feature_cancel_feed_lookup`
│  ├─ [✓] Navigate article scopes (0.99) `feature_navigate_article_scopes`
│  │    ─TRANSITIONS_TO→ Saved articles empty state
│  ├─ [✓] Open another feed shortcut (0.98) `feature_open_another_feed_shortcut`
│  │    ─TRANSITIONS_TO→ Navigation drawer
│  ├─ [✓] Open tag article scope (0.99) `feature_open_tag_article_scope`
│  │    ─TRANSITIONS_TO→ Tag article scope
│  ├─ [✓] Clear and exit article search (0.99) `feature_clear_and_exit_article_search`
├─ Data
│  ├─ [✓] Feed subscription (0.99) `data_feed_subscription`
│  │    ─PERSISTS_TO→ Feed persists across restart
│  ├─ [✓] Feed tag (0.99) `data_feed_tag`
│  │    ─REVEALS→ Navigation drawer with tag group
│  │    ─ENABLES→ Open tag article scope
```

## Features
### Live article search `feature_live_article_search`
- status: ClaimStatus.CONFIRMED · confidence: 0.96
- Tapping search replaces the title bar with a focused search field and clear button. Text entry updates the query. Back first hides the keyboard and Back again exits search.
- evidence: step 6 (frame 11 → 12)

### Add feed from URL with fallback `feature_add_feed_from_url_with_fallback`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The add-feed screen accepts a non-empty URL, enables Search, normalizes a missing scheme to HTTP, displays a download error when lookup fails, and offers Still add. The fallback opens a full feed editor; confirming creates the named feed and navigates to its empty article list.
- evidence: step 18 (frame 35 → 36)

### Configure feed properties `feature_configure_feed_properties`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The feed editor allows changing title and tags, independently toggling feed behaviors, and selecting one of four content opening choices. Confirm saves the choices; Cancel abandons them.
- evidence: step 24 (frame 47 → 48)

### Feed persists across restart `feature_feed_persists_across_restart`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- A newly added feed remains the active destination with its custom title after the app is restarted.
- evidence: step 35 (frame 69 → 70)

### Expand and collapse tag groups `feature_expand_and_collapse_tag_groups`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Tapping a tag-row chevron alternately reveals and hides the feed assigned to that tag without closing the navigation drawer.
- evidence: step 37 (frame 73 → 74)

### Cancel feed lookup `feature_cancel_feed_lookup`
- status: ClaimStatus.CONFIRMED · confidence: 0.96
- Pressing Back from a lookup result returns to the prior article list without creating another feed.
- evidence: step 63 (frame 124 → 125)

### Navigate article scopes `feature_navigate_article_scopes`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- The drawer changes the article-list scope among All feeds, Saved articles, individual feeds, and tagged feeds. Choosing a destination closes the drawer and updates the title.
- evidence: step 3 (frame 5 → 6)

### Open another feed shortcut `feature_open_another_feed_shortcut`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- The empty-state Open another feed link opens the navigation drawer so a different feed can be chosen.
- evidence: step 46 (frame 90 → 91)

### Open tag article scope `feature_open_tag_article_scope`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Tapping a tag name closes the drawer and opens an article-list scope for that tag.
- evidence: step 67 (frame 132 → 133)

### Clear and exit article search `feature_clear_and_exit_article_search`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- When a query is present, tapping the X button clears the query, closes the keyboard, and restores the normal article-list toolbar.
- evidence: step 70 (frame 138 → 139)
