# Organic Maps reproduction supplementary information (non-entity materials)

## Map interaction

- Basemap: a programmatically drawn fictional city sketch (street grid + parcel color blocks + POI pins);
  real tiles are not required.
- Gestures: one-finger drag to pan, pinch to zoom (levels 1–5), double-tap to zoom in.
- Locate button: shows a blue position dot; after panning away, tap "back to current location" to recenter.
- The zoom level affects POI density (low levels show only major categories).

## POIs and the details page

- Tapping a POI on the map (city/road/park/station/business) opens a details card:
  name, category, coordinates, phone/opening hours (if any).
- Details-card actions: "add bookmark", "start from here", "route here".

## Search

- Type a keyword into the top bar → a suggestion dropdown (sorted by category and match) → tap to locate
  and open the details.
- Category quick filters: a row of icons for dining/lodging/fuel/parking, etc.

## Bookmarks and tracks

- Bookmarks: a saved-place list (name/coordinates/group/color), tap to jump;
  names and groups are editable; deleting removes it from the list.
- Tracks (KML concept): a set of imported coordinate points can be connected and shown on the map.
- Bookmarks/tracks export to a file and can be re-imported (round-trip keeps the set unchanged).

## Route planning

- Endpoints from three sources: search results / map taps / bookmarks.
- Travel modes: driving / walking / transit / cycling; routes and times recompute on switch.
- Alternative routes: 2–3 shown for the same endpoints (primary + alternatives), tap to switch.
- Route preferences (driving): avoid tolls/highways/ferries; recomputes after changes.
- Transit: multi-leg plans combining "walk → line → transfer → walk".
- Navigation: tap "Start" to enter simulated navigation (an arrow advancing along the route + remaining distance/time);
  "End" exits.

## Offline regions

- Region list: name + size + status (not downloaded / downloading / up to date / update available).
- Download → progress bar → offline usable when done; can be updated again after map updates.
- Offline, downloaded regions' maps, roads, and places remain browsable and searchable.

## Key reproduction behaviors

- The position dot, POI pop-ups, search-to-locate, and endpoint routes (recomputed on mode switch) are
  the core observable loops; sketch precision does not matter, but the state machine must be right.
