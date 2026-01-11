"""
Plugin Manager for ChitUI

Handles plugin discovery, loading, enabling/disabling, and lifecycle management.
"""

import os
import sys
import json
import importlib.util
from loguru import logger


class PluginManager:
    """Manages all ChitUI plugins"""

    def __init__(self, plugins_dir):
        """
        Initialize the plugin manager.

        Args:
            plugins_dir: Path to the plugins directory
        """
        self.plugins_dir = plugins_dir
        self.plugins = {}
        self.enabled_plugins = {}
        self.plugin_order = {}
        self.settings_file = os.path.expanduser('~/.chitui/plugin_settings.json')

        # Ensure plugins directory exists
        os.makedirs(self.plugins_dir, exist_ok=True)

        # Load plugin settings
        self.load_plugin_settings()

    def load_plugin_settings(self):
        """Load plugin settings from file"""
        if os.path.exists(self.settings_file):
            try:
                with open(self.settings_file, 'r') as f:
                    settings = json.load(f)
                    # Handle old format (just enabled plugins dict)
                    if isinstance(settings, dict) and 'enabled' not in settings:
                        self.enabled_plugins = settings
                        self.plugin_order = {}
                    else:
                        self.enabled_plugins = settings.get('enabled', {})
                        self.plugin_order = settings.get('order', {})
            except Exception as e:
                logger.error(f"Failed to load plugin settings: {e}")
                self.enabled_plugins = {}
                self.plugin_order = {}
        else:
            self.enabled_plugins = {}
            self.plugin_order = {}

    def save_plugin_settings(self):
        """Save plugin settings to file"""
        try:
            os.makedirs(os.path.dirname(self.settings_file), exist_ok=True)
            settings = {
                'enabled': self.enabled_plugins,
                'order': self.plugin_order
            }
            with open(self.settings_file, 'w') as f:
                json.dump(settings, f, indent=2)
        except Exception as e:
            logger.error(f"Failed to save plugin settings: {e}")

    def discover_plugins(self):
        """
        Discover all plugins in the plugins directory.

        Returns:
            Dict of plugin_name -> plugin_info
        """
        discovered = {}

        if not os.path.exists(self.plugins_dir):
            logger.warning(f"Plugins directory not found: {self.plugins_dir}")
            return discovered

        for item in os.listdir(self.plugins_dir):
            plugin_path = os.path.join(self.plugins_dir, item)

            # Skip if not a directory
            if not os.path.isdir(plugin_path):
                continue

            # Skip __pycache__ and hidden directories
            if item.startswith('__') or item.startswith('.'):
                continue

            # Check for plugin.json
            manifest_path = os.path.join(plugin_path, 'plugin.json')
            if not os.path.exists(manifest_path):
                logger.warning(f"Plugin {item} has no plugin.json, skipping")
                continue

            # Load manifest
            try:
                with open(manifest_path, 'r') as f:
                    manifest = json.load(f)

                discovered[item] = {
                    'name': manifest.get('name', item),
                    'version': manifest.get('version', '0.0.0'),
                    'author': manifest.get('author', 'Unknown'),
                    'description': manifest.get('description', ''),
                    'path': plugin_path,
                    'manifest': manifest,
                    'enabled': self.enabled_plugins.get(item, True)
                }
            except Exception as e:
                logger.error(f"Failed to load plugin {item}: {e}")
                continue

        return discovered

    def load_plugin(self, plugin_name, app, socketio, **kwargs):
        """
        Load and initialize a plugin.

        Args:
            plugin_name: Name of the plugin directory
            app: Flask app instance
            socketio: SocketIO instance
            **kwargs: Additional context to pass to plugin (e.g., printers, send_printer_cmd)

        Returns:
            Plugin instance or None
        """
        plugin_path = os.path.join(self.plugins_dir, plugin_name)

        if not os.path.exists(plugin_path):
            logger.error(f"Plugin not found: {plugin_name}")
            return None

        # Load the plugin module
        try:
            # Import the plugin's __init__.py
            spec = importlib.util.spec_from_file_location(
                f"plugins.{plugin_name}",
                os.path.join(plugin_path, "__init__.py")
            )
            if spec is None or spec.loader is None:
                logger.error(f"Failed to load plugin spec: {plugin_name}")
                return None

            module = importlib.util.module_from_spec(spec)
            sys.modules[f"plugins.{plugin_name}"] = module
            spec.loader.exec_module(module)

            # Get the plugin class (should be named after the directory)
            plugin_class = getattr(module, 'Plugin', None)
            if plugin_class is None:
                logger.error(f"Plugin {plugin_name} has no Plugin class")
                return None

            # Instantiate the plugin
            plugin_instance = plugin_class(plugin_path)

            # Install dependencies if needed
            logger.info(f"Installing dependencies for {plugin_name}...")
            if not plugin_instance.install_dependencies():
                logger.warning(f"Failed to install dependencies for {plugin_name}")

            # Initialize the plugin with additional context
            plugin_instance.on_startup(app, socketio, **kwargs)

            # Register SocketIO handlers
            plugin_instance.register_socket_handlers(socketio)

            # Register blueprint if available
            blueprint = plugin_instance.get_blueprint()
            if blueprint:
                app.register_blueprint(blueprint, url_prefix=f'/plugin/{plugin_name}')

            self.plugins[plugin_name] = plugin_instance
            logger.info(f"Plugin loaded: {plugin_name} v{plugin_instance.get_version()}")

            return plugin_instance

        except Exception as e:
            logger.error(f"Failed to load plugin {plugin_name}: {e}")
            import traceback
            traceback.print_exc()
            return None

    def load_all_plugins(self, app, socketio, **kwargs):
        """Load all enabled plugins with additional context"""
        discovered = self.discover_plugins()

        for plugin_name, info in discovered.items():
            if info['enabled']:
                logger.info(f"Loading plugin: {plugin_name}")
                self.load_plugin(plugin_name, app, socketio, **kwargs)
            else:
                logger.info(f"Plugin {plugin_name} is disabled, skipping")

    def enable_plugin(self, plugin_name):
        """Enable a plugin"""
        self.enabled_plugins[plugin_name] = True
        self.save_plugin_settings()

    def disable_plugin(self, plugin_name):
        """Disable a plugin"""
        self.enabled_plugins[plugin_name] = False
        self.save_plugin_settings()

        # NOTE: We keep the plugin loaded in memory because Flask doesn't
        # allow re-registering blueprints. The plugin stays loaded but won't
        # appear in the UI when disabled (filtered by get_enabled_plugins).

    def get_plugin(self, plugin_name):
        """Get a loaded plugin instance"""
        return self.plugins.get(plugin_name)

    def get_all_plugins(self):
        """Get all loaded plugins (including disabled ones)"""
        return self.plugins

    def get_enabled_plugins(self, sorted_by_order=False):
        """Get only enabled and loaded plugins, optionally sorted by order"""
        enabled = {}
        for plugin_name, plugin in self.plugins.items():
            if self.enabled_plugins.get(plugin_name, True):
                enabled[plugin_name] = plugin

        if sorted_by_order:
            # Sort by order (plugins without order go to end)
            sorted_items = sorted(
                enabled.items(),
                key=lambda x: self.plugin_order.get(x[0], 9999)
            )
            return dict(sorted_items)

        return enabled

    def set_plugin_order(self, order_list):
        """Set the display order for plugins

        Args:
            order_list: List of plugin names in desired order
        """
        self.plugin_order = {name: idx for idx, name in enumerate(order_list)}
        self.save_plugin_settings()

    def get_plugin_order(self):
        """Get the current plugin order"""
        return self.plugin_order

    def get_plugin_info(self):
        """Get information about all discovered plugins"""
        discovered = self.discover_plugins()
        info_list = []

        for plugin_name, info in discovered.items():
            info_list.append({
                'id': plugin_name,
                'name': info['name'],
                'version': info['version'],
                'author': info['author'],
                'description': info['description'],
                'enabled': info['enabled'],
                'loaded': plugin_name in self.plugins
            })

        return info_list

    def notify_printer_connected(self, printer_id, printer_info):
        """Notify all plugins that a printer connected"""
        for plugin in self.plugins.values():
            try:
                plugin.on_printer_connected(printer_id, printer_info)
            except Exception as e:
                logger.error(f"Plugin error in on_printer_connected: {e}")

    def notify_printer_disconnected(self, printer_id):
        """Notify all plugins that a printer disconnected"""
        for plugin in self.plugins.values():
            try:
                plugin.on_printer_disconnected(printer_id)
            except Exception as e:
                logger.error(f"Plugin error in on_printer_disconnected: {e}")

    def notify_printer_message(self, printer_id, message):
        """Notify all plugins of a printer message"""
        for plugin in self.plugins.values():
            try:
                plugin.on_printer_message(printer_id, message)
            except Exception as e:
                logger.error(f"Plugin error in on_printer_message: {e}")
