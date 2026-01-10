"""
File Manager Plugin for ChitUI

This plugin provides complete file management functionality including upload, delete, and print operations.
"""

from .file_manager import FileManagerPlugin

# Plugin class that the plugin manager will look for
Plugin = FileManagerPlugin
