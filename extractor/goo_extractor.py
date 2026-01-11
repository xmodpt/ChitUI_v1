#!/usr/bin/env python3
"""
GOO File Thumbnail Extractor with Offset Fine-Tuning

Extracts thumbnail preview images from Elegoo .goo resin 3D printer files.
Includes automatic offset correction to handle alignment issues.
"""

import struct
import sys
from pathlib import Path
from typing import Tuple, Optional

try:
    from PIL import Image
except ImportError:
    print("Error: Pillow library is required. Install with: pip install Pillow")
    sys.exit(1)


class GooThumbnailExtractor:
    """Extracts thumbnail images from .goo files with offset correction."""

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

    def __init__(self, filepath: str, offset_adjustment: int = 8):
        """
        Initialize the extractor with a .goo file path.

        Args:
            filepath: Path to the .goo file
            offset_adjustment: Manual offset adjustment in bytes (default: +8 for GOO v3.0)
        """
        self.filepath = Path(filepath)
        self.offset_adjustment = offset_adjustment
        
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
        
        # Apply manual adjustment
        offset += self.offset_adjustment
        
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

    def _calculate_image_quality(self, img: Image.Image) -> float:
        """
        Calculate a quality score for an image to detect misalignment.
        
        Args:
            img: PIL Image object
            
        Returns:
            Quality score (higher is better)
        """
        pixels = list(img.getdata())
        
        # Check for edge discontinuity (left vs right edge)
        width, height = img.size
        
        # Sample left edge and right edge
        left_edge = [pixels[y * width] for y in range(0, height, 5)]
        right_edge = [pixels[y * width + width - 1] for y in range(0, height, 5)]
        
        # Calculate color difference between left and right edges
        edge_diff = 0
        for left, right in zip(left_edge, right_edge):
            diff = abs(left[0] - right[0]) + abs(left[1] - right[1]) + abs(left[2] - right[2])
            edge_diff += diff
        
        edge_diff /= len(left_edge)
        
        # Lower edge difference = better alignment
        # Also check spatial coherence (adjacent pixels should be similar)
        coherence_score = 0
        checks = 0
        
        for y in range(0, height, 10):
            for x in range(0, width - 1, 10):
                idx = y * width + x
                if idx + 1 < len(pixels):
                    p1, p2 = pixels[idx], pixels[idx + 1]
                    diff = abs(p1[0] - p2[0]) + abs(p1[1] - p2[1]) + abs(p1[2] - p2[2])
                    if diff < 50:  # Similar adjacent pixels
                        coherence_score += 1
                    checks += 1
        
        coherence = coherence_score / checks if checks > 0 else 0
        
        # Combined score: lower edge discontinuity + higher coherence
        quality = coherence * 100 - edge_diff / 10
        
        return quality

    def _extract_preview_image(self, data: bytes, width: int, height: int, 
                               endian: str = '>') -> Image.Image:
        """
        Extract a preview image from raw RGB565 data.

        Args:
            data: Raw bytes containing RGB565 pixel data
            width: Image width in pixels
            height: Image height in pixels
            endian: Byte order ('>' for big-endian, '<' for little-endian)

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

        # Read each 16-bit pixel
        for i in range(0, expected_size, 2):
            pixel = struct.unpack(f'{endian}H', data[i:i+2])[0]
            rgb = self._decode_rgb565(pixel)
            pixels.append(rgb)

        img.putdata(pixels)
        return img

    def find_best_offset(self, base_offset: int, search_range: int = 100) -> int:
        """
        Find the best offset by testing different values and scoring image quality.
        
        Args:
            base_offset: Starting offset to search from
            search_range: Number of bytes to search (+/- from base)
            
        Returns:
            Best offset found
        """
        print(f"Searching for best offset (base: {base_offset}, range: ±{search_range} bytes)...")
        
        best_offset = base_offset
        best_quality = -999999
        
        with open(self.filepath, 'rb') as f:
            # Try different offsets
            for offset_adj in range(-search_range, search_range + 1, 2):  # Step by 2 (RGB565 alignment)
                test_offset = base_offset + offset_adj
                
                try:
                    f.seek(test_offset)
                    
                    # Test with big preview (more data = better quality assessment)
                    big_size = self.BIG_PREVIEW_SIZE[0] * self.BIG_PREVIEW_SIZE[1] * 2
                    test_data = f.read(big_size)
                    
                    if len(test_data) < big_size:
                        continue
                    
                    # Try both endianness
                    for endian in ['>', '<']:
                        test_img = self._extract_preview_image(
                            test_data,
                            self.BIG_PREVIEW_SIZE[0],
                            self.BIG_PREVIEW_SIZE[1],
                            endian
                        )
                        
                        quality = self._calculate_image_quality(test_img)
                        
                        if quality > best_quality:
                            best_quality = quality
                            best_offset = test_offset
                            best_endian = endian
                            
                except Exception:
                    continue
                
                # Progress
                if offset_adj % 20 == 0:
                    print(f"  Testing offset {test_offset} (adj: {offset_adj:+d})...", end='\r')
        
        print(f"\n  Best offset: {best_offset} (adjustment: {best_offset - base_offset:+d} bytes)")
        print(f"  Quality score: {best_quality:.1f}")
        print(f"  Endianness: {'big' if best_endian == '>' else 'little'}")
        
        self.best_endian = best_endian
        return best_offset

    def extract_thumbnails(self, output_dir: str = None, auto_adjust: bool = False, 
                          rotate: int = 0) -> Tuple[Image.Image, Image.Image]:
        """
        Extract both thumbnail images from the .goo file.

        Args:
            output_dir: Optional directory to save the images
            auto_adjust: Automatically find best offset (slower but more accurate, default: False)
            rotate: Rotation angle in degrees (0, 90, 180, 270)

        Returns:
            Tuple of (small_preview, big_preview) as PIL Image objects
        """
        base_offset = self._calculate_preview_offset()
        
        # Auto-adjust offset if requested
        if auto_adjust:
            actual_offset = self.find_best_offset(base_offset, search_range=100)
            endian = self.best_endian
        else:
            actual_offset = base_offset
            endian = '>'  # Default big-endian
        
        with open(self.filepath, 'rb') as f:
            f.seek(actual_offset)

            # Extract small preview (116x116)
            small_size = self.SMALL_PREVIEW_SIZE[0] * self.SMALL_PREVIEW_SIZE[1] * 2
            small_data = f.read(small_size)
            small_preview = self._extract_preview_image(
                small_data,
                self.SMALL_PREVIEW_SIZE[0],
                self.SMALL_PREVIEW_SIZE[1],
                endian
            )

            # Extract big preview (290x290)
            big_size = self.BIG_PREVIEW_SIZE[0] * self.BIG_PREVIEW_SIZE[1] * 2
            big_data = f.read(big_size)
            big_preview = self._extract_preview_image(
                big_data,
                self.BIG_PREVIEW_SIZE[0],
                self.BIG_PREVIEW_SIZE[1],
                endian
            )
        
        # Apply rotation if requested
        if rotate != 0:
            if rotate == 90:
                small_preview = small_preview.rotate(-90, expand=True)
                big_preview = big_preview.rotate(-90, expand=True)
            elif rotate == 180:
                small_preview = small_preview.rotate(180)
                big_preview = big_preview.rotate(180)
            elif rotate == 270:
                small_preview = small_preview.rotate(90, expand=True)
                big_preview = big_preview.rotate(90, expand=True)

        # Save images if output directory is specified
        if output_dir:
            output_path = Path(output_dir)
            output_path.mkdir(parents=True, exist_ok=True)

            base_name = self.filepath.stem
            small_path = output_path / f"{base_name}_small.png"
            big_path = output_path / f"{base_name}_big.png"

            small_preview.save(small_path)
            big_preview.save(big_path)

            print(f"\nSaved small preview ({self.SMALL_PREVIEW_SIZE[0]}x{self.SMALL_PREVIEW_SIZE[1]}): {small_path}")
            print(f"Saved big preview ({self.BIG_PREVIEW_SIZE[0]}x{self.BIG_PREVIEW_SIZE[1]}): {big_path}")

        return small_preview, big_preview


def main():
    """Command-line interface for the thumbnail extractor."""
    if len(sys.argv) < 2:
        print("Usage: python extract_goo_thumbnails.py <goo_file> [output_directory] [--offset N] [--rotate DEGREES] [--auto]")
        print("\nExtracts thumbnail preview images from Elegoo .goo files.")
        print("\nArguments:")
        print("  goo_file          Path to the .goo file")
        print("  output_directory  Optional directory to save thumbnails (default: current directory)")
        print("  --offset N        Manual offset adjustment in bytes (default: +8)")
        print("  --rotate DEGREES  Rotate image (0, 90, 180, 270)")
        print("  --auto            Enable automatic offset detection (slower)")
        print("\nExample:")
        print("  python extract_goo_thumbnails.py model.goo")
        print("  python extract_goo_thumbnails.py model.goo ./thumbnails")
        print("  python extract_goo_thumbnails.py model.goo . --rotate 90  # Rotate 90° clockwise")
        sys.exit(1)

    goo_file = sys.argv[1]
    output_dir = "."
    offset_adj = 8  # Default to +8 for GOO v3.0 format
    auto_adjust = False  # Disabled by default for speed (use --auto to enable)
    rotate = 0  # No rotation by default
    
    # Parse arguments
    i = 2
    while i < len(sys.argv):
        arg = sys.argv[i]
        if arg == '--offset' and i + 1 < len(sys.argv):
            offset_adj = int(sys.argv[i + 1])
            i += 2
        elif arg == '--rotate' and i + 1 < len(sys.argv):
            rotate = int(sys.argv[i + 1])
            if rotate not in [0, 90, 180, 270]:
                print(f"Error: rotate must be 0, 90, 180, or 270")
                sys.exit(1)
            i += 2
        elif arg == '--auto':
            auto_adjust = True
            i += 1
        elif not arg.startswith('--'):
            output_dir = arg
            i += 1
        else:
            i += 1

    try:
        extractor = GooThumbnailExtractor(goo_file, offset_adjustment=offset_adj)
        extractor.extract_thumbnails(output_dir, auto_adjust=auto_adjust, rotate=rotate)
        print("\n✓ Thumbnail extraction completed successfully!")
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
