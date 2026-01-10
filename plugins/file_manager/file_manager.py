"""
File Manager Plugin for ChitUI

Provides complete file management functionality including:
- File upload (USB gadget and network modes)
- File deletion
- File printing
- Storage monitoring
- USB gadget management
"""

from plugins.base import ChitUIPlugin
from flask import Blueprint, request, Response, jsonify
from werkzeug.utils import secure_filename
from loguru import logger
import os
import hashlib
import uuid
import threading
import time
import subprocess
import requests
import json


class FileManagerPlugin(ChitUIPlugin):
    def __init__(self, plugin_dir):
        super().__init__(plugin_dir)
        self.app = None
        self.socketio = None
        self.printers = None
        self.send_printer_cmd = None

        # Upload configuration
        self.DATA_FOLDER = None
        self.UPLOAD_FOLDER = None
        self.USB_GADGET_FOLDER = '/mnt/usb_share'
        self.ALLOWED_EXTENSIONS = {'ctb', 'goo', 'prz'}
        self.USE_USB_GADGET = False
        self.USB_AUTO_REFRESH = False
        self.ENABLE_USB_GADGET = True

        # Upload progress tracking (thread-safe)
        self.uploadProgress = {}
        self.uploadProgressLock = threading.Lock()
        self.uploadLock = threading.Lock()

        logger.info("File Manager Plugin initialized")

    def get_name(self):
        """Return the plugin name"""
        return "File Manager"

    def get_version(self):
        """Return the plugin version"""
        return "1.0.0"

    def get_description(self):
        """Return the plugin description"""
        return "Complete file management system with upload, download, delete, and print capabilities"

    def get_ui_integration(self):
        """Return UI integration configuration"""
        return {
            'type': 'card',
            'location': 'main',
            'icon': 'bi-folder2-open',
            'title': 'Files',
            'template': 'file_manager.html'
        }

    def on_startup(self, app, socketio, **kwargs):
        """Initialize plugin on startup"""
        self.app = app
        self.socketio = socketio
        self.printers = kwargs.get('printers', {})
        self.send_printer_cmd = kwargs.get('send_printer_cmd')

        # Get configuration from environment or app config
        self.DATA_FOLDER = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'data')
        self.USB_GADGET_FOLDER = os.environ.get('USB_GADGET_PATH', '/mnt/usb_share')
        self.ENABLE_USB_GADGET = os.environ.get('ENABLE_USB_GADGET', 'true').lower() not in ['0', 'false', 'no', 'off']
        self.USB_AUTO_REFRESH = os.environ.get('USB_AUTO_REFRESH', 'false').lower() not in ['0', 'false', 'no', 'off']

        # Check if USB gadget is available and writable
        self._check_usb_gadget()

        # Set upload folder
        if not self.USE_USB_GADGET:
            self.UPLOAD_FOLDER = os.path.join(self.DATA_FOLDER, 'uploads')
            os.makedirs(self.UPLOAD_FOLDER, exist_ok=True)

        # Store in app config for access from routes
        app.config['FILE_MANAGER_UPLOAD_FOLDER'] = self.UPLOAD_FOLDER

        logger.info(f"File Manager - USB Gadget: {'enabled' if self.USE_USB_GADGET else 'disabled'}")
        logger.info(f"File Manager - Upload folder: {self.UPLOAD_FOLDER}")
        logger.info(f"File Manager - Auto-refresh: {'enabled' if self.USB_AUTO_REFRESH else 'disabled'}")

    def _check_usb_gadget(self):
        """Check if USB gadget is available and writable"""
        if not self.ENABLE_USB_GADGET:
            logger.info("USB gadget mode disabled by configuration")
            self.USE_USB_GADGET = False
            return

        if not os.path.exists(self.USB_GADGET_FOLDER):
            logger.info(f"USB gadget folder not found: {self.USB_GADGET_FOLDER}")
            self.USE_USB_GADGET = False
            return

        try:
            # Test write access
            test_file = os.path.join(self.USB_GADGET_FOLDER, '.chitui_test')
            with open(test_file, 'w') as f:
                f.write('test')
            os.remove(test_file)
            logger.info(f"✓ USB gadget found and writable at {self.USB_GADGET_FOLDER}")
            self.UPLOAD_FOLDER = self.USB_GADGET_FOLDER
            self.USE_USB_GADGET = True
        except (PermissionError, OSError) as e:
            logger.warning(f"USB gadget folder not writable: {e}")
            self.USE_USB_GADGET = False

    def get_blueprint(self):
        """Return Flask Blueprint for file manager routes"""
        bp = Blueprint('file_manager', __name__,
                      static_folder='static',
                      static_url_path='/static')

        @bp.route('/upload', methods=['GET', 'POST'])
        def upload_file():
            if request.method == 'POST':
                # Check if another upload is in progress
                if not self.uploadLock.acquire(blocking=False):
                    logger.warning("Upload already in progress")
                    return Response('{"upload": "error", "msg": "Another upload is already in progress. Please wait."}',
                                  status=429, mimetype="application/json")

                try:
                    if 'file' not in request.files:
                        logger.error("No 'file' parameter in request.")
                        return Response('{"upload": "error", "msg": "Malformed request - no file."}',
                                      status=400, mimetype="application/json")

                    file = request.files['file']
                    if file.filename == '':
                        logger.error('No file selected to be uploaded.')
                        return Response('{"upload": "error", "msg": "No file selected."}',
                                      status=400, mimetype="application/json")

                    form_data = request.form.to_dict()
                    if 'printer' not in form_data or form_data['printer'] == "":
                        logger.error("No 'printer' parameter in request.")
                        return Response('{"upload": "error", "msg": "Malformed request - no printer."}',
                                      status=400, mimetype="application/json")

                    printer = self.printers[form_data['printer']]
                    if file and not self._allowed_file(file.filename):
                        logger.error("Invalid filetype.")
                        return Response('{"upload": "error", "msg": "Invalid filetype."}',
                                      status=400, mimetype="application/json")

                    # Get destination (local or usb)
                    destination = form_data.get('destination', 'usb' if self.USE_USB_GADGET else 'local')
                    logger.info(f"Upload destination: {destination}")
                    logger.info(f"USB gadget mode: {'enabled' if self.USE_USB_GADGET else 'disabled'}")

                    # Generate unique upload ID for progress tracking
                    upload_id = form_data.get('upload_id', str(uuid.uuid4()))
                    filename = secure_filename(file.filename)

                    # Determine upload path based on printer's USB device type
                    printer_id = form_data['printer']
                    usb_device_type = self.printers[printer_id].get('usb_device_type', 'physical')

                    # For virtual USB gadget, always use /mnt/usb_share
                    if destination == 'usb' and usb_device_type == 'virtual':
                        upload_folder = self.USB_GADGET_FOLDER
                        # Verify mount point is available and writable
                        if not os.path.exists(upload_folder):
                            logger.error(f"USB gadget folder not found: {upload_folder}")
                            return Response('{"upload": "error", "msg": "USB mount point not found. Please mount USB gadget first."}',
                                          status=500, mimetype="application/json")
                        if not os.access(upload_folder, os.W_OK):
                            logger.error(f"USB gadget folder not writable: {upload_folder}")
                            return Response('{"upload": "error", "msg": "USB mount point not writable. Check permissions."}',
                                          status=500, mimetype="application/json")
                    else:
                        upload_folder = self.UPLOAD_FOLDER

                    filepath = os.path.join(upload_folder, filename)
                    logger.info(f"Saving '{filename}' to {filepath} (upload_id: {upload_id})")
                    logger.info(f"USB device type: {usb_device_type}, Upload folder: {upload_folder}")

                    try:
                        # Initialize progress
                        with self.uploadProgressLock:
                            self.uploadProgress[upload_id] = 0

                        # Save file with progress tracking
                        file.save(filepath)
                        logger.info(f"✓ File '{filename}' saved successfully!")

                        # Check destination: if user selected USB and printer is configured for USB, process accordingly
                        if destination == 'usb' and (self.USE_USB_GADGET or usb_device_type == 'virtual'):
                            # File saved to USB gadget - update progress
                            with self.uploadProgressLock:
                                self.uploadProgress[upload_id] = 50
                            self.socketio.emit('upload_progress', {'upload_id': upload_id, 'progress': 50}, namespace='/')

                            logger.info("Destination: USB Gadget (Pi's virtual USB)")

                            # For virtual USB gadget, unmount/mount/reload
                            if usb_device_type == 'virtual':
                                logger.info("Virtual USB gadget detected - performing unmount/mount/reload")
                                with self.uploadProgressLock:
                                    self.uploadProgress[upload_id] = 75
                                self.socketio.emit('upload_progress', {'upload_id': upload_id, 'progress': 75}, namespace='/')

                                self._reload_usb_gadget()
                                refresh_success = True
                            else:
                                # Physical USB or auto-refresh disabled
                                refresh_success = False
                                if self.USB_AUTO_REFRESH:
                                    logger.info("Triggering USB gadget refresh to notify printer...")
                                    with self.uploadProgressLock:
                                        self.uploadProgress[upload_id] = 75
                                    self.socketio.emit('upload_progress', {'upload_id': upload_id, 'progress': 75}, namespace='/')
                                    refresh_success = self._trigger_usb_gadget_refresh()

                                    if refresh_success:
                                        logger.info("Waiting for printer to detect new file...")
                                        time.sleep(2)
                                else:
                                    logger.info("USB auto-refresh disabled (USB_AUTO_REFRESH=false)")
                                    logger.info("💡 Manually refresh on printer or set USB_AUTO_REFRESH=true")

                            # Set progress to 100%
                            with self.uploadProgressLock:
                                self.uploadProgress[upload_id] = 100
                            self.socketio.emit('upload_progress', {'upload_id': upload_id, 'progress': 100}, namespace='/')

                            logger.info("✓ Upload to USB gadget complete!")

                            # Emit page refresh for virtual USB gadget
                            if usb_device_type == 'virtual':
                                self.socketio.emit('refresh_page', {'reason': 'virtual_usb_upload'})
                                msg = "File saved to USB gadget. Page will refresh to show updated file list."
                            elif self.USB_AUTO_REFRESH and refresh_success:
                                msg = "File saved to USB gadget. Printer should detect it automatically."
                            elif self.USB_AUTO_REFRESH and not refresh_success:
                                msg = "File saved to USB gadget. Auto-refresh failed - manually refresh on printer."
                                logger.warning("USB gadget refresh failed - manual intervention may be needed")
                                logger.info("💡 To enable automatic refresh, run: sudo python3 main.py")
                            else:
                                msg = "File saved to USB gadget. Manually refresh on printer or reconnect USB to detect it."

                            return Response(
                                json.dumps({
                                    "upload": "success",
                                    "msg": msg,
                                    "upload_id": upload_id,
                                    "usb_gadget": True,
                                    "filename": filename,
                                    "refresh_triggered": refresh_success
                                }),
                                status=200,
                                mimetype="application/json"
                            )
                        else:
                            # Upload to printer via network (either local or usb storage on printer)
                            logger.info(f"Uploading to printer '{printer['name']}' - {destination} storage...")
                            success = self._upload_file_to_printer(printer['ip'], filepath, upload_id, destination)

                            if success:
                                # Emit page refresh for physical USB uploads
                                if destination == 'usb':
                                    self.socketio.emit('refresh_page', {'reason': 'physical_usb_upload'})

                                return Response(
                                    json.dumps({
                                        "upload": "success",
                                        "msg": "File uploaded to printer",
                                        "upload_id": upload_id,
                                        "usb_gadget": False,
                                        "filename": filename
                                    }),
                                    status=200,
                                    mimetype="application/json"
                                )
                            else:
                                return Response(
                                    json.dumps({
                                        "upload": "error",
                                        "msg": "Failed to upload to printer",
                                        "upload_id": upload_id,
                                        "usb_gadget": False
                                    }),
                                    status=500,
                                    mimetype="application/json"
                                )

                    except Exception as e:
                        logger.error(f"Upload failed: {e}")
                        return Response(f'{{"upload": "error", "msg": "Upload failed: {str(e)}", "upload_id": "{upload_id}"}}',
                                      status=500, mimetype="application/json")
                finally:
                    # Always release the lock
                    self.uploadLock.release()
            else:
                return Response("u r doin it rong", status=405, mimetype='text/plain')

        @bp.route('/usb-gadget/storage', methods=['GET'])
        def get_usb_gadget_storage():
            """Get USB gadget storage information"""
            try:
                import shutil

                # Check if USB gadget is available
                if not os.path.exists('/mnt/usb_share'):
                    return jsonify({
                        "success": False,
                        "available": False,
                        "message": "USB gadget mount point not found"
                    })

                # Get disk usage for /mnt/usb_share
                stat = shutil.disk_usage('/mnt/usb_share')

                # Also try to get the size of /piusb.bin for total capacity
                total_bytes = stat.total
                if os.path.exists('/piusb.bin'):
                    bin_size = os.path.getsize('/piusb.bin')
                    # Use bin file size if it's larger (more accurate)
                    if bin_size > total_bytes:
                        total_bytes = bin_size

                used_bytes = stat.used
                free_bytes = stat.free

                # Calculate percentage used
                percent_used = (used_bytes / total_bytes * 100) if total_bytes > 0 else 0

                return jsonify({
                    "success": True,
                    "available": True,
                    "total": total_bytes,
                    "used": used_bytes,
                    "free": free_bytes,
                    "percent": round(percent_used, 1)
                })
            except Exception as e:
                logger.error(f"Error getting USB gadget storage: {e}")
                return jsonify({
                    "success": False,
                    "available": False,
                    "message": str(e)
                })

        return bp

    def register_socket_handlers(self, socketio):
        """Register Socket.IO event handlers"""

        @socketio.on('action_delete')
        def handle_action_delete(data):
            logger.debug(f'client.action_delete >> {json.dumps(data)}')

            printer_id = data['id']
            file_path = data['data']

            # Check if this is a virtual USB gadget and file is on USB
            if printer_id in self.printers:
                usb_device_type = self.printers[printer_id].get('usb_device_type', 'physical')

                # For virtual USB gadget, delete from mount point directly
                if usb_device_type == 'virtual' and file_path.startswith('/usb/'):
                    logger.info(f">>> Deleting file from VIRTUAL USB GADGET: {file_path}")
                    logger.info("✓ Using DIRECT MOUNT delete method (direct file removal)")

                    # Delete directly from mount point
                    success = self._delete_file_from_mount(file_path)

                    if success:
                        logger.info("✓ File deleted from mount point successfully")
                        # Refresh the UI after delete
                        socketio.emit('refresh_page', {'reason': 'file_deleted_from_usb'})
                    else:
                        logger.error("✗ Failed to delete file from mount point")
                    return

            # For all other cases (physical USB or local storage), use SDCP delete command
            logger.info(f">>> Deleting file via SDCP command: {file_path}")
            logger.info("✓ Using LOCAL STORAGE delete method (SDCP 259 command to printer)")
            self.send_printer_cmd(printer_id, 259, {"FileList": [file_path]})

        @socketio.on('action_print')
        def handle_action_print(data):
            logger.debug(f'client.action_print >> {json.dumps(data)}')
            self.send_printer_cmd(data['id'], 128, {
                "Filename": data['data'], "StartLayer": 0})

    def _allowed_file(self, filename):
        """Check if file extension is allowed"""
        return '.' in filename and \
               filename.rsplit('.', 1)[1].lower() in self.ALLOWED_EXTENSIONS

    def _upload_file_to_printer(self, printer_ip, filepath, upload_id, destination='local'):
        """Upload file to printer in chunks via HTTP API"""
        part_size = 1048576  # 1MB chunks
        filename = os.path.basename(filepath)

        # Initialize progress for this upload
        with self.uploadProgressLock:
            self.uploadProgress[upload_id] = 0

        # Calculate MD5 hash
        md5_hash = hashlib.md5()
        with open(filepath, "rb") as f:
            for byte_block in iter(lambda: f.read(4096), b""):
                md5_hash.update(byte_block)

        file_stats = os.stat(filepath)
        post_data = {
            'S-File-MD5': md5_hash.hexdigest(),
            'Check': 1,
            'Offset': 0,
            'Uuid': uuid.uuid4(),
            'TotalSize': file_stats.st_size,
        }

        # Use same endpoint for both destinations
        url = 'http://{ip}:3030/uploadFile/upload'.format(ip=printer_ip)

        # USB uploads: send complete file in one request (no chunking)
        if destination == 'usb':
            logger.info(f"Upload destination: USB storage")
            logger.info(f"Uploading complete file to USB storage (size: {file_stats.st_size} bytes)...")

            # Update progress to 30% (upload starting)
            with self.uploadProgressLock:
                self.uploadProgress[upload_id] = 30

            # Read entire file
            with open(filepath, 'rb') as f:
                file_content = f.read()

            # Try different upload methods for USB
            upload_methods = [
                {
                    'name': 'USB complete upload',
                    'post_data': post_data,
                    'files': {'file': (filename, file_content, 'application/octet-stream')}
                }
            ]

            # Update progress to 40%
            with self.uploadProgressLock:
                self.uploadProgress[upload_id] = 40

            for method in upload_methods:
                logger.info(f"Trying method: {method['name']}")

                post_files = method['files']

                try:
                    # Update progress to 60%
                    with self.uploadProgressLock:
                        self.uploadProgress[upload_id] = 60

                    response = requests.post(url, data=method['post_data'], files=post_files, timeout=120)

                    logger.info(f"Response status: {response.status_code}")
                    logger.info(f"Response headers: {response.headers}")

                    # Some printers return 200 with no JSON body for success
                    if response.status_code == 200:
                        # Try parsing JSON
                        try:
                            status = response.json()
                        except:
                            # No JSON body but 200 status = success
                            if response.status_code == 200:
                                logger.info(f"✓ Upload successful (HTTP 200, non-JSON response)")
                                with self.uploadProgressLock:
                                    self.uploadProgress[upload_id] = 100
                                logger.info(f"✓ Method '{method['name']}' worked! Saving for future uploads.")
                                return True
                    else:
                        status = {}

                    # Check for success
                    if status.get('success') or status.get('status') == 'success':
                        logger.info(f"✓ Upload successful!")
                        with self.uploadProgressLock:
                            self.uploadProgress[upload_id] = 100
                        logger.info(f"✓ Method '{method['name']}' worked! Saving for future uploads.")
                        return True
                    else:
                        logger.warning(f"Method failed: {status}")

                except Exception as e:
                    logger.error(f"Upload method failed: {e}")
                    continue

            # All methods failed
            logger.error("All USB upload methods failed. Please check:")
            logger.error("  1. Printer has a USB drive inserted")
            logger.error("  2. USB drive is formatted as FAT32 or exFAT")
            logger.error("  3. Printer firmware supports network uploads to USB")
            with self.uploadProgressLock:
                self.uploadProgress[upload_id] = 0
            return False

        # Local storage: chunked upload
        else:
            logger.info(f"Upload destination: Local storage")
            logger.info(f"Uploading file in {part_size} byte chunks...")

            with open(filepath, 'rb') as file:
                offset = 0
                while True:
                    chunk = file.read(part_size)
                    if not chunk:
                        break

                    # Update progress (thread-safe)
                    progress_value = min(int((offset / file_stats.st_size) * 90), 90)
                    with self.uploadProgressLock:
                        self.uploadProgress[upload_id] = progress_value

                    # Emit progress via WebSocket (bypasses SSE buffering issues)
                    self.socketio.emit('upload_progress', {
                        'upload_id': upload_id,
                        'progress': progress_value
                    }, namespace='/')

                    post_data['Offset'] = offset
                    post_files = {'file': (filename, chunk, 'application/octet-stream')}

                    try:
                        response = requests.post(url, data=post_data, files=post_files, timeout=60)
                        status = response.json()
                        if not (status.get('success') or status.get('status') == 'success'):
                            logger.error(f"Chunk upload failed at offset {offset}: {status}")
                            logger.error("Uploading file to printer failed.")
                            # Set progress to 0 to indicate failure
                            with self.uploadProgressLock:
                                self.uploadProgress[upload_id] = 0
                            return False
                    except Exception as e:
                        logger.error(f"Error during chunk upload: {e}")
                        return False

                    offset += len(chunk)

            # Set progress to 100% (thread-safe)
            with self.uploadProgressLock:
                self.uploadProgress[upload_id] = 100

            # Emit 100% via WebSocket
            self.socketio.emit('upload_progress', {
                'upload_id': upload_id,
                'progress': 100
            }, namespace='/')

            logger.info("✓ File uploaded successfully!")
            return True

    def _trigger_usb_gadget_refresh(self):
        """Trigger USB gadget to refresh/reconnect so printer detects new/changed files"""
        if not self.USE_USB_GADGET:
            logger.warning("USB gadget is not enabled, skipping refresh")
            return False

        try:
            # Method 1: Ensure all data is written to disk
            os.sync()
            logger.debug("Synced filesystem")

            # Method 2: Try to find and use configfs UDC paths
            configfs_gadget_dirs = []
            configfs_base = '/sys/kernel/config/usb_gadget'

            if os.path.exists(configfs_base):
                try:
                    configfs_gadget_dirs = [os.path.join(configfs_base, d) for d in os.listdir(configfs_base)]
                except:
                    pass

            # Add known UDC paths
            gadget_paths = []
            for gadget_dir in configfs_gadget_dirs:
                udc_path = os.path.join(gadget_dir, 'UDC')
                if os.path.exists(udc_path):
                    gadget_paths.append(udc_path)

            # Also try common hardcoded paths
            gadget_paths.extend([
                '/sys/kernel/config/usb_gadget/pi4/UDC',
                '/sys/kernel/config/usb_gadget/mass_storage/UDC',
                '/sys/kernel/config/usb_gadget/g1/UDC',
            ])

            for udc_path in gadget_paths:
                if os.path.exists(udc_path):
                    try:
                        # Read current UDC value
                        with open(udc_path, 'r') as f:
                            udc_value = f.read().strip()

                        if udc_value and udc_value != '' and udc_value != 'none':
                            # Disconnect and reconnect
                            logger.info(f"Attempting USB gadget reconnect via {udc_path}")

                            # Disconnect
                            with open(udc_path, 'w') as f:
                                f.write('')
                            time.sleep(0.5)

                            # Reconnect
                            with open(udc_path, 'w') as f:
                                f.write(udc_value)

                            logger.info("✓ USB gadget reconnected successfully")
                            return True
                    except PermissionError:
                        logger.warning(f"No permission to write to {udc_path}")
                        logger.info("💡 Run ChitUI as root: sudo python3 main.py")
                    except Exception as e:
                        logger.debug(f"Could not use {udc_path}: {e}")

            # Method 3: Try forced_eject for g_mass_storage
            forced_eject_path = '/sys/module/g_mass_storage/parameters/forced_eject'
            if os.path.exists(forced_eject_path):
                try:
                    logger.info("Using g_mass_storage forced_eject to trigger refresh...")
                    with open(forced_eject_path, 'w') as f:
                        f.write('1')
                    time.sleep(0.5)
                    with open(forced_eject_path, 'w') as f:
                        f.write('0')
                    logger.info("✓ USB gadget media change signaled")
                    return True
                except PermissionError:
                    logger.warning(f"No permission to write to {forced_eject_path}")
                    logger.info("💡 Run ChitUI as root: sudo python3 main.py")
                except Exception as e:
                    logger.debug(f"Could not use forced_eject: {e}")

            # No method worked
            logger.info("⚠ Could not trigger USB gadget reconnect - printer will need to poll for changes")
            logger.info("💡 Options:")
            logger.info("   1. Run ChitUI as root: sudo python3 main.py")
            logger.info("   2. Manually refresh on printer screen")
            logger.info("   3. Reconnect USB cable between Pi and printer")
            return False

        except Exception as e:
            logger.error(f"Error triggering USB gadget refresh: {e}")
            return False

    def _unmount_usb_gadget(self):
        """Unmount the USB gadget mount point"""
        try:
            logger.info("Unmounting USB gadget...")
            result = subprocess.run(['umount', '/mnt/usb_share'],
                                  capture_output=True, text=True, timeout=5)
            if result.returncode == 0:
                logger.info("USB gadget unmounted successfully")
                return True
            else:
                logger.warning(f"Unmount returned non-zero: {result.stderr}")
                return True  # Continue anyway
        except subprocess.TimeoutExpired:
            logger.error("USB gadget unmount timed out")
            return False
        except Exception as e:
            logger.error(f"Error unmounting USB gadget: {e}")
            return False

    def _mount_usb_gadget(self):
        """Mount the USB gadget mount point with read-write permissions"""
        try:
            logger.info("Mounting USB gadget...")

            # Check if already mounted and if it's read-only
            try:
                check_result = subprocess.run(['mountpoint', '-q', '/mnt/usb_share'],
                                            capture_output=True)
                if check_result.returncode == 0:
                    # Already mounted - check if read-only
                    mount_info = subprocess.run(['mount'], capture_output=True, text=True)
                    for line in mount_info.stdout.split('\n'):
                        if '/mnt/usb_share' in line:
                            if 'ro,' in line or '(ro)' in line:
                                logger.warning("USB gadget is mounted read-only, remounting as read-write...")
                                # Remount as read-write
                                result = subprocess.run(['mount', '-o', 'remount,rw', '/mnt/usb_share'],
                                                      capture_output=True, text=True, timeout=5)
                                if result.returncode == 0:
                                    logger.info("✓ USB gadget remounted as read-write")
                                    return True
                                else:
                                    logger.error(f"Failed to remount as rw: {result.stderr}")
                                    # Try unmounting and mounting fresh
                                    logger.info("Trying full unmount/mount cycle...")
                                    self._unmount_usb_gadget()
                                    time.sleep(0.5)
                            else:
                                logger.info("USB gadget already mounted as read-write")
                                return True
            except Exception as e:
                logger.debug(f"Mountpoint check error (continuing): {e}")

            # Try mounting from fstab first
            result = subprocess.run(['mount', '/mnt/usb_share'],
                                  capture_output=True, text=True, timeout=5)
            if result.returncode == 0:
                logger.info("USB gadget mounted successfully from fstab")
                return True

            # If fstab mount failed, try manual mount with explicit options
            logger.info("Fstab mount failed, trying manual mount with rw options...")
            result = subprocess.run([
                'mount', '-t', 'vfat', '-o',
                'loop,rw,umask=000,uid=1000,gid=1000',
                '/piusb.bin', '/mnt/usb_share'
            ], capture_output=True, text=True, timeout=5)

            if result.returncode == 0:
                logger.info("USB gadget mounted successfully with manual mount")
                return True
            else:
                logger.warning(f"Mount failed: {result.stderr}")
                return False
        except subprocess.TimeoutExpired:
            logger.error("USB gadget mount timed out")
            return False
        except Exception as e:
            logger.error(f"Error mounting USB gadget: {e}")
            return False

    def _reload_usb_gadget(self):
        """Reload the USB gadget to reflect file changes on the printer"""
        try:
            logger.info("Reloading USB gadget to notify printer...")

            # Use the reload script - it works when run manually so call it from Python
            script_path = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'scripts', 'reload_usb_gadget.sh')
            if os.path.exists(script_path):
                logger.info(f"Running reload script: {script_path}")
                result = subprocess.run(['sudo', 'bash', script_path],
                                      capture_output=True, text=True, timeout=30)

                # Log script output
                if result.stdout:
                    for line in result.stdout.strip().split('\n'):
                        logger.info(f"  SCRIPT: {line}")
                if result.stderr:
                    for line in result.stderr.strip().split('\n'):
                        logger.warning(f"  SCRIPT ERR: {line}")

                if result.returncode == 0:
                    logger.info("✓ USB gadget reload complete!")
                    return True
                else:
                    logger.error(f"✗ Reload script failed (exit code {result.returncode})")
                    return False

            logger.error(f"Reload script not found at {script_path}")
            return False

        except subprocess.TimeoutExpired:
            logger.error("USB gadget reload timed out")
            return False
        except Exception as e:
            logger.error(f"Error reloading USB gadget: {e}")
            return False

    def _delete_file_from_mount(self, file_path):
        """Delete a file directly from the USB gadget mount point"""
        try:
            # Convert printer path to mount point path
            # e.g., /usb/file.goo -> /mnt/usb_share/file.goo
            if file_path.startswith('/usb/'):
                mount_path = file_path.replace('/usb/', '/mnt/usb_share/', 1)
            else:
                logger.error(f"Invalid file path for mount delete: {file_path}")
                return False

            # Check if file exists
            if not os.path.exists(mount_path):
                logger.warning(f"File not found at mount point: {mount_path}")
                return False

            # Delete the file
            os.remove(mount_path)
            logger.info(f"Deleted file from mount point: {mount_path}")

            # Reload the USB gadget so the printer sees the change
            logger.info(">>> Starting USB gadget reload after delete...")
            reload_result = self._reload_usb_gadget()
            if reload_result:
                logger.info("✓ USB gadget reload completed successfully")
            else:
                logger.warning("⚠ USB gadget reload returned False - printer may not detect change")

            return True
        except Exception as e:
            logger.error(f"Error deleting file from mount point: {e}")
            return False


# Plugin entry point
def setup(plugin_dir):
    return FileManagerPlugin(plugin_dir)
