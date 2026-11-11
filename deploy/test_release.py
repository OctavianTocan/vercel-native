import importlib.util
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

if __name__ == "__main__":
    unittest.main()
