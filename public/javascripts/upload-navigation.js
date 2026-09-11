// Keeps the upload page out of the browser's session history.
//
// Without this, uploads and removals push a new page to the navigation history.
(function () {
  "use strict";

  function replaceNavigation(url) {
    window.location.replace(url);
  }

  function initRemoveLinks() {
    var links = document.querySelectorAll("a[data-replace-nav]");
    for (var i = 0; i < links.length; i++) {
      links[i].addEventListener("click", function (event) {
        event.preventDefault();
        replaceNavigation(this.href);
      });
    }
  }

  function initUploadForm() {
    var form = document.querySelector("form[data-upload-form]");
    if (!form) return;

    var key = form.getAttribute("data-upload-key");
    var postedUrl = form.getAttribute("data-file-posted-url");
    var maximumBytes = parseInt(form.getAttribute("data-maximum-file-size-bytes"), 10);
    if (!key || !postedUrl) return;

    form.addEventListener("submit", function (event) {
      var input = form.querySelector('input[type="file"]');
      var file = input && input.files && input.files[0];

      if (!file || file.size < 1) return;
      if (maximumBytes && file.size > maximumBytes) return;

      event.preventDefault();
      setBusy(form, true);

      window
        .fetch(form.action, {
          method: "POST",
          body: new FormData(form),
          mode: "cors",
          redirect: "manual"
        })
        .then(function (response) {
          if (response.type === "opaqueredirect") {
            replaceNavigation(postedUrl + "?key=" + encodeURIComponent(key));
          } else {
            fallbackToNativeSubmit(form);
          }
        })
        .catch(function () {
          fallbackToNativeSubmit(form);
        });
    });
  }

  function fallbackToNativeSubmit(form) {
    setBusy(form, false);
    form.submit();
  }

  function setBusy(form, busy) {
    var button = form.querySelector("button");
    if (button) button.disabled = busy;
    form.setAttribute("aria-busy", busy ? "true" : "false");
  }

  function init() {
    initRemoveLinks();
    if (window.fetch && window.FormData) initUploadForm();
  }

  if (document.readyState !== "loading") {
    init();
  } else {
    document.addEventListener("DOMContentLoaded", init);
  }
})();
