// Check file status and transition them from "Uploading" to "Uploaded" tag.
// Every INTERVAL_MS the poller will try to get the status of files tagged in yellow (uploading).
// Once any status changes, the poller will refresh the whole page.
(function () {
  "use strict";

  var MAX_ATTEMPTS = 15;
  var INTERVAL_MS = 2000;

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
      if (attempts < MAX_ATTEMPTS) window.setTimeout(tick, INTERVAL_MS);
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
