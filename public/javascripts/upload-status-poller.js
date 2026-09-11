// Check file status and transition them from "Uploading" to "Uploaded" tag.
// Polls every 2s for 30s (15 attempts), then every 10s indefinitely.
// Once any status changes, the poller will refresh the whole page.
// This matches the no-JS meta-refresh fallback (every 3s, never gives up).
(function () {
  "use strict";

  var FAST_ATTEMPTS = 15;
  var FAST_INTERVAL_MS = 2000;
  var SLOW_INTERVAL_MS = 10000;

  // Non-terminal statuses from FileVerificationStatus.fileStatus: keep polling on these.
  var PENDING = { WAITING: true, NOT_UPLOADED: true };

  function pollRow(row) {
    var url = row.getAttribute("data-status-url");
    if (!url) return;
    var attempts = 0;

    function tick() {
      attempts++;
      fetch(url, { headers: { Accept: "application/json" } })
        .then(function (response) {
          return response.ok ? response.json() : null;
        })
        .then(function (status) {
          if (status && status.fileStatus && !PENDING[status.fileStatus]) {
            window.location.reload();
          } else {
            retry();
          }
        })
        .catch(retry);
    }

    function retry() {
      window.setTimeout(tick, attempts < FAST_ATTEMPTS ? FAST_INTERVAL_MS : SLOW_INTERVAL_MS);
    }

    tick();
  }

  function init() {
    if (!window.fetch) return;
    var rows = document.querySelectorAll("[data-upload-status-row]");
    for (var i = 0; i < rows.length; i++) {
      if (rows[i].querySelector(".govuk-tag--yellow")) {
        pollRow(rows[i]);
      }
    }
  }

  if (document.readyState !== "loading") {
    init();
  } else {
    document.addEventListener("DOMContentLoaded", init);
  }
})();
