/**
 * File Manager with Thumbnails Plugin - Frontend Logic
 *
 * Handles file upload, thumbnail display, file list management, storage monitoring, and file operations
 */

(function() {
  'use strict';

  // ============ THUMBNAIL LIGHTBOX ============

  window.openLightbox = function(imageSrc, needsRotation) {
    const lightbox = document.getElementById('thumbnailLightbox');
    const lightboxImg = document.getElementById('lightboxImage');

    if (lightbox && lightboxImg) {
      lightboxImg.src = imageSrc;

      if (needsRotation) {
        lightboxImg.classList.add('auto-rotate');
      } else {
        lightboxImg.classList.remove('auto-rotate');
      }

      lightbox.style.display = 'flex';
    }
  };

  window.closeLightbox = function() {
    const lightbox = document.getElementById('thumbnailLightbox');
    if (lightbox) {
      lightbox.style.display = 'none';
    }
  };

  // Close lightbox on ESC key
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
      window.closeLightbox();
    }
  });

  // ============ FILE UPLOAD ============

  $('#btnUpload').on('click', function () {
    uploadFile();
  });

  // Upload destination radio button handlers
  $('input[name="destination"]').on('change', function() {
    const destination = $(this).val();
    $('#uploadDestPath').val(destination);
    console.log('Upload destination changed to:', destination);
  });

  function uploadFile() {
    // Generate a unique upload ID for progress tracking
    var uploadId = generateUUID();

    // Get the form data and add the upload ID
    var formData = new FormData($('#formUpload')[0]);
    formData.append('upload_id', uploadId);

    // Show and reset progress bar
    $('.progress-enhanced').show();
    $('#progressUpload').text('0%').css('width', '0%');

    // Start progress tracking BEFORE starting upload to avoid race condition
    var progressEventSource = null;
    var progressStarted = false;

    var req = $.ajax({
      url: '/plugin/file_manager_thumbs/upload',
      type: 'POST',
      data: formData,
      cache: false,
      contentType: false,
      processData: false,
      xhr: function () {
        var myXhr = $.ajaxSettings.xhr();
        if (myXhr.upload) {
          myXhr.upload.addEventListener('progress', function (e) {
            if (e.lengthComputable) {
              var percent = Math.floor(e.loaded / e.total * 100);
              $('#progressUpload').text('Upload to ChitUI: ' + percent + '%').css('width', percent + '%');

              // Start server-to-printer progress tracking when client upload reaches 50%
              if (percent >= 50 && !progressStarted) {
                progressStarted = true;
                progressEventSource = fileTransferProgress(uploadId);
              }
            }
          }, false);
        }
        return myXhr;
      }
    });

    req.done(function (data) {
      $('#uploadFile').val('');

      // Show success toast with appropriate message
      var toastMsg = '✓ File uploaded successfully!';
      if (data.usb_gadget) {
        if (data.refresh_triggered) {
          toastMsg = '✓ File saved to USB gadget. Checking printer...';
        } else {
          toastMsg = '⚠ File saved. You may need to reconnect USB or refresh on printer.';
        }
      }

      $("#toastUploadText").text(toastMsg);
      $("#toastUpload").show();
      setTimeout(function () {
        $("#toastUpload").hide();
      }, 5000);

      // Handle file list refresh based on upload type
      if (data.usb_gadget) {
        // USB gadget upload - use retry logic since printer may need time to detect
        console.log('USB gadget upload detected, starting retry logic...');
        refreshFileListWithRetry(data.filename, 0);
      } else {
        // Network upload - single refresh is usually sufficient
        setTimeout(function() {
          if (window.currentPrinter) {
            console.log('Refreshing file list after network upload...');
            refreshFileList();
          }
        }, 1000);
      }
    });

    req.fail(function (xhr, status, error) {
      // Close progress EventSource if it was started
      if (progressEventSource) {
        progressEventSource.close();
      }

      // Reset progress bar
      $('#progressUpload').text('0%').css('width', '0%')
        .removeClass('progress-bar-striped progress-bar-animated text-bg-warning');

      // Better error handling - check if responseJSON exists
      var errorMsg = 'Upload failed';
      if (xhr.responseJSON && xhr.responseJSON.msg) {
        errorMsg = xhr.responseJSON.msg;
      } else if (xhr.responseText) {
        try {
          var response = JSON.parse(xhr.responseText);
          errorMsg = response.msg || errorMsg;
        } catch (e) {
          errorMsg = xhr.responseText || errorMsg;
        }
      } else if (error) {
        errorMsg = error;
      }

      alert(errorMsg);
    });

    req.always(function () {
      // Cleanup if needed
    });
  }

  // Helper function to generate UUID
  function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  function fileTransferProgress(uploadId) {
    $('#progressUpload').addClass('progress-bar-striped progress-bar-animated');

    // Use WebSocket for progress updates
    var progressHandler = function(data) {
      // Only handle events for this specific upload
      if (data.upload_id !== uploadId) return;

      var progressValue = parseInt(data.progress);
      console.log('Upload progress:', progressValue + '%');

      if (progressValue > 0) {
        $('#progressUpload').text('Upload to printer: ' + progressValue + '%').css('width', progressValue + '%').addClass('text-bg-warning');
      }
      if (progressValue >= 100) {
        setTimeout(function () {
          $('#progressUpload').text('0%').css('width', '0%');
          setTimeout(function () {
            $('#progressUpload').removeClass('progress-bar-striped progress-bar-animated text-bg-warning');
          }, 1000);
        }, 1000);
        // Remove the listener after completion
        socket.off('upload_progress', progressHandler);
      }
    };

    // Listen for progress events
    socket.on('upload_progress', progressHandler);

    // Return an object with a close method for compatibility
    return {
      close: function() {
        socket.off('upload_progress', progressHandler);
      },
      handler: progressHandler
    };
  }

  // ============ FILE LIST REFRESH HELPERS ============

  function refreshFileList() {
    /**
     * Simple file list refresh - clears cache and requests fresh lists
     */
    if (!window.currentPrinter) return;

    // Clear the cached files list
    if (window.printers[window.currentPrinter]) {
      window.printers[window.currentPrinter]['files'] = [];
    }

    // Request fresh file list from both USB and local
    if (typeof window.getPrinterFiles === 'function') {
      window.getPrinterFiles(window.currentPrinter, '/usb');
      window.getPrinterFiles(window.currentPrinter, '/local');
    }
  }

  function refreshFileListWithRetry(uploadedFilename, attemptNumber) {
    /**
     * Retry file list refresh for USB gadget uploads
     * The printer may need time to detect the new file
     */
    var maxAttempts = 5;
    var delays = [2000, 3000, 5000, 7000, 10000]; // Progressive delays in milliseconds

    if (attemptNumber >= maxAttempts) {
      console.log('Max retry attempts reached for file list refresh');
      $("#toastUploadText").text('⚠ File saved but not detected yet. Try refreshing the printer screen or reconnecting USB.');
      $("#toastUpload").show();
      return;
    }

    console.log(`Retry attempt ${attemptNumber + 1}/${maxAttempts} - waiting ${delays[attemptNumber]}ms`);

    // Wait before checking
    setTimeout(function() {
      // Request fresh file list
      refreshFileList();

      // Check if file appeared (after a short delay to allow response)
      setTimeout(function() {
        var fileFound = false;
        if (window.currentPrinter && window.printers[window.currentPrinter] && window.printers[window.currentPrinter]['files']) {
          var files = window.printers[window.currentPrinter]['files'];
          fileFound = files.some(function(file) {
            return file.Filename && file.Filename.includes(uploadedFilename);
          });
        }

        if (fileFound) {
          console.log('File detected on printer!');
          $("#toastUploadText").text('✓ File uploaded and detected by printer!');
          $("#toastUpload").show();
          setTimeout(function () {
            $("#toastUpload").hide();
          }, 3000);
        } else {
          console.log(`File not found yet, scheduling next retry...`);
          refreshFileListWithRetry(uploadedFilename, attemptNumber + 1);
        }
      }, 1000);
    }, delays[attemptNumber]);
  }

  // ============ STORAGE DISPLAY ============

  function updateStorageDisplay(remainingBytes) {
    // RemainingMemory is in bytes, convert to GB
    const remainingGB = remainingBytes / (1024 * 1024 * 1024);

    // Try to determine total storage from remaining
    // Most printers have 16GB or 32GB internal storage
    let totalGB = 32;
    if (remainingGB > 32) {
      totalGB = 64;
    } else if (remainingGB < 16) {
      totalGB = 16;
    }

    const usedGB = totalGB - remainingGB;
    const usedPercent = (usedGB / totalGB * 100).toFixed(1);

    // Format sizes nicely
    function formatSize(gb) {
      if (gb < 1) {
        return (gb * 1024).toFixed(0) + ' MB';
      }
      return gb.toFixed(2) + ' GB';
    }

    // Update circular gauge (file manager)
    const $gaugeCircle = $('#storageGaugeCircle');
    const $gaugePercent = $('#storageGaugePercent');
    const circumference = 2 * Math.PI * 80; // 2 * PI * radius (80)
    const offset = circumference - (usedPercent / 100 * circumference);

    $gaugeCircle.css('stroke-dashoffset', offset);
    $gaugePercent.text(Math.round(usedPercent) + '%');

    // Color code the circular gauge
    $gaugeCircle.removeClass('warning danger');
    if (usedPercent < 70) {
      // Green (default)
    } else if (usedPercent < 90) {
      $gaugeCircle.addClass('warning');
    } else {
      $gaugeCircle.addClass('danger');
    }

    // Update simple display text
    $('#storageUsedSimple').text(formatSize(usedGB));
    $('#storageFreeSimple').text(formatSize(remainingGB));
    $('#storageInfo').show();
  }

  function updateUsbGadgetStorage() {
    fetch('/plugin/file_manager_thumbs/usb-gadget/storage')
      .then(response => response.json())
      .then(data => {
        if (data.success && data.available) {
          // Show the gauge
          $('#usbGaugeWrapper').show();

          // Format size function
          function formatSize(bytes) {
            const mb = bytes / (1024 * 1024);
            const gb = bytes / (1024 * 1024 * 1024);
            if (gb >= 1) {
              return gb.toFixed(2) + ' GB';
            }
            return mb.toFixed(0) + ' MB';
          }

          // Update gauge
          const usedPercent = data.percent;
          const $gaugeCircle = $('#usbGaugeCircle');
          const $gaugePercent = $('#usbGaugePercent');
          const circumference = 2 * Math.PI * 80; // 2 * PI * radius (80)
          const offset = circumference - (usedPercent / 100 * circumference);

          $gaugeCircle.css('stroke-dashoffset', offset);
          $gaugePercent.text(Math.round(usedPercent) + '%');

          // Color code the USB gauge
          $gaugeCircle.removeClass('warning danger');
          if (usedPercent < 70) {
            // Green (default)
          } else if (usedPercent < 90) {
            $gaugeCircle.addClass('warning');
          } else {
            $gaugeCircle.addClass('danger');
          }

          // Update details text
          $('#usbGaugeDetails').text(formatSize(data.used) + ' / ' + formatSize(data.total));
        } else {
          // Hide the gauge if USB gadget is not available
          $('#usbGaugeWrapper').hide();
        }
      })
      .catch(error => {
        console.error('Error fetching USB gadget storage:', error);
        $('#usbGaugeWrapper').hide();
      });
  }

  // Update USB gadget storage every 5 seconds
  setInterval(updateUsbGadgetStorage, 5000);
  // Also update immediately on page load
  $(document).ready(function() {
    updateUsbGadgetStorage();
  });

  // ============ FILE MANAGER UI SYNC ============

  // Monitor for files being added to the Files tab
  function syncFilesToFileManager() {
    console.log('Initializing file sync with thumbnails...');

    const observer = new MutationObserver(() => {
      const fileTable = document.getElementById('tableFiles');
      if (fileTable && fileTable.children.length > 0) {
        syncTableToFileManager(fileTable);
      }
      hideFilesTab();
    });

    observer.observe(document.body, {
      childList: true,
      subtree: true
    });

    setInterval(() => {
      const fileTable = document.getElementById('tableFiles');
      if (fileTable && fileTable.children.length > 0) {
        syncTableToFileManager(fileTable);
      }
      hideFilesTab();
    }, 2000);
  }

  function hideFilesTab() {
    const filesTab = document.getElementById('tab-Files');
    if (filesTab && filesTab.parentElement) {
      filesTab.parentElement.style.visibility = 'hidden';
      filesTab.parentElement.style.position = 'absolute';
      filesTab.parentElement.style.pointerEvents = 'none';
    }
  }

  let lastFileCount = 0;

  function syncTableToFileManager(sourceTbody) {
    const fileManagerBody = document.getElementById('fileManagerBody');
    if (!fileManagerBody) {
      console.warn('fileManagerBody not found!');
      return;
    }
    if (!sourceTbody) {
      console.warn('sourceTbody not provided!');
      return;
    }

    const rows = sourceTbody.querySelectorAll('tr');
    console.log(`syncTableToFileManager: Found ${rows.length} rows in source table`);

    if (rows.length === lastFileCount) {
      console.log('File count unchanged, skipping sync');
      return;
    }

    lastFileCount = rows.length;
    console.log(`Syncing ${rows.length} files to File Manager with thumbnails`);

    fileManagerBody.innerHTML = '';

    rows.forEach((row, index) => {
      const newRow = document.createElement('tr');
      newRow.classList.add('file-row');

      const cells = row.querySelectorAll('td');

      if (cells.length >= 2) {
        // Extract filename from the field-value content
        const fieldValueContent = cells[1].innerHTML;
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = fieldValueContent;
        const fullPath = tempDiv.childNodes[0]?.textContent || '';

        // Extract just the filename without path (e.g., "/usb/file.goo" -> "file.goo")
        const filename = fullPath.split('/').pop();

        // Determine file location from path
        let location = 'Unknown';
        if (fullPath.startsWith('/usb/')) {
          location = 'USB';
        } else if (fullPath.startsWith('/local/')) {
          location = 'Local';
        }

        // Try to extract file size from the original row (if available)
        let fileSize = '--';
        const sizeMatch = fieldValueContent.match(/(\d+(?:\.\d+)?)\s*(MB|KB|GB)/i);
        if (sizeMatch) {
          fileSize = sizeMatch[1] + ' ' + sizeMatch[2];
        }

        // Extract file extension and base name (needed for status badge)
        const fileExt = filename.toLowerCase().split('.').pop();
        const baseName = filename.substring(0, filename.lastIndexOf('.')) || filename;

        // Create thumbnail cell
        const thumbnailCell = document.createElement('td');
        thumbnailCell.classList.add('thumbnail-cell');

        // Check if thumbnail exists for .goo or .ctb files
        if (fileExt === 'goo' || fileExt === 'ctb') {
          const thumbnailUrl = `/plugin/file_manager_thumbs/thumbnails/${baseName}_big.png`;
          console.log(`Loading thumbnail for ${filename}: ${thumbnailUrl}`);

          // Create thumbnail image
          const img = document.createElement('img');
          img.src = thumbnailUrl;
          img.classList.add('file-thumbnail');
          img.alt = 'Preview';
          img.setAttribute('loading', 'lazy');

          // Auto-rotation detection
          img.onload = function() {
            console.log(`${baseName}: ${this.naturalWidth}x${this.naturalHeight}`);
            if (this.naturalWidth > this.naturalHeight) {
              console.log(`ROTATING ${baseName}`);
              this.classList.add('auto-rotate');
              this.dataset.needsRotation = 'true';
            } else {
              console.log(`NO ROTATION for ${baseName}`);
            }

            // Update status badge to "Ready"
            const statusBadge = document.getElementById(`status-${baseName}`);
            if (statusBadge) {
              statusBadge.className = 'badge bg-success';
              statusBadge.innerHTML = '<i class="bi bi-check-circle me-1"></i>Ready';
            }
          };

          // Handle thumbnail load error
          img.onerror = function() {
            console.log(`Failed to load thumbnail: ${thumbnailUrl}`);
            // Replace with placeholder if thumbnail doesn't exist
            const placeholder = document.createElement('div');
            placeholder.classList.add('thumbnail-placeholder');
            placeholder.innerHTML = '<i class="bi bi-file-earmark"></i>';
            thumbnailCell.replaceChild(placeholder, this);

            // Update status badge to "No Thumbnail"
            const statusBadge = document.getElementById(`status-${baseName}`);
            if (statusBadge) {
              statusBadge.className = 'badge bg-warning';
              statusBadge.innerHTML = '<i class="bi bi-exclamation-triangle me-1"></i>No Thumbnail';
            }
          };

          // Click to open lightbox
          img.onclick = function() {
            const needsRotation = this.dataset.needsRotation === 'true';
            window.openLightbox(thumbnailUrl, needsRotation);
          };

          thumbnailCell.appendChild(img);
        } else {
          // Non-goo/ctb files - show icon placeholder
          const placeholder = document.createElement('div');
          placeholder.classList.add('thumbnail-placeholder');
          placeholder.innerHTML = '<i class="bi bi-file-earmark"></i>';
          thumbnailCell.appendChild(placeholder);
        }

        newRow.appendChild(thumbnailCell);

        // Filename cell
        const filenameCell = document.createElement('td');
        filenameCell.textContent = filename;
        filenameCell.style.fontFamily = 'monospace';
        newRow.appendChild(filenameCell);

        // Size cell
        const sizeCell = document.createElement('td');
        sizeCell.textContent = fileSize;
        sizeCell.classList.add('text-muted');
        newRow.appendChild(sizeCell);

        // Location cell
        const locationCell = document.createElement('td');
        const locationBadge = document.createElement('span');
        locationBadge.classList.add('badge');
        if (location === 'USB') {
          locationBadge.classList.add('bg-primary');
          locationBadge.innerHTML = '<i class="bi bi-usb-drive me-1"></i>USB';
        } else if (location === 'Local') {
          locationBadge.classList.add('bg-secondary');
          locationBadge.innerHTML = '<i class="bi bi-device-ssd me-1"></i>Local';
        } else {
          locationBadge.classList.add('bg-secondary');
          locationBadge.textContent = location;
        }
        locationCell.appendChild(locationBadge);
        newRow.appendChild(locationCell);

        // Status cell
        const statusCell = document.createElement('td');
        const statusBadge = document.createElement('span');
        statusBadge.classList.add('badge');

        // Check if this file type should have a thumbnail
        const shouldHaveThumbnail = fileExt === 'goo' || fileExt === 'ctb';

        if (!shouldHaveThumbnail) {
          statusBadge.classList.add('bg-secondary');
          statusBadge.innerHTML = '<i class="bi bi-dash-circle me-1"></i>N/A';
        } else {
          // We'll update this when the thumbnail loads/fails
          statusBadge.classList.add('bg-warning');
          statusBadge.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Loading';
          statusBadge.id = `status-${baseName}`;
        }
        statusCell.appendChild(statusBadge);
        newRow.appendChild(statusCell);

        // Actions cell
        const actionsCell = document.createElement('td');
        const icons = tempDiv.querySelectorAll('i');
        icons.forEach(icon => {
          const clonedIcon = icon.cloneNode(true);
          const iconAction = icon.getAttribute('data-action');

          // Re-attach click handler to cloned icon
          clonedIcon.addEventListener('click', function(e) {
            var action = this.getAttribute('data-action');
            var file = this.getAttribute('data-file');
            if (action && file) {
              // Show confirmation modal for both delete and print actions
              $('#modalConfirmTitle').text('Confirm ' + action);
              $('#modalConfirmAction').text(action);
              $('#modalConfirmValue').text(file);
              $('#btnConfirm').data('action', action).data('value', file);
              if (window.modalConfirm) {
                window.modalConfirm.show();
              }
            }
          });
          actionsCell.appendChild(clonedIcon);
        });
        actionsCell.style.textAlign = 'right';
        newRow.appendChild(actionsCell);

        const dataFile = row.getAttribute('data-file');
        if (dataFile) {
          newRow.setAttribute('data-file', dataFile);
        }

        fileManagerBody.appendChild(newRow);
      }
    });

    console.log('File Manager updated with', fileManagerBody.children.length, 'files');
  }

  setTimeout(syncFilesToFileManager, 1000);

  // ============ FILE SEARCH ============

  const fileSearchInput = document.getElementById('fileSearchInput');
  if (fileSearchInput) {
    fileSearchInput.addEventListener('input', (e) => {
      const searchTerm = e.target.value.toLowerCase();
      const fileRows = document.querySelectorAll('#fileManagerBody tr');

      fileRows.forEach(row => {
        if (row.cells.length === 1) return;
        // Search in filename column (index 1)
        const fileName = row.cells[1]?.textContent.toLowerCase() || '';
        row.style.display = fileName.includes(searchTerm) ? '' : 'none';
      });
    });
  }

  // ============ FILE OPERATIONS ============

  // Refresh files button
  const btnRefreshFiles = document.getElementById('btnRefreshFiles');
  if (btnRefreshFiles) {
    btnRefreshFiles.addEventListener('click', () => {
      console.log('Refreshing files...');
      refreshFileList();
    });
  }

  // Refresh PI USB button - manually trigger USB gadget reload
  const btnRefreshPI = document.getElementById('btnRefreshPI');
  if (btnRefreshPI) {
    btnRefreshPI.addEventListener('click', () => {
      console.log('Refreshing PI USB gadget...');

      // Disable button and show loading state
      const originalHTML = btnRefreshPI.innerHTML;
      btnRefreshPI.disabled = true;
      btnRefreshPI.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Refreshing...';

      // Refresh file list after a delay
      setTimeout(() => {
        refreshFileList();

        // Reset button
        btnRefreshPI.disabled = false;
        btnRefreshPI.innerHTML = originalHTML;

        console.log('PI USB refresh triggered');
      }, 2000);
    });
  }

  // Scan and extract thumbnails button
  const btnScanThumbnails = document.getElementById('btnScanThumbnails');
  if (btnScanThumbnails) {
    btnScanThumbnails.addEventListener('click', () => {
      console.log('Scanning for thumbnails...');

      // Disable button and show loading state
      const originalHTML = btnScanThumbnails.innerHTML;
      btnScanThumbnails.disabled = true;
      btnScanThumbnails.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Extracting...';

      // Collect file list from the file manager table
      const fileList = [];
      const fileRows = document.querySelectorAll('#fileManagerBody tr[data-file]');

      fileRows.forEach(row => {
        const dataFile = row.getAttribute('data-file');
        if (dataFile) {
          // Extract filename from the path (e.g., /usb/file.goo -> file.goo)
          const filename = dataFile.split('/').pop();

          // Only include .goo and .ctb files
          if (filename.toLowerCase().endsWith('.goo') || filename.toLowerCase().endsWith('.ctb')) {
            fileList.push({
              filename: filename,
              path: dataFile
            });
          }
        }
      });

      // Get current printer ID from the printer selector
      const printerSelect = document.getElementById('uploadPrinter');
      const printerId = printerSelect ? printerSelect.value : null;

      console.log(`Found ${fileList.length} files to process, printer: ${printerId}`);

      // Call scan endpoint with file list and printer info
      fetch('/plugin/file_manager_thumbs/scan-thumbnails', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          files: fileList,
          printer_id: printerId
        })
      })
        .then(response => response.json())
        .then(data => {
          console.log('Thumbnail scan results:', data);

          if (data.success) {
            let msg = `Thumbnails extracted!\n\n`;
            msg += `Total files: ${data.total}\n`;
            msg += `Extracted: ${data.extracted}\n`;
            msg += `Failed: ${data.failed}\n\n`;
            msg += `Cache directory: ${data.cache_dir || 'Unknown'}\n`;
            msg += `USB Gadget mode: ${data.usb_gadget_mode ? 'Enabled' : 'Disabled'}`;

            alert(msg);

            // Refresh file list to show new thumbnails
            setTimeout(() => {
              refreshFileList();
            }, 1000);
          } else {
            alert(`Error: ${data.message}`);
          }
        })
        .catch(error => {
          console.error('Error scanning thumbnails:', error);
          alert('Failed to scan thumbnails. Check console for details.');
        })
        .finally(() => {
          // Reset button
          btnScanThumbnails.disabled = false;
          btnScanThumbnails.innerHTML = originalHTML;
        });
    });
  }

  // ============ DRAG & DROP FILE UPLOAD ============

  const uploadArea = document.querySelector('.upload-area');
  const fileInput = document.getElementById('uploadFile');
  const uploadButton = document.getElementById('btnUpload');

  if (uploadArea && fileInput) {
    uploadArea.addEventListener('click', (e) => {
      if (!e.target.closest('button')) {
        fileInput.click();
      }
    });

    uploadArea.addEventListener('dragover', (e) => {
      e.preventDefault();
      uploadArea.classList.add('drag-over');
    });

    uploadArea.addEventListener('dragleave', () => {
      uploadArea.classList.remove('drag-over');
    });

    uploadArea.addEventListener('drop', (e) => {
      e.preventDefault();
      uploadArea.classList.remove('drag-over');
      if (e.dataTransfer.files.length) {
        fileInput.files = e.dataTransfer.files;
        handleFileSelect();
      }
    });

    fileInput.addEventListener('change', handleFileSelect);
  }

  function handleFileSelect() {
    if (fileInput.files.length > 0) {
      const fileName = fileInput.files[0].name;
      uploadArea.querySelector('h5').textContent = fileName;
      if (uploadButton) uploadButton.style.display = 'block';

      // Show destination selector
      const uploadDestination = document.getElementById('uploadDestination');
      if (uploadDestination) {
        uploadDestination.style.display = 'block';

        // Check if USB gadget is available
        fetch('/status')
          .then(response => response.json())
          .then(data => {
            const usbOption = document.getElementById('destUsb');
            const usbLabel = usbOption?.parentElement;

            if (!data.usb_gadget.enabled && usbLabel) {
              // Hide USB option if gadget is disabled
              usbLabel.style.display = 'none';
            } else if (usbLabel) {
              usbLabel.style.display = 'block';
            }
          })
          .catch(err => {
            console.error('Error checking USB gadget status:', err);
          });
      }

      // Set printer ID in hidden field
      if (window.currentPrinter) {
        document.getElementById('uploadPrinter').value = window.currentPrinter;
      }
    }
  }

  // Export functions to global scope for compatibility
  window.updateStorageDisplay = updateStorageDisplay;
  window.updateUsbGadgetStorage = updateUsbGadgetStorage;
  window.refreshFileList = refreshFileList;
  window.refreshFileListWithRetry = refreshFileListWithRetry;

})();
