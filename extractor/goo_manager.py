#!/usr/bin/env python3
"""
GOO File Manager - Flask Web Application

Web interface for managing and viewing .goo 3D printer files with thumbnails.
Features:
- Dark mode file manager interface
- Upload .goo files
- Automatic thumbnail extraction
- View thumbnails and file details
"""

from flask import Flask, render_template, request, redirect, url_for, send_from_directory, jsonify
from werkzeug.utils import secure_filename
from pathlib import Path
import struct
from PIL import Image
from typing import Tuple, List, Dict
import os
from datetime import datetime

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = './uploads'
app.config['MAX_CONTENT_LENGTH'] = 500 * 1024 * 1024  # 500MB max file size
app.config['ALLOWED_EXTENSIONS'] = {'goo', 'ctb'}

# Ensure upload folder exists
Path(app.config['UPLOAD_FOLDER']).mkdir(parents=True, exist_ok=True)


class CtbThumbnailExtractor:
    """Extracts thumbnail images from .ctb files (Chitubox format)."""
    
    def __init__(self, filepath: str):
        self.filepath = Path(filepath)
        
        if not self.filepath.exists():
            raise FileNotFoundError(f"File not found: {filepath}")
    
    def _decode_rgb565(self, pixel: int) -> Tuple[int, int, int]:
        """Decode RGB565 to RGB888."""
        red = ((pixel >> 11) & 0x1F) * 255 // 31
        green = ((pixel >> 5) & 0x3F) * 255 // 63
        blue = (pixel & 0x1F) * 255 // 31
        return (red, green, blue)
    
    def _extract_preview_image(self, data: bytes, width: int, height: int) -> Image.Image:
        """Extract preview image from RGB565 data."""
        expected_size = width * height * 2
        if len(data) < expected_size:
            raise ValueError(f"Insufficient data")
        
        img = Image.new('RGB', (width, height))
        pixels = []
        
        for i in range(0, expected_size, 2):
            # CTB uses little-endian
            pixel = struct.unpack('<H', data[i:i+2])[0]
            pixels.append(self._decode_rgb565(pixel))
        
        img.putdata(pixels)
        return img
    
    def _detect_orientation(self, img: Image.Image) -> int:
        """Detect if image needs rotation."""
        width, height = img.size
        pixels = list(img.getdata())
        
        try:
            # Calculate edge content
            top_sum = sum((r + g + b) for r, g, b in [pixels[x] for x in range(min(width, len(pixels)))])
            top_edge = top_sum / width if width > 0 else 0
            
            bottom_start = (height - 1) * width
            bottom_sum = sum((r + g + b) for r, g, b in [pixels[min(bottom_start + x, len(pixels)-1)] for x in range(width)])
            bottom_edge = bottom_sum / width if width > 0 else 0
            
            left_sum = sum((r + g + b) for r, g, b in [pixels[min(y * width, len(pixels)-1)] for y in range(height)])
            left_edge = left_sum / height if height > 0 else 0
            
            right_sum = sum((r + g + b) for r, g, b in [pixels[min(y * width + width - 1, len(pixels)-1)] for y in range(height)])
            right_edge = right_sum / height if height > 0 else 0
            
            vertical_edge_content = (left_edge + right_edge) / 2
            horizontal_edge_content = (top_edge + bottom_edge) / 2
            
            if vertical_edge_content > horizontal_edge_content * 1.1:
                print(f"  Rotating CTB (V={vertical_edge_content:.1f} > H={horizontal_edge_content:.1f})")
                return 270
        except:
            pass
        
        return 0
    
    def _smart_rotate(self, img: Image.Image) -> Image.Image:
        """Rotate image if needed."""
        rotation = self._detect_orientation(img)
        if rotation == 270:
            return img.rotate(90, expand=True)
        return img
    
    def extract_thumbnails(self, output_dir: str = None) -> Tuple[Image.Image, Image.Image]:
        """Extract thumbnails from CTB file."""
        with open(self.filepath, 'rb') as f:
            # Read CTB header
            magic = struct.unpack('<I', f.read(4))[0]
            version = struct.unpack('<I', f.read(4))[0]
            
            # Skip to preview offsets (at offset 8)
            f.seek(8)
            preview_small_offset = struct.unpack('<I', f.read(4))[0]
            preview_large_offset = struct.unpack('<I', f.read(4))[0]
            
            # Extract small preview
            f.seek(preview_small_offset)
            small_width = struct.unpack('<I', f.read(4))[0]
            small_height = struct.unpack('<I', f.read(4))[0]
            small_data_size = struct.unpack('<I', f.read(4))[0]
            small_data = f.read(small_data_size)
            
            small_preview = self._extract_preview_image(small_data, small_width, small_height)
            
            # Extract large preview
            f.seek(preview_large_offset)
            large_width = struct.unpack('<I', f.read(4))[0]
            large_height = struct.unpack('<I', f.read(4))[0]
            large_data_size = struct.unpack('<I', f.read(4))[0]
            large_data = f.read(large_data_size)
            
            big_preview = self._extract_preview_image(large_data, large_width, large_height)
        
        # Rotate if needed
        small_preview = self._smart_rotate(small_preview)
        big_preview = self._smart_rotate(big_preview)
        
        # Save if output directory specified
        if output_dir:
            output_path = Path(output_dir)
            base_name = self.filepath.stem
            small_path = output_path / f"{base_name}_small.png"
            big_path = output_path / f"{base_name}_big.png"
            
            small_preview.save(small_path)
            big_preview.save(big_path)
            
            print(f"  Saved CTB thumbnails for {base_name}")
        
        return small_preview, big_preview


