import GOVUKFrontend from 'webjars/lib/govuk-frontend/govuk/all.js';
import HMRCFrontend from 'webjars/lib/hmrc-frontend/hmrc/all.js';

export default function

  init(): void {
  GOVUKFrontend.initAll();
  HMRCFrontend.initAll();

  Array
    .from(document.querySelectorAll('button[data-disable-after-click="true"]'))
    .forEach(element => {
      element.addEventListener('click', function (event: Event) {
        const target = event.target as HTMLInputElement;
        window.setTimeout((target: HTMLInputElement) => {
          target.setAttribute('disabled', '');
        }, 10, target);
        return true;
      });
    });

  const backLinkAnchor = document.querySelector('.govuk-back-link');

  if (backLinkAnchor && backLinkAnchor.getAttribute("href") == "#") {
    backLinkAnchor.addEventListener('click', function (event) {
      event.preventDefault();
      history.back();
    });
  }

}
