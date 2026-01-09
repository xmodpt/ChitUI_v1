#!/usr/bin/env python3
"""
GOO File Thumbnail Extractor

Extracts thumbnail preview images from Elegoo .goo resin 3D printer files.
Based on the Elegoo GOO Format Specification V1.2

The .goo format contains two embedded preview images:
- Small preview: 116x116 pixels
- Big preview: 290x290 pixels

Both images are stored in RGB565 format (16-bit color):
- Red: 5 bits (bits 11-15)
- Green: 6 bits (bits 5-10)
- Blue: 5 bits (bits 0-4)
"""

import struct
import sys
from pathlib import Path
from typing import Tuple

try:
    from PIL import Image
except ImportError:
    print("Error: Pillow library is required. Install with: pip install Pillow")
    sys.exit(1)


class GooThumbnailExtractor:
    """Extracts thumbnail images from .goo files."""

    # Header field sizes (in bytes)
    VERSION_SIZE = 4
    SOFTWARE_INFO_SIZE = 32
    SOFTWARE_VERSION_SIZE = 24
    FILE_TIME_SIZE = 24
    PRINTER_NAME_SIZE = 32
    PRINTER_TYPE_SIZE = 32
    PROFILE_NAME_SIZE = 32

    # Preview image dimensions
    SMALL_PREVIEW_SIZE = (116, 116)
    BIG_PREVIEW_SIZE = (290, 290)

    def __init__(self, filepath: str):
        """
        Initialize the extractor with a .goo file path.

        Args:
            filepath: Path to the .goo file
        """
        self.filepath = Path(filepath)
        if not self.filepath.exists():
            raise FileNotFoundError(f"File not found: {filepath}")
        if not self.filepath.suffix.lower() == '.goo':
            print(f"Warning: File doesn't have .goo extension: {filepath}")

    def _calculate_preview_offset(self) -> int:
        """Calculate the byte offset where preview images start."""
        offset = 0
        offset += self.VERSION_SIZE
        offset += self.SOFTWARE_INFO_SIZE
        offset += self.SOFTWARE_VERSION_SIZE
        offset += self.FILE_TIME_SIZE
        offset += self.PRINTER_NAME_SIZE
        offset += self.PRINTER_TYPE_SIZE
        offset += self.PROFILE_NAME_SIZE
        offset += 2  # anti_aliasing_level (u16)
        offset += 2  # grey_level (u16)
        offset += 2  # blur_level (u16)
        return offset

    def _decode_rgb565(self, pixel: int) -> Tuple[int, int, int]:
        """
        Decode a 16-bit RGB565 pixel to 8-bit RGB values.

        Args:
            pixel: 16-bit RGB565 value

        Returns:
            Tuple of (red, green, blue) as 8-bit values (0-255)
        """
        # Extract 5-bit red (bits 11-15)
        red = (pixel >> 11) & 0x1F
        # Extract 6-bit green (bits 5-10)
        green = (pixel >> 5) & 0x3F
        # Extract 5-bit blue (bits 0-4)
        blue = pixel & 0x1F

        # Scale to 8-bit values (0-255)
        red = (red * 255) // 31
        green = (green * 255) // 63
        blue = (blue * 255) // 31

        return (red, green, blue)

    def _extract_preview_image(self, data: bytes, width: int, height: int) -> Image.Image:
        """
        Extract a preview image from raw RGB565 data.

        Args:
            data: Raw bytes containing RGB565 pixel data
            width: Image width in pixels
            height: Image height in pixels

        Returns:
            PIL Image object
        """
        expected_size = width * height * 2  # 2 bytes per pixel
        if len(data) < expected_size:
            raise ValueError(
                f"Insufficient data for {width}x{height} image. "
                f"Expected {expected_size} bytes, got {len(data)}"
            )

        # Create new RGB image
        img = Image.new('RGB', (width, height))
        pixels = []

        # Read each 16-bit pixel (big-endian)
        for i in range(0, expected_size, 2):
            # Unpack as big-endian unsigned short (>H)
            pixel = struct.unpack('>H', data[i:i+2])[0]
            rgb = self._decode_rgb565(pixel)
            pixels.append(rgb)

        # Put pixels into image
        img.putdata(pixels)
        return img

    def extract_thumbnails(self, output_dir: str = None) -> Tuple[Image.Image, Image.Image]:
        """
        Extract both thumbnail images from the .goo file.

        Args:
            output_dir: Optional directory to save the images. If None, images are not saved.

        Returns:
            Tuple of (small_preview, big_preview) as PIL Image objects
        """
        with open(self.filepath, 'rb') as f:
            # Calculate offset to preview images
            preview_offset = self._calculate_preview_offset()
            f.seek(preview_offset)

            # Extract small preview (116x116)
            small_size = self.SMALL_PREVIEW_SIZE[0] * self.SMALL_PREVIEW_SIZE[1] * 2
            small_data = f.read(small_size)
            small_preview = self._extract_preview_image(
                small_data,
                self.SMALL_PREVIEW_SIZE[0],
                self.SMALL_PREVIEW_SIZE[1]
            )

            # Extract big preview (290x290)
            big_size = self.BIG_PREVIEW_SIZE[0] * self.BIG_PREVIEW_SIZE[1] * 2
            big_data = f.read(big_size)
            big_preview = self._extract_preview_image(
                big_data,
                self.BIG_PREVIEW_SIZE[0],
                self.BIG_PREVIEW_SIZE[1]
            )

        # Save images if output directory is specified
        if output_dir:
            output_path = Path(output_dir)
            output_path.mkdir(parents=True, exist_ok=True)

            base_name = self.filepath.stem
            small_path = output_path / f"{base_name}_small.png"
            big_path = output_path / f"{base_name}_big.png"

            small_preview.save(small_path)
            big_preview.save(big_path)

            print(f"Saved small preview ({self.SMALL_PREVIEW_SIZE[0]}x{self.SMALL_PREVIEW_SIZE[1]}): {small_path}")
            print(f"Saved big preview ({self.BIG_PREVIEW_SIZE[0]}x{self.BIG_PREVIEW_SIZE[1]}): {big_path}")

        return small_preview, big_preview


def main():
    """Command-line interface for the thumbnail extractor."""
    if len(sys.argv) < 2:
        print("Usage: python extract_goo_thumbnails.py <goo_file> [output_directory]")
        print("\nExtracts thumbnail preview images from Elegoo .goo files.")
        print("\nArguments:")
        print("  goo_file          Path to the .goo file")
        print("  output_directory  Optional directory to save the thumbnails (default: current directory)")
        print("\nExample:")
        print("  python extract_goo_thumbnails.py model.goo")
        print("  python extract_goo_thumbnails.py model.goo ./thumbnails")
        sys.exit(1)

    goo_file = sys.argv[1]
    output_dir = sys.argv[2] if len(sys.argv) > 2 else "."

    try:
        extractor = GooThumbnailExtractor(goo_file)
        extractor.extract_thumbnails(output_dir)
        print("\nThumbnail extraction completed successfully!")
    except FileNotFoundError as e:
        print(f"Error: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error extracting thumbnails: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
