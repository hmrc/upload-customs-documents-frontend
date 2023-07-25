import toggleElementVisually from '../../javascripts/utils/toggle-element.util';

describe('toggleElementVisually utility', () => {
  let element;

  afterEach(() => {
    element.remove();
  });

  describe('Given an element is visible', () => {
    beforeEach(() => {
      document.body.insertAdjacentHTML('afterbegin', `<div class="test-container"></div>`);

      element = document.querySelector('.test-container');
    });

    describe('When toggleElementVisually is called with state="true"', () => {
      beforeEach(() => {
        toggleElementVisually(element, true);
      });

      it('Then should not visually hide element', () => {
        expect(element.classList.contains('hidden')).toEqual(false);
      });
    });

    describe('When toggleElementVisually is called with state="false"', () => {
      beforeEach(() => {
        toggleElementVisually(element, false);
      });

      it('Then should visually hide element', () => {
        expect(element.classList.contains('hidden')).toEqual(true);
      });
    });
  });

  describe('Given an element is visually hidden', () => {
    beforeEach(() => {
      document.body.insertAdjacentHTML('afterbegin', `<div class="test-container hidden"></div>`);

      element = document.querySelector('.test-container');
    });

    describe('When toggleElementVisually is called with state="true"', () => {
      beforeEach(() => {
        toggleElementVisually(element, true);
      });

      it('Then should show element', () => {
        expect(element.classList.contains('hidden')).toEqual(false);
      });
    });

    describe('When toggleElementVisually is called with state="false"', () => {
      beforeEach(() => {
        toggleElementVisually(element, false);
      });

      it('Then should not show element', () => {
        expect(element.classList.contains('hidden')).toEqual(true);
      });
    });
  });
});
