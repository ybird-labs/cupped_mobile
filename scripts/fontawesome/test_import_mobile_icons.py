#!/usr/bin/env python3

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from scripts.fontawesome import import_mobile_icons


class ImportMobileIconsTests(unittest.TestCase):
    def test_scaled_android_size_preserves_square_icons(self) -> None:
        self.assertEqual(import_mobile_icons.scaled_android_size("560", "560"), ("24dp", "24dp"))

    def test_scaled_android_size_preserves_tall_icons(self) -> None:
        self.assertEqual(import_mobile_icons.scaled_android_size("448", "512"), ("21dp", "24dp"))

    def test_scaled_android_size_preserves_wide_icons(self) -> None:
        self.assertEqual(
            import_mobile_icons.scaled_android_size("576", "512"),
            ("24dp", "21.33dp"),
        )

    def test_scaled_android_size_rejects_invalid_dimensions(self) -> None:
        with self.assertRaisesRegex(ValueError, "Invalid viewBox dimensions"):
            import_mobile_icons.scaled_android_size("0", "512")

    def test_write_android_vector_preserves_viewport_and_formats_output(self) -> None:
        svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
  <path fill="currentColor" d="M1 2L3 4z"/>
</svg>
"""
        with tempfile.TemporaryDirectory() as tmpdir:
            svg_path = Path(tmpdir) / "icon.svg"
            out_dir = Path(tmpdir) / "drawable"
            svg_path.write_text(svg, encoding="utf-8")
            out_dir.mkdir()

            with patch.object(import_mobile_icons, "ANDROID_DRAWABLE_ROOT", out_dir):
                import_mobile_icons.write_android_vector(svg_path, "sample_icon")

            actual = (out_dir / "sample_icon.xml").read_text(encoding="utf-8")

        self.assertIn('<?xml version="1.0" encoding="utf-8"?>', actual)
        self.assertIn('android:width="21dp"', actual)
        self.assertIn('android:height="24dp"', actual)
        self.assertIn('android:viewportWidth="448"', actual)
        self.assertIn('android:viewportHeight="512"', actual)
        self.assertIn('\n  <path ', actual)
        self.assertIn('android:fillColor="#FF000000"', actual)
        self.assertIn('android:pathData="M1 2L3 4z"', actual)


if __name__ == "__main__":
    unittest.main()
