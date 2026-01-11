#!/usr/bin/env python3
"""
GOO File Thumbnail Extractor - Batch Processing Mode

Automatically scans a directory for .goo files and extracts thumbnails.
Skips files that already have thumbnails extracted.
"""

import struct
import sys
import os
from pathlib import Path
from typing import Tuple, Optional, List

try:
    from PIL import Image
except ImportError:
    print("Error: Pillow library is required. Install with: pip install Pillow")
    sys.exit(1)


class GooThumbnailExtractor:
    """Extracts thumbnail images from .goo files with automatic orientation detection."""

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
        red = ((pixel >> 11) & 0x1F) * 255 // 31
        green = ((pixel >> 5) & 0x3F) * 255 // 63
        blue = (pixel & 0x1F) * 255 // 31
        return (red, green, blue)

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
        expected_size = width * height * 2
        if len(data) < expected_size:
            raise ValueError(f"Insufficient data for {width}x{height} image")

        img = Image.new('RGB', (width, height))
        pixels = []

        for i in range(0, expected_size, 2):
            pixel = struct.unpack(f'{endian}H', data[i:i+2])[0]
            rgb = self._decode_rgb565(pixel)
            pixels.append(rgb)

        img.putdata(pixels)
        return img

    def _detect_orientation(self, img: Image.Image) -> int:
        """
        Detect if image needs rotation by analyzing content distribution.
        Returns rotation angle needed (0, 90, 180, 270).
        """
        width, height = img.size
        pixels = list(img.getdata())
        
        # If image is square, no rotation needed
        if width == height:
            return 0
        
        # Check top vs bottom edges
        top_edge = sum((r + g + b) for r, g, b in [pixels[x] for x in range(min(width, len(pixels)))]) / width
        bottom_edge = sum((r + g + b) for r, g, b in [pixels[min((height-1) * width + x, len(pixels)-1)] for x in range(width)]) / width
        
        # Check left vs right edges
        left_edge = sum((r + g + b) for r, g, b in [pixels[min(y * width, len(pixels)-1)] for y in range(height)]) / height
        right_edge = sum((r + g + b) for r, g, b in [pixels[min(y * width + width - 1, len(pixels)-1)] for y in range(height)]) / height
        
        # If image is landscape but has more vertical edge content, rotate to portrait
        if width > height:
            vertical_edge_content = (left_edge + right_edge) / 2
            horizontal_edge_content = (top_edge + bottom_edge) / 2
            
            if vertical_edge_content > horizontal_edge_content * 1.2:
                return 270  # Rotate to portrait
        
        return 0

    def _smart_rotate(self, img: Image.Image, rotation: int = None) -> Image.Image:
        """
        Rotate image, using auto-detection if rotation is None.
        """
        if rotation is None:
            rotation = self._detect_orientation(img)
        
        if rotation == 0:
            return img
        elif rotation == 90:
            return img.rotate(-90, expand=True)
        elif rotation == 180:
            return img.rotate(180, expand=False)
        elif rotation == 270:
            return img.rotate(90, expand=True)
        else:
            return img

    def extract_thumbnails(self, output_dir: str = None, rotate: int = None) -> Tuple[Image.Image, Image.Image]:
        """
        Extract both thumbnail images from the .goo file.

        Args:
            output_dir: Optional directory to save the images
            rotate: Rotation angle or None for auto-detect

        Returns:
            Tuple of (small_preview, big_preview) as PIL Image objects
        """
        actual_offset = self._calculate_preview_offset()
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
        
        # Apply rotation
        small_preview = self._smart_rotate(small_preview, rotate)
        big_preview = self._smart_rotate(big_preview, rotate)

        # Save images if output directory is specified
        if output_dir:
            output_path = Path(output_dir)
            output_path.mkdir(parents=True, exist_ok=True)

            base_name = self.filepath.stem
            small_path = output_path / f"{base_name}_small.png"
            big_path = output_path / f"{base_name}_big.png"

            small_preview.save(small_path)
            big_preview.save(big_path)

        return small_preview, big_preview


def find_goo_files(directory: str) -> List[Path]:
    """
    Find all .goo files in a directory.
    
    Args:
        directory: Path to directory to scan
        
    Returns:
        List of Path objects for .goo files
    """
    dir_path = Path(directory)
    if not dir_path.exists():
        return []
    
    return sorted(dir_path.glob("*.goo"))


def check_if_processed(goo_file: Path) -> bool:
    """
    Check if thumbnails already exist for this .goo file.
    
    Args:
        goo_file: Path to .goo file
        
    Returns:
        True if thumbnails exist, False otherwise
    """
    base_name = goo_file.stem
    output_dir = goo_file.parent
    
    small_path = output_dir / f"{base_name}_small.png"
    big_path = output_dir / f"{base_name}_big.png"
    
    return small_path.exists() and big_path.exists()


def batch_process(directory: str = "./uploads", force: bool = False):
    """
    Batch process all .goo files in a directory.
    
    Args:
        directory: Directory to scan for .goo files
        force: If True, re-process files even if thumbnails exist
    """
    print("=" * 70)
    print("GOO Thumbnail Extractor - Batch Mode")
    print("=" * 70)
    print(f"\nScanning directory: {directory}")
    
    goo_files = find_goo_files(directory)
    
    if not goo_files:
        print(f"\n⚠ No .goo files found in {directory}")
        return
    
    print(f"Found {len(goo_files)} .goo file(s)\n")
    
    processed = 0
    skipped = 0
    failed = 0
    
    for goo_file in goo_files:
        print(f"Processing: {goo_file.name}")
        
        # Check if already processed
        if not force and check_if_processed(goo_file):
            print(f"  ⏭  Skipped (thumbnails already exist)")
            skipped += 1
            continue
        
        try:
            extractor = GooThumbnailExtractor(str(goo_file))
            extractor.extract_thumbnails(output_dir=str(goo_file.parent))
            print(f"  ✓ Extracted thumbnails")
            processed += 1
            
        except Exception as e:
            print(f"  ✗ Failed: {e}")
            failed += 1
    
    # Summary
    print("\n" + "=" * 70)
    print("Summary:")
    print(f"  Processed: {processed}")
    print(f"  Skipped:   {skipped}")
    print(f"  Failed:    {failed}")
    print("=" * 70)


def main():
    """Command-line interface for batch processing."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Extract thumbnails from Elegoo .goo files (batch mode)',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Process all .goo files in ./uploads (default)
  python goo_batch.py
  
  # Process all .goo files in a specific directory
  python goo_batch.py --dir /path/to/files
  
  # Force re-processing (ignore existing thumbnails)
  python goo_batch.py --force
  
  # Process a single file
  python goo_batch.py --file model.goo
        """
    )
    
    parser.add_argument(
        '--dir',
        default='./uploads',
        help='Directory to scan for .goo files (default: ./uploads)'
    )
    
    parser.add_argument(
        '--force',
        action='store_true',
        help='Force re-processing even if thumbnails exist'
    )
    
    parser.add_argument(
        '--file',
        help='Process a single .goo file instead of batch mode'
    )
    
    args = parser.parse_args()
    
    if args.file:
        # Single file mode
        try:
            print(f"Processing single file: {args.file}")
            extractor = GooThumbnailExtractor(args.file)
            file_path = Path(args.file)
            extractor.extract_thumbnails(output_dir=str(file_path.parent))
            print("✓ Thumbnail extraction completed!")
        except Exception as e:
            print(f"✗ Error: {e}")
            sys.exit(1)
    else:
        # Batch mode
        batch_process(args.dir, args.force)


if __name__ == "__main__":
    main()
