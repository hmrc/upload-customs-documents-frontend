import parseHtml from '../utils/parse-html.util';
import { ErrorList } from '../interfaces/error-list.interface';
import { KeyValue } from '../interfaces/key-value.interface';

export default class ErrorManager {
  private classes: KeyValue;
  private errorSummaryTpl: string;
  private errorSummaryItemTpl: string;
  private errorMessageTpl: string;
  private errorSummary: HTMLElement;
  private errorSummaryList: HTMLUListElement;
  private errors: ErrorList = {};
  private metaTitle: string;
  private metaTitleErrorPrefix: string;

  constructor() {
    this.classes = {
      inputContainer: 'govuk-form-group',
      inputContainerError: 'govuk-form-group--error',
      errorSummaryList: 'govuk-error-summary__list',
      label: 'govuk-label'
    };

    this.cacheTemplates();
    this.cacheElements();
    this.cacheMetaTitle();
  }

  private static getH1() {
    return document.querySelector('h1');
  }

  private cacheTemplates(): void {
    this.errorSummaryTpl = document.getElementById('error-manager-summary-tpl').textContent;
    this.errorSummaryItemTpl = document.getElementById('error-manager-summary-item-tpl').textContent;
    this.errorMessageTpl = document.getElementById('error-manager-message-tpl').textContent;
  }

  private cacheElements(): void {
    this.errorSummary = parseHtml(this.errorSummaryTpl, {});
    this.errorSummaryList = this.errorSummary.querySelector(`.${this.classes.errorSummaryList}`);
  }

  private cacheMetaTitle(): void {
    this.metaTitle = document.title;
    this.metaTitleErrorPrefix = this.errorSummary.dataset.errorPrefix;
  }

  public addError(inputId: string, message: string): void {
    this.removeError(inputId);

    const errorMessage = this.addErrorToField(inputId, message);
    const errorSummaryRow = this.addErrorToSummary(inputId, message);

    this.errors[inputId] = {
      errorMessage: errorMessage,
      errorSummaryRow: errorSummaryRow
    };

    this.updateErrorSummaryVisibility();
    this.updateMetaTitle();
  }

  public removeError(inputId: string): void {
    if (!Object.prototype.hasOwnProperty.call(this.errors, inputId)) {
      return;
    }

    const error = this.errors[inputId];
    const input = document.getElementById(inputId);
    const inputContainer = this.getContainer(input);

    error.errorMessage.remove();
    error.errorSummaryRow.remove();

    inputContainer.classList.remove(this.classes.inputContainerError);

    delete this.errors[inputId];

    this.updateErrorSummaryVisibility();
  }

  public hasSingleError(inputId: string): boolean {
    return Object.entries(this.errors).length === 1 && this.hasError(inputId);
  }

  public hasError(inputId: string): boolean {
    return Object.prototype.hasOwnProperty.call(this.errors, inputId);
  }

  public hasErrors(): boolean {
    return Object.entries(this.errors).length > 0;
  }

  private addErrorToField(inputId: string, message: string): HTMLElement {
    const errorMessage = parseHtml(this.errorMessageTpl, {
      errorMessage: message
    });

    const input = document.getElementById(inputId);

    if (input) {
      const inputContainer = this.getContainer(input);
      const label = this.getLabel(inputContainer);

      inputContainer.classList.add(this.classes.inputContainerError);

      label.after(errorMessage);
    }

    return errorMessage;
  }

  private addErrorToSummary(inputId: string, message: string): HTMLElement {
    const summaryRow = parseHtml(this.errorSummaryItemTpl, {
      inputId: inputId,
      errorMessage: message
    });

    document.getElementById(inputId).setAttribute('aria-describedBy', 'multi-file-upload-error');

    this.bindErrorEvents(summaryRow, inputId);
    this.errorSummaryList.append(summaryRow);

    return summaryRow;
  }

  private bindErrorEvents(errorItem: HTMLElement, inputId: string): void {
    errorItem.querySelector('a').addEventListener('click', (e) => {
      e.preventDefault();

      if (inputId == 'initial') {
        document.getElementById('choice').focus();
      } else {
        document.getElementById(inputId).focus();
      }

    });
  }

  private updateErrorSummaryVisibility(): void {
    if (this.hasErrors()) {
      ErrorManager.getH1().before(this.errorSummary);
    }
    else {
      this.errorSummary.remove();
    }
  }

  private updateMetaTitle(): void {
    if (this.hasErrors()) {
      document.title = `${this.metaTitleErrorPrefix} ${this.metaTitle}`;
    }
    else {
      document.title = `${this.metaTitle}`;
    }
  }

  public focusSummary(): void {
    this.errorSummary.focus();
  }

  private getContainer(input: HTMLElement): HTMLElement {
    return input.closest(`.${this.classes.inputContainer}`) as HTMLElement;
  }

  private getLabel(container: HTMLElement): HTMLLabelElement {
    return container.querySelector(`.${this.classes.label}`);
  }
}
