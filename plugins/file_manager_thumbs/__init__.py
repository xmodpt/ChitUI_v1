"""
File Manager with Thumbnails Plugin for ChitUI

This plugin provides complete file management functionality with automatic
thumbnail extraction and display for .goo and .ctb 3D printer files.
"""

from .file_manager_thumbs import FileManagerThumbsPlugin

# Plugin class that the plugin manager will look for
Plugin = FileManagerThumbsPlugin
