import importlib.util
import io
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("release", Path(__file__).with_name("release.py"))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)

class ReleaseTests(unittest.TestCase):
    def test_seed_and_merge_assigned_notes_share_one_source(self):
        entry = release.history(release.assemble())[0]
        self.assertGreater(entry["versionCode"], 0)
        self.assertTrue(entry["notes"])
    def test_regressions_are_rejected(self):
        with self.assertRaises(ValueError):
            release.history("## 0.1.0 (1), 2026-11-11\n- New.\n## 0.2.0 (2), 2026-11-10\n- Old.\n")
    def test_runtime_gate_ignores_docs_but_covers_sources(self):
        self.assertTrue(release.runtime_path("android/app/src/main/kotlin/App.kt"))
        self.assertTrue(release.runtime_path("android/app/build.gradle.kts"))
        self.assertFalse(release.runtime_path("android/app/src/test/kotlin/AppTest.kt"))
        self.assertFalse(release.runtime_path("android/app/src/main/CONTRACTS"))
    def test_change_note_requires_actual_notes(self):
        with self.assertRaises(ValueError):
            release.change("android/changes/example.md", "## 2026-11-11\nNo note item")

    def test_storage_keeps_manifest_etag_and_applies_it_to_promotion(self):
        class Store:
            def get_object(self, **kwargs):
                self.read = kwargs
                return {"Body": io.BytesIO(b'{"files":[]}'), "ETag": '"original"'}
            def put_object(self, **kwargs):
                self.write = {key: value for key, value in kwargs.items() if key != "Body"}
        remote = release.Remote.__new__(release.Remote)
        remote.s3 = Store()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "manifest.json"
            etag = remote.r2("get", "manifest.json", path)
            self.assertEqual(etag, '"original"')
            remote.r2("put", "manifest.json", path, if_match=etag)
        self.assertEqual(remote.s3.read, {"Bucket": "vercel-native", "Key": "manifest.json"})
        self.assertEqual(remote.s3.write["Bucket"], "vercel-native")
        self.assertEqual(remote.s3.write["IfMatch"], '"original"')

    def test_candidate_must_match_before_the_manifest_can_be_promoted(self):
        remote = release.Remote.__new__(release.Remote)
        with tempfile.TemporaryDirectory() as directory:
            remote.stage = Path(directory)
            remote.request = lambda path, output: output.write_bytes(b"wrong APK")
            entry = {"name": "vercel-native-0.1.0.apk", "bytes": 42, "sha256": "a" * 64}
            with self.assertRaisesRegex(ValueError, "active feed was not changed"):
                remote.verify_package(entry, None, None)

if __name__ == "__main__":
    unittest.main()