class GooThumbnailExtractor:
    """Extracts thumbnail images from .goo files."""

    VERSION_SIZE = 4
    SOFTWARE_INFO_SIZE = 32
    SOFTWARE_VERSION_SIZE = 24
    FILE_TIME_SIZE = 24
    PRINTER_NAME_SIZE = 32
    PRINTER_TYPE_SIZE = 32
    PROFILE_NAME_SIZE = 32

    SMALL_PREVIEW_SIZE = (116, 116)
    BIG_PREVIEW_SIZE = (290, 290)

    def __init__(self, filepath: str, offset_adjustment: int = 8):
        self.filepath = Path(filepath)
        self.offset_adjustment = offset_adjustment
        
        if not self.filepath.exists():
            raise FileNotFoundError(f"File not found: {filepath}")

    def _calculate_preview_offset(self) -> int:
        offset = 0
        offset += self.VERSION_SIZE
        offset += self.SOFTWARE_INFO_SIZE
        offset += self.SOFTWARE_VERSION_SIZE
        offset += self.FILE_TIME_SIZE
        offset += self.PRINTER_NAME_SIZE
        offset += self.PRINTER_TYPE_SIZE
        offset += self.PROFILE_NAME_SIZE
        offset += 2 + 2 + 2
        offset += self.offset_adjustment
        return offset

    def _decode_rgb565(self, pixel: int) -> Tuple[int, int, int]:
        red = ((pixel >> 11) & 0x1F) * 255 // 31
        green = ((pixel >> 5) & 0x3F) * 255 // 63
        blue = (pixel & 0x1F) * 255 // 31
        return (red, green, blue)

    def _extract_preview_image(self, data: bytes, width: int, height: int, 
                               endian: str = '>') -> Image.Image:
        expected_size = width * height * 2
        if len(data) < expected_size:
            raise ValueError(f"Insufficient data")

        img = Image.new('RGB', (width, height))
        pixels = []

        for i in range(0, expected_size, 2):
            pixel = struct.unpack(f'{endian}H', data[i:i+2])[0]
            pixels.append(self._decode_rgb565(pixel))

        img.putdata(pixels)
        return img

    def _detect_orientation(self, img: Image.Image) -> int:
        width, height = img.size
        
        # Even if image is square, check if CONTENT inside is landscape/portrait
        pixels = list(img.getdata())
        
        try:
            # Calculate edge content
            top_sum = 0
            for x in range(width):
                if x < len(pixels):
                    r, g, b = pixels[x]
                    top_sum += r + g + b
            top_edge = top_sum / width if width > 0 else 0
            
            bottom_start = (height - 1) * width
            bottom_sum = 0
            for x in range(width):
                idx = bottom_start + x
                if idx < len(pixels):
                    r, g, b = pixels[idx]
                    bottom_sum += r + g + b
            bottom_edge = bottom_sum / width if width > 0 else 0
            
            left_sum = 0
            for y in range(height):
                idx = y * width
                if idx < len(pixels):
                    r, g, b = pixels[idx]
                    left_sum += r + g + b
            left_edge = left_sum / height if height > 0 else 0
            
            right_sum = 0
            for y in range(height):
                idx = y * width + (width - 1)
                if idx < len(pixels):
                    r, g, b = pixels[idx]
                    right_sum += r + g + b
            right_edge = right_sum / height if height > 0 else 0
            
            # Calculate content distribution
            vertical_edge_content = (left_edge + right_edge) / 2
            horizontal_edge_content = (top_edge + bottom_edge) / 2
            
            # If more content on vertical edges, rotate to portrait
            if vertical_edge_content > horizontal_edge_content * 1.1:
                print(f"  Rotating (V={vertical_edge_content:.1f} > H={horizontal_edge_content:.1f})")
                return 270
        except Exception as e:
            print(f"  Rotation detection error: {e}")
        
        return 0

    def _smart_rotate(self, img: Image.Image) -> Image.Image:
        rotation = self._detect_orientation(img)
        
        if rotation == 270:
            return img.rotate(90, expand=True)
        return img

    def extract_thumbnails(self, output_dir: str = None) -> Tuple[Image.Image, Image.Image]:
        actual_offset = self._calculate_preview_offset()
        endian = '>'
        
        with open(self.filepath, 'rb') as f:
            f.seek(actual_offset)

            small_size = self.SMALL_PREVIEW_SIZE[0] * self.SMALL_PREVIEW_SIZE[1] * 2
            small_data = f.read(small_size)
            small_preview = self._extract_preview_image(
                small_data,
                self.SMALL_PREVIEW_SIZE[0],
                self.SMALL_PREVIEW_SIZE[1],
                endian
            )

            big_size = self.BIG_PREVIEW_SIZE[0] * self.BIG_PREVIEW_SIZE[1] * 2
            big_data = f.read(big_size)
            big_preview = self._extract_preview_image(
                big_data,
                self.BIG_PREVIEW_SIZE[0],
                self.BIG_PREVIEW_SIZE[1],
                endian
            )
        
        small_preview = self._smart_rotate(small_preview)
        big_preview = self._smart_rotate(big_preview)

        if output_dir:
            output_path = Path(output_dir)
            base_name = self.filepath.stem
            small_path = output_path / f"{base_name}_small.png"
            big_path = output_path / f"{base_name}_big.png"

            # Save the rotated images
            small_preview.save(small_path)
            big_preview.save(big_path)
            
            print(f"  Saved thumbnails for {base_name}")

        return small_preview, big_preview


