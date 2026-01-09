# GOO Thumbnail Extractor

A Python script to extract thumbnail preview images from Elegoo `.goo` resin 3D printer files.

## Overview

The `.goo` format is Elegoo's proprietary file format for resin 3D printers. Each file contains two embedded preview thumbnails:
- **Small preview**: 116×116 pixels
- **Big preview**: 290×290 pixels

This script extracts these thumbnails and saves them as PNG images.

## Requirements

- Python 3.6 or higher
- Pillow (PIL) library

### Installation

Install the required dependency:

```bash
pip install Pillow
```

## Usage

### Command Line

```bash
python extract_goo_thumbnails.py <goo_file> [output_directory]
```

**Arguments:**
- `goo_file`: Path to the .goo file (required)
- `output_directory`: Directory to save the extracted thumbnails (optional, defaults to current directory)

**Examples:**

```bash
# Extract thumbnails to current directory
python extract_goo_thumbnails.py model.goo

# Extract thumbnails to a specific directory
python extract_goo_thumbnails.py model.goo ./thumbnails

# Extract from multiple files
python extract_goo_thumbnails.py prints/*.goo ./output
```

### Python API

You can also use the script as a Python module:

```python
from extract_goo_thumbnails import GooThumbnailExtractor

# Create extractor
extractor = GooThumbnailExtractor("model.goo")

# Extract and save thumbnails
small_preview, big_preview = extractor.extract_thumbnails(output_dir="./thumbnails")

# Or extract without saving
small_preview, big_preview = extractor.extract_thumbnails()

# Work with PIL Image objects
small_preview.show()  # Display the image
big_preview.save("custom_name.png")  # Save with custom name
```

## Output

The script generates two PNG files:
- `{filename}_small.png` - 116×116 pixel thumbnail
- `{filename}_big.png` - 290×290 pixel thumbnail

For example, if your input file is `dragon.goo`, the output will be:
- `dragon_small.png`
- `dragon_big.png`

## Technical Details

### File Format

The `.goo` format stores preview images in RGB565 format (16-bit color):
- **Red channel**: 5 bits (bits 11-15)
- **Green channel**: 6 bits (bits 5-10)
- **Blue channel**: 5 bits (bits 0-4)

All data is stored in big-endian byte order.

### Image Location

The preview images are located in the file header after these fields:
- Version (4 bytes)
- Software info (32 bytes)
- Software version (24 bytes)
- File time (24 bytes)
- Printer name (32 bytes)
- Printer type (32 bytes)
- Profile name (32 bytes)
- Anti-aliasing level (2 bytes)
- Grey level (2 bytes)
- Blur level (2 bytes)

The small preview starts at byte offset 186, followed immediately by the big preview.

## References

- [Official GOO Format Specification](https://github.com/elegooofficial/GOO/blob/main/Goo%20Format%20Spec%20V1.2.pdf)
- [Rust GOO Library](https://github.com/connorslade/goo)
- [UVtools](https://github.com/sn4k3/UVtools) - Supports .goo files

## License

This script is provided as-is for working with Elegoo .goo files.

## Troubleshooting

### "File not found" error
Make sure the path to your .goo file is correct and the file exists.

### "Pillow library is required" error
Install Pillow using: `pip install Pillow`

### Images look corrupted
The .goo file may be corrupted or from a different version of the format. Try opening the file with UVtools to verify it's valid.

### Wrong colors
The script assumes big-endian byte order and RGB565 format as per the official specification. If colors appear incorrect, the file may use a different encoding.
