const visuallyHiddenClass = 'govuk-visually-hidden';

export default function toggleElementVisually(element: HTMLElement, state: boolean): void {
  element.classList.toggle(visuallyHiddenClass, !state);
}
