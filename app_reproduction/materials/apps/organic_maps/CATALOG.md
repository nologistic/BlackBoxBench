# Organic Maps reproduction supplementary materials

This directory is the target-specific material pack for the `organic_maps` (offline maps and navigation) reproduction workspace,
mounted read-only as `/materials/app`. All places are fictional.

## Entity materials

- `pois.json`: 46 fictional POIs (city/park/road/station/dining/hotel categories,
  with indicative coordinates), covering search, category browsing, and route endpoint picking.
- `map_regions.json`: a list of fictional offline-downloadable map regions (name/size/status).

## Non-entity supplementary information

See `SUPPLEMENT.md`: map interaction (locate/zoom/pan), the POI details page,
search, bookmarks and tracks, route planning and travel modes, and the offline-region lifecycle.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there.
The map basemap can be a programmatically drawn fictional city sketch (grid streets + POI dots).