def allowed_file(filename):
    """Check if file has allowed extension."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']


def get_file_info(filepath: Path) -> Dict:
    """Get information about a .goo file."""
    stat = filepath.stat()
    
    return {
        'name': filepath.name,
        'stem': filepath.stem,
        'size': stat.st_size,
        'size_mb': round(stat.st_size / (1024 * 1024), 2),
        'modified': datetime.fromtimestamp(stat.st_mtime).strftime('%Y-%m-%d %H:%M:%S'),
        'has_small_thumb': (filepath.parent / f"{filepath.stem}_small.png").exists(),
        'has_big_thumb': (filepath.parent / f"{filepath.stem}_big.png").exists(),
    }


def get_all_goo_files() -> List[Dict]:
    """Get list of all .goo and .ctb files with their info."""
    upload_dir = Path(app.config['UPLOAD_FOLDER'])
    goo_files = list(upload_dir.glob("*.goo"))
    ctb_files = list(upload_dir.glob("*.ctb"))
    all_files = sorted(goo_files + ctb_files, key=lambda x: x.stat().st_mtime, reverse=True)
    
    return [get_file_info(f) for f in all_files]


def extract_thumbnail_for_file(filename: str) -> bool:
    """Extract thumbnail for a specific file (GOO or CTB)."""
    filepath = Path(app.config['UPLOAD_FOLDER']) / filename
    
    if not filepath.exists():
        return False
    
    try:
        # Determine file type and use appropriate extractor
        if filepath.suffix.lower() == '.goo':
            extractor = GooThumbnailExtractor(str(filepath))
        elif filepath.suffix.lower() == '.ctb':
            extractor = CtbThumbnailExtractor(str(filepath))
        else:
            return False
        
        extractor.extract_thumbnails(output_dir=str(filepath.parent))
        return True
    except Exception as e:
        print(f"Error extracting thumbnail: {e}")
        import traceback
        traceback.print_exc()
        return False


@app.route('/')
def index():
    """Main page - file manager view."""
    files = get_all_goo_files()
    return render_template('index.html', files=files)


@app.route('/upload', methods=['POST'])
def upload_file():
    """Handle file upload."""
    if 'file' not in request.files:
        return jsonify({'error': 'No file part'}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        filepath = Path(app.config['UPLOAD_FOLDER']) / filename
        
        # Save file
        file.save(str(filepath))
        
        # Extract thumbnail
        success = extract_thumbnail_for_file(filename)
        
        return jsonify({
            'success': True,
            'filename': filename,
            'thumbnail_extracted': success
        })
    
    return jsonify({'error': 'Invalid file type'}), 400


@app.route('/extract/<filename>')
def extract_thumbnail(filename):
    """Extract thumbnail for a specific file."""
    success = extract_thumbnail_for_file(filename)
    
    if success:
        return jsonify({'success': True})
    else:
        return jsonify({'error': 'Failed to extract thumbnail'}), 500


@app.route('/delete/<filename>')
def delete_file(filename):
    """Delete a .goo file and its thumbnails."""
    filepath = Path(app.config['UPLOAD_FOLDER']) / secure_filename(filename)
    
    if not filepath.exists():
        return jsonify({'error': 'File not found'}), 404
    
    try:
        # Delete main file
        filepath.unlink()
        
        # Delete thumbnails
        small_thumb = filepath.parent / f"{filepath.stem}_small.png"
        big_thumb = filepath.parent / f"{filepath.stem}_big.png"
        
        if small_thumb.exists():
            small_thumb.unlink()
        if big_thumb.exists():
            big_thumb.unlink()
        
        return jsonify({'success': True})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/uploads/<filename>')
def serve_upload(filename):
    """Serve uploaded files."""
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


@app.route('/api/files')
def api_files():
    """API endpoint to get file list."""
    files = get_all_goo_files()
    return jsonify(files)


if __name__ == '__main__':
    print("=" * 70)
    print("GOO/CTB File Manager - Web Interface")
    print("=" * 70)
    print(f"\nUpload folder: {Path(app.config['UPLOAD_FOLDER']).absolute()}")
    print("Supported formats: .goo (Elegoo), .ctb (Chitubox)")
    print("\nStarting server...")
    print("Open your browser to: http://localhost:5000")
    print("\nPress Ctrl+C to stop the server")
    print("=" * 70)
    
    app.run(debug=True, host='0.0.0.0', port=5000)
