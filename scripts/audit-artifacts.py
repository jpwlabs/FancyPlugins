#!/usr/bin/env python3
"""Fail closed when JPW's three Fancy presentation artifacts violate policy."""

from __future__ import annotations

import hashlib
import json
import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARTIFACTS = {
    "FancyNpcs": ROOT / "plugins/fancynpcs-v2/build/libs/FancyNpcs-2.9.2.341-jpw.1.jar",
    "FancyHolograms": ROOT / "plugins/fancyholograms-v2/build/libs/FancyHolograms-2.9.1.180-jpw.1.jar",
    "FancyDialogs": ROOT / "plugins/fancydialogs/build/libs/FancyDialogs-1.1.2-jpw.1.jar",
}
FORBIDDEN = (
    b"de/oliver/fancyanalytics",
    b"org/bstats",
    b"PlayerCommandAsOpAction",
    b"player_command_as_op",
    b"NpcConvertCMD",
    b"versionFetcher",
    b"api.fancyanalytics.com",
    b"bstats.org",
    b"api.modrinth.com/v2/project/",
    b"api.hangar.papermc.io/v1/projects/",
)
MAX_CLASS_MAJOR = 65


def digest(path: Path, algorithm: str) -> str:
    value = hashlib.new(algorithm)
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def audit(name: str, path: Path) -> dict[str, object]:
    if not path.is_file():
        raise ValueError(f"missing locked artifact: {path}")
    maximum = 0
    found_forbidden: set[str] = set()
    with zipfile.ZipFile(path) as archive:
        bad = archive.testzip()
        if bad:
            raise ValueError(f"{path.name}: corrupt ZIP entry {bad}")
        for entry in archive.infolist():
            data = archive.read(entry)
            if entry.filename.endswith(".class"):
                if len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe":
                    raise ValueError(f"{path.name}: malformed class {entry.filename}")
                maximum = max(maximum, struct.unpack(">H", data[6:8])[0])
            searchable = entry.filename.encode() + b"\0" + data
            for token in FORBIDDEN:
                if token.lower() in searchable.lower():
                    found_forbidden.add(token.decode())
    if maximum > MAX_CLASS_MAJOR:
        raise ValueError(f"{path.name}: class major {maximum} exceeds Java 21 ({MAX_CLASS_MAJOR})")
    if found_forbidden:
        raise ValueError(f"{path.name}: forbidden content: {', '.join(sorted(found_forbidden))}")
    return {
        "name": name,
        "file": path.name,
        "sha256": digest(path, "sha256"),
        "sha512": digest(path, "sha512"),
        "maximumClassMajor": maximum,
    }


def main() -> int:
    try:
        report = [audit(name, path) for name, path in ARTIFACTS.items()]
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        print(f"artifact audit failed: {error}", file=sys.stderr)
        return 1
    print(json.dumps({"artifacts": report}, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
