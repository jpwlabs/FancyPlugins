#!/usr/bin/env python3
"""Rewrite JPW plugin jars into a byte-identical, cross-platform ZIP form."""

from __future__ import annotations

import argparse
import os
import tempfile
import zipfile
from collections import Counter
from pathlib import Path

FIXED_TIMESTAMP = (1980, 1, 1, 0, 0, 0)
FILE_MODE = 0o100644 << 16
DIRECTORY_MODE = 0o40755 << 16


def normalize(path: Path) -> None:
    with zipfile.ZipFile(path) as source:
        corrupt = source.testzip()
        if corrupt:
            raise ValueError(f"{path.name}: corrupt ZIP entry {corrupt}")
        entries = source.infolist()
        duplicates = sorted(name for name, count in Counter(item.filename for item in entries).items() if count > 1)
        if duplicates:
            raise ValueError(f"{path.name}: duplicate ZIP entries: {', '.join(duplicates[:5])}")
        payloads = {entry.filename: source.read(entry) for entry in entries}

    ordered = sorted(payloads, key=lambda name: (name != "META-INF/MANIFEST.MF", name))
    with tempfile.NamedTemporaryFile(prefix=path.name + ".", suffix=".normalized", dir=path.parent, delete=False) as handle:
        temporary = Path(handle.name)
    try:
        with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_STORED, strict_timestamps=True) as target:
            target.comment = b""
            for name in ordered:
                info = zipfile.ZipInfo(name, FIXED_TIMESTAMP)
                info.compress_type = zipfile.ZIP_STORED
                info.create_system = 3
                info.external_attr = DIRECTORY_MODE if name.endswith("/") else FILE_MODE
                info.extra = b""
                info.comment = b""
                target.writestr(info, payloads[name])
        with temporary.open("rb") as normalized:
            os.fsync(normalized.fileno())
        os.chmod(temporary, 0o644)
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("artifacts", nargs="+", type=Path)
    args = parser.parse_args()
    try:
        for artifact in args.artifacts:
            if not artifact.is_file():
                raise ValueError(f"missing artifact: {artifact}")
            normalize(artifact)
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        parser.error(str(error))
    print("normalized artifacts: " + ", ".join(path.name for path in args.artifacts))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
