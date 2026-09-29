# Materials catalog

This directory is the reproduction Agent's shared read-only material library. All people, brands, content, and accounts are fictional test data.

## Readable content

- `source.json`: structured source content — users, products, articles, posts, comments, messages, and orders.
- `generated/source.json`: the normalized copy used at build time.
- `generated/manifest.json`: size and SHA-256 for every generated file.
- `generated/library.db`: the SQLite seed database holding all content.
- `generated/images/avatars/`: 6 avatars.
- `generated/images/products/`: 8 product images.
- `generated/images/covers/`: 5 article covers.
- `generated/images/posts/`: 4 social images.
- `generated/images/placeholders/`: generic hero and empty-state images.
- `generated/audio/`: notification, success-feedback, and ambient WAV files.
- `generated/videos/`: product showcase and social loop MP4s.
- `backend/server.py`: optional FastAPI backend template.
- `backend/API.md`: backend API reference.

## Usage rules

Do not modify this directory directly. To use a file, copy it into `website_output/`:

```text
reproduction/materials/generated/images/...  ──copy──▶ website_output/public/...
reproduction/materials/generated/library.db  ──copy──▶ website_output/data/library.db
reproduction/materials/generated/library.db  ──copy──▶ website_output/seed/library.db
reproduction/materials/backend/               ──copy──▶ website_output/backend/...
```

Test accounts all use the password `demo123`, e.g. `linxi/demo123`. The materials do not dictate page layout; the Agent should
decide the information architecture, pages, and interactions from the Functional Topology.
