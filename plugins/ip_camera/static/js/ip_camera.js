/**
 * IP Camera Plugin - Client-side JavaScript
 */

const IPCameraPlugin = {
    cameras: [],
    activeCameras: new Set(),
    configModal: null,
    editModal: null,
    fullscreenModal: null,

    init: function() {
        console.log('IP Camera Plugin initialized');

        // Initialize Bootstrap modals
        this.configModal = new bootstrap.Modal(document.getElementById('ipCameraConfigModal'));
        this.editModal = new bootstrap.Modal(document.getElementById('ipCameraEditModal'));
        this.fullscreenModal = new bootstrap.Modal(document.getElementById('ipCameraFullscreenModal'));

        // Set up form handlers
        document.getElementById('addCameraForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.addCamera();
        });

        // Load cameras on init
        this.loadCameras();

        // Refresh camera list every 5 seconds
        setInterval(() => {
            if (this.cameras.length > 0) {
                this.loadCameras();
            }
        }, 5000);
    },

    loadCameras: function() {
        fetch('/plugin/ip_camera/cameras')
            .then(response => response.json())
            .then(data => {
                if (data.ok) {
                    this.cameras = data.cameras;
                    this.renderCameras();
                    this.renderConfigList();
                }
            })
            .catch(error => {
                console.error('Error loading cameras:', error);
            });
    },

    renderCameras: function() {
        const grid = document.getElementById('ipCameraGrid');

        if (this.cameras.length === 0) {
            grid.innerHTML = `
                <div class="col-12 text-center text-muted py-5">
                    <i class="bi bi-camera-video-off" style="font-size: 3rem;"></i>
                    <p class="mt-3">No cameras configured</p>
                    <button class="btn btn-primary" onclick="IPCameraPlugin.showConfigModal()">
                        <i class="bi bi-plus-circle"></i> Add Camera
                    </button>
                </div>
            `;
            return;
        }

        let html = '';
        this.cameras.forEach(camera => {
            const isActive = camera.active;
            const statusClass = isActive ? 'text-success' : 'text-muted';
            const statusIcon = isActive ? 'bi-record-circle-fill' : 'bi-circle';
            const btnText = isActive ? 'Stop' : 'Start';
            const btnClass = isActive ? 'btn-danger' : 'btn-success';
            const btnIcon = isActive ? 'bi-stop-circle' : 'bi-play-circle';

            html += `
                <div class="col-12 col-md-6 col-xl-4 mb-3">
                    <div class="card ip-camera-card">
                        <div class="card-header d-flex justify-content-between align-items-center">
                            <h6 class="mb-0">
                                <i class="bi ${statusIcon} ${statusClass}"></i>
                                ${camera.name}
                            </h6>
                            <button class="btn ${btnClass} btn-sm" onclick="IPCameraPlugin.toggleCamera('${camera.id}', ${isActive})">
                                <i class="bi ${btnIcon}"></i> ${btnText}
                            </button>
                        </div>
                        <div class="card-body p-0 position-relative camera-viewport">
                            ${isActive ? `
                                <img src="/plugin/ip_camera/camera/${camera.id}/video"
                                     alt="${camera.name}"
                                     class="camera-stream"
                                     onclick="IPCameraPlugin.showFullscreen('${camera.id}', '${camera.name}')">
                                <div class="camera-overlay">
                                    <button class="btn btn-sm btn-light" onclick="IPCameraPlugin.showFullscreen('${camera.id}', '${camera.name}')">
                                        <i class="bi bi-arrows-fullscreen"></i>
                                    </button>
                                </div>
                            ` : `
                                <div class="camera-placeholder">
                                    <i class="bi bi-camera-video-off"></i>
                                    <p>Camera Stopped</p>
                                </div>
                            `}
                        </div>
                        <div class="card-footer">
                            <small class="text-muted">
                                <i class="bi bi-link-45deg"></i> ${camera.protocol.toUpperCase()}
                            </small>
                        </div>
                    </div>
                </div>
            `;
        });

        grid.innerHTML = html;
    },

    renderConfigList: function() {
        const list = document.getElementById('ipCameraConfigList');

        if (this.cameras.length === 0) {
            list.innerHTML = '<p class="text-muted text-center">No cameras configured</p>';
            return;
        }

        let html = '';
        this.cameras.forEach((camera, index) => {
            html += `
                <div class="card mb-2">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start">
                            <div>
                                <h6 class="mb-1">${camera.name}</h6>
                                <small class="text-muted">
                                    <i class="bi bi-link-45deg"></i> ${camera.url}<br>
                                    <i class="bi bi-gear"></i> ${camera.protocol.toUpperCase()}
                                </small>
                            </div>
                            <div>
                                <button class="btn btn-sm btn-outline-primary me-1"
                                        onclick="IPCameraPlugin.editCamera(${index})"
                                        title="Edit">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                <button class="btn btn-sm btn-outline-danger"
                                        onclick="IPCameraPlugin.deleteCamera(${index})"
                                        title="Delete">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });

        list.innerHTML = html;
    },

    toggleCamera: function(cameraId, isActive) {
        if (isActive) {
            this.stopCamera(cameraId);
        } else {
            this.startCamera(cameraId);
        }
    },

    startCamera: function(cameraId) {
        fetch(`/plugin/ip_camera/camera/${cameraId}/start`, {
            method: 'POST'
        })
        .then(response => response.json())
        .then(data => {
            if (data.ok) {
                this.showMessage('success', data.msg || 'Camera started');
                this.activeCameras.add(cameraId);
                setTimeout(() => this.loadCameras(), 1000);
            } else {
                this.showMessage('error', data.msg || 'Failed to start camera');
            }
        })
        .catch(error => {
            console.error('Error starting camera:', error);
            this.showMessage('error', 'Error starting camera: ' + error.message);
        });
    },

    stopCamera: function(cameraId) {
        fetch(`/plugin/ip_camera/camera/${cameraId}/stop`, {
            method: 'POST'
        })
        .then(response => response.json())
        .then(data => {
            if (data.ok) {
                this.showMessage('success', data.msg || 'Camera stopped');
                this.activeCameras.delete(cameraId);
                this.loadCameras();
            } else {
                this.showMessage('error', data.msg || 'Failed to stop camera');
            }
        })
        .catch(error => {
            console.error('Error stopping camera:', error);
            this.showMessage('error', 'Error stopping camera: ' + error.message);
        });
    },

    addCamera: function() {
        const name = document.getElementById('cameraName').value;
        const protocol = document.getElementById('cameraProtocol').value;
        const url = document.getElementById('cameraUrl').value;

        fetch('/plugin/ip_camera/config', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                action: 'add',
                name: name,
                protocol: protocol,
                url: url
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.ok) {
                this.showMessage('success', 'Camera added successfully');
                document.getElementById('addCameraForm').reset();
                this.loadCameras();
            } else {
                this.showMessage('error', data.msg || 'Failed to add camera');
            }
        })
        .catch(error => {
            console.error('Error adding camera:', error);
            this.showMessage('error', 'Error adding camera: ' + error.message);
        });
    },

    editCamera: function(index) {
        const camera = this.cameras[index];
        if (!camera) return;

        document.getElementById('editCameraIndex').value = index;
        document.getElementById('editCameraName').value = camera.name;
        document.getElementById('editCameraProtocol').value = camera.protocol;
        document.getElementById('editCameraUrl').value = camera.url;

        this.configModal.hide();
        this.editModal.show();
    },

    saveEdit: function() {
        const index = parseInt(document.getElementById('editCameraIndex').value);
        const name = document.getElementById('editCameraName').value;
        const protocol = document.getElementById('editCameraProtocol').value;
        const url = document.getElementById('editCameraUrl').value;

        fetch('/plugin/ip_camera/config', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                action: 'update',
                index: index,
                name: name,
                protocol: protocol,
                url: url
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.ok) {
                this.showMessage('success', 'Camera updated successfully');
                this.editModal.hide();
                this.loadCameras();
            } else {
                this.showMessage('error', data.msg || 'Failed to update camera');
            }
        })
        .catch(error => {
            console.error('Error updating camera:', error);
            this.showMessage('error', 'Error updating camera: ' + error.message);
        });
    },

    deleteCamera: function(index) {
        if (!confirm('Are you sure you want to delete this camera?')) {
            return;
        }

        fetch('/plugin/ip_camera/config', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                action: 'delete',
                index: index
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.ok) {
                this.showMessage('success', 'Camera deleted successfully');
                this.loadCameras();
            } else {
                this.showMessage('error', data.msg || 'Failed to delete camera');
            }
        })
        .catch(error => {
            console.error('Error deleting camera:', error);
            this.showMessage('error', 'Error deleting camera: ' + error.message);
        });
    },

    showFullscreen: function(cameraId, cameraName) {
        document.getElementById('fullscreenCameraTitle').textContent = cameraName;
        document.getElementById('fullscreenCameraImage').src = `/plugin/ip_camera/camera/${cameraId}/video`;
        this.fullscreenModal.show();
    },

    showConfigModal: function() {
        this.configModal.show();
    },

    showMessage: function(type, message) {
        // Create a toast notification
        const toast = document.createElement('div');
        toast.className = `alert alert-${type === 'success' ? 'success' : 'danger'} alert-dismissible fade show position-fixed`;
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        toast.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(toast);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            toast.remove();
        }, 5000);
    }
};

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    // Wait a bit for the plugin to be loaded
    setTimeout(() => {
        IPCameraPlugin.init();
    }, 500);
});
