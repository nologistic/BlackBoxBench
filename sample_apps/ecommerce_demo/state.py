"""JSON-file backed state store for the Nimbus Market sample app.

The whole application state lives in a single ``state.json`` inside the
``--data-dir`` directory. On first start (file missing) the built-in seed is
written; every mutating request is flushed to disk immediately so state
survives restarts.
"""

import json
import os
from pathlib import Path

from .seed import build_seed


class Store:
    def __init__(self, data_dir: str):
        self.data_dir = Path(data_dir)
        self.data_dir.mkdir(parents=True, exist_ok=True)
        self.path = self.data_dir / "state.json"
        if self.path.exists():
            self.data = json.loads(self.path.read_text(encoding="utf-8"))
        else:
            self.data = build_seed()
            self.save()

    def save(self) -> None:
        tmp = self.path.with_name(self.path.name + ".tmp")
        tmp.write_text(json.dumps(self.data, indent=2), encoding="utf-8")
        os.replace(tmp, self.path)
