import hashlib
import os
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

SCRIPT = Path(__file__).with_name("normalize-artifacts.py")


class NormalizeArtifactsTest(unittest.TestCase):
    def test_different_zip_metadata_normalizes_byte_identically(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            first = root / "first.jar"
            second = root / "second.jar"
            entries = {
                "META-INF/MANIFEST.MF": b"Manifest-Version: 1.0\n",
                "example/Plugin.class": b"\xca\xfe\xba\xbe\x00\x00\x00\x41",
                "paper-plugin.yml": b"name: Example\n",
            }
            with zipfile.ZipFile(first, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                for name, data in entries.items():
                    archive.writestr(name, data)
            with zipfile.ZipFile(second, "w", compression=zipfile.ZIP_STORED) as archive:
                for name, data in reversed(tuple(entries.items())):
                    info = zipfile.ZipInfo(name, (2026, 8, 20, 12, 34, 56))
                    info.create_system = 0
                    info.external_attr = 0
                    archive.writestr(info, data)
            os.chmod(first, 0o600)
            os.chmod(second, 0o755)

            for path in (first, second):
                subprocess.run([sys.executable, str(SCRIPT), str(path)], check=True, capture_output=True, text=True)

            self.assertEqual(hashlib.sha256(first.read_bytes()).digest(), hashlib.sha256(second.read_bytes()).digest())
            with zipfile.ZipFile(first) as archive:
                self.assertIsNone(archive.testzip())
                self.assertTrue(all(entry.compress_type == zipfile.ZIP_STORED for entry in archive.infolist()))


if __name__ == "__main__":
    unittest.main()
