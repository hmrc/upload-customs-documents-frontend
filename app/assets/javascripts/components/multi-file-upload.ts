import { Component } from './component';
import { KeyValue } from '../interfaces/key-value.interface';
import { UploadState } from '../enums/upload-state.enum';
import parseTemplate from '../utils/parse-template.util';
import parseHtml from '../utils/parse-html.util';
import toggleElement from '../utils/toggle-element.util';
import toggleElementVisually from '../utils/toggle-element-visually.util';
import ErrorManager from '../tools/error-manager.tool';

export class MultiFileUpload extends Component {
  private config;
  private uploadData = {};
  private messages: KeyValue;
  private classes: KeyValue;
  private formStatus: HTMLElement;
  private submitBtn: HTMLInputElement;
  private addAnotherBtn: HTMLButtonElement;
  private uploadMoreMessage: HTMLElement;
  private notifications: HTMLElement;
  private itemTpl: string;
  private inputTpl: string;
  private itemList: HTMLUListElement;
  private inputList: HTMLUListElement;
  private dropZone: HTMLElement;
  private lastFileIndex = 0;
  private readonly errorManager;
  private draggedFiles: {[key: string]: File} = {};

  constructor(form: HTMLFormElement) {
    super(form);

    this.config = {
      startRows: parseInt(form.dataset.multiFileUploadStartRows) || 1,
      minFiles: parseInt(form.dataset.multiFileUploadMinFiles),
      maxFiles: parseInt(form.dataset.multiFileUploadMaxFiles) || 100,
      maxFileSize: parseInt(form.dataset.multiFileUploadMaxFileSize),
      uploadedFiles: form.dataset.multiFileUploadUploadedFiles ? JSON.parse(form.dataset.multiFileUploadUploadedFiles) : [],
      retryDelayMs: parseInt(form.dataset.multiFileUploadRetryDelayMs, 10) || 1000,
      maxRetries: parseInt(form.dataset.multiFileUploadMaxRetries) || 30,
      actionUrl: form.action,
      sendUrlTpl: decodeURIComponent(form.dataset.multiFileUploadSendUrlTpl),
      statusUrlTpl: decodeURIComponent(form.dataset.multiFileUploadStatusUrlTpl),
      removeUrlTpl: decodeURIComponent(form.dataset.multiFileUploadRemoveUrlTpl),
      showAddAnotherDocumentButton: form.dataset.multiFileUploadShowAddAnotherDocumentButton !== undefined
    };

    this.messages = {
      noFilesUploadedError: form.dataset.multiFileUploadErrorSelectFile,
      genericError: form.dataset.multiFileUploadErrorGeneric,
      couldNotRemoveFile: form.dataset.multiFileUploadErrorRemoveFile,
      stillTransferring: form.dataset.multiFileUploadStillTransferring,
      documentUploaded: form.dataset.multiFileUploadDocumentUploaded,
      documentDeleted: form.dataset.multiFileUploadDocumentDeleted,
      invalidSizeLargeError: form.dataset.multiFileUploadErrorInvalidSizeLarge,
      invalidSizeSmallError: form.dataset.multiFileUploadErrorInvalidSizeSmall,
      invalidTypeError: form.dataset.multiFileUploadErrorInvalidType,
      chooseFirstFileLabel: form.dataset.multiFileUploadChooseFirstFileLabel,
      chooseNextFileLabel: form.dataset.multiFileUploadChooseNextFileLabel,
      newFileDescription: form.dataset.multiFileUploadNewFileDescription,
      initialError: form.dataset.multiFileUploadInitialError
    };

    this.classes = {
      inputList: 'multi-file-upload__input-list',
      itemList: 'multi-file-upload__item-list',
      item: 'multi-file-upload__item',
      itemContent: 'multi-file-upload__item-content',
      input: 'multi-file-upload__input',
      inputLabel: 'multi-file-upload__input-label',
      waiting: 'multi-file-upload__item--waiting',
      uploading: 'multi-file-upload__item--uploading',
      verifying: 'multi-file-upload__item--verifying',
      uploaded: 'multi-file-upload__item--uploaded',
      removing: 'multi-file-upload__item--removing',
      file: 'multi-file-upload__file',
      fileName: 'multi-file-upload__file-name',
      filePreview: 'multi-file-upload__file-preview',
      remove: 'multi-file-upload__remove-item',
      addAnother: 'multi-file-upload__add-another',
      formStatus: 'multi-file-upload__form-status',
      submit: 'upload-documents-submit',
      fileNumber: 'multi-file-upload__number',
      progressBar: 'multi-file-upload__progress-bar',
      uploadMore: 'multi-file-upload__upload-more-message',
      notifications: 'multi-file-upload__notifications',
      description: 'multi-file-upload__description'
    };

    this.errorManager = new ErrorManager();

    if (this.messages.initialError) {
      this.errorManager.addError("initial", this.messages.initialError, "");
    }

    this.cacheElements();
    this.cacheTemplates();
    this.bindEvents();
  }

  private cacheElements(): void {
    this.itemList = this.container.querySelector(`.${this.classes.itemList}`);
    this.inputList = this.container.querySelector(`.${this.classes.inputList}`);
    this.addAnotherBtn = this.container.querySelector(`.${this.classes.addAnother}`);
    this.uploadMoreMessage = this.container.querySelector(`.${this.classes.uploadMore}`);
    this.formStatus = this.container.querySelector(`.${this.classes.formStatus}`);
    this.submitBtn = this.container.querySelector(`#${this.classes.submit}`);
    this.notifications = this.container.querySelector(`.${this.classes.notifications}`);
    this.dropZone = this.container.closest('.govuk-template__body');
  }

  private cacheTemplates(): void {
    this.itemTpl = document.getElementById('multi-file-upload-item-tpl').textContent;
    this.inputTpl = document.getElementById('multi-file-upload-input-tpl').textContent;
  }

  private bindEvents(): void {
    this.container.addEventListener('submit', this.handleSubmit.bind(this));
    this.addAnotherBtn.addEventListener('click', this.handleAddInput.bind(this));
    this.dropZone.addEventListener('drop', this.handleFileDrop.bind(this));
    this.dropZone.addEventListener('dragover', this.handleFileDragOver.bind(this));
    this.dropZone.addEventListener('dragenter', this.handleDragEnter.bind(this));
    this.dropZone.addEventListener('dragleave', this.handleDragLeave.bind(this));
  }

  private bindItemEvents(item: HTMLElement): void {
    this.getRemoveButtonFromItem(item)?.addEventListener('click', this.handleRemoveItem.bind(this));
  }

  private bindInputEvents(item: HTMLElement): void {
    this.getFileInputFromItem(item)?.addEventListener('change', this.handleFileInputEvent.bind(this));
  }

  public init(): void {
    this.removeAllItems();
    this.createInitialRows();
    this.updateButtonVisibility();
  }

  private createInitialRows(): void {
    let rowCount = 0;

    this.config.uploadedFiles.filter(file => file['fileStatus'] === 'ACCEPTED').forEach(fileData => {
      this.createUploadedItem(fileData);

      rowCount++;
    });

    const startRows = Math.min(this.config.startRows, this.config.maxFiles);

    if (rowCount < this.config.maxFiles) {
      this.addUploadInput();
    }
  }

  private createUploadedItem(fileData: unknown): HTMLElement | undefined {
    const item = this.addItemWithFileInput();
    if(item){
      const fileInput = this.getFileInputFromItem(item);
      const fileName = this.extractFileName(fileData['fileName']);
      const filePreview = this.getFilePreviewElement(item);

      this.setItemState(item, UploadState.Uploaded);
      this.getFileNameElements(item).forEach((elem) => {elem.textContent = fileName});
      this.getDescriptionElement(item).textContent = fileData['description'];

      filePreview.textContent = fileName;
      filePreview.href = fileData['previewUrl'];

      fileInput.dataset.multiFileUploadFileRef = fileData['reference'];

      return item;
    } else {
      return undefined;
    }
  }

  private createWaitingItem(file: File): HTMLElement | undefined {
    const item = this.addItemWithFileInput();
    if(item){
      const fileName = file.name;
      const fileInput = this.getFileInputFromItem(item);

      this.setItemState(item, UploadState.Waiting);
      this.getFileNameElements(item).forEach((elem) => {elem.textContent = fileName});
      this.getDescriptionElement(item).textContent = this.messages.newFileDescription;

      this.draggedFiles[fileInput.id] = file;

      this.updateButtonVisibility();
      this.updateFormStatusVisibility();

      return item;
    } else {
      return undefined;
    }
  }

  private handleSubmit(e: Event): void {

    this.updateFormStatusVisibility(this.isBusy());

    // if (this.errorManager.hasErrors()
    //   && !this.errorManager.hasSingleError("initial")) {
    //   this.errorManager.focusSummary();
    //   e.preventDefault();
    //   return;
    // }

    if (this.isInProgress()) {
      this.addNotification(this.messages.stillTransferring);
      e.preventDefault();
      return;
    }

    const uploadedFiles = this.container.querySelectorAll(`.${this.classes.uploaded}`);
    const radioInputNo = document.getElementById('choice-2') as HTMLInputElement;
    if (radioInputNo.checked && uploadedFiles.length < this.config.minFiles) {
      const firstFileInput = this.inputList.querySelector(`.${this.classes.file}`);
      this.errorManager.addError(firstFileInput.id, this.messages.noFilesUploadedError,undefined);
      this.errorManager.focusSummary();
      e.preventDefault();
    }
  }

  private handleAddInput(): void {
    const input = this.addUploadInput();
    const file = this.getFileInputFromItem(input);

    file.focus();
  }

  private addItem(): HTMLElement | undefined {
     if(this.getItems().length < this.config.maxFiles){
      const fileNumber = this.getItems().length + 1;
      const itemParams = {
        fileNumber: fileNumber.toString()
      }
      const item = parseHtml(this.itemTpl, itemParams) as HTMLElement;

      this.bindItemEvents(item);
      this.itemList.prepend(item);
      this.getDescriptionElement(item).textContent = this.messages.newFileDescription;
      this.updateButtonVisibility();

      return item;
    } else {
      return undefined;
    }
  }

  private addItemWithFileInput(): HTMLElement | undefined {
    if(this.getItems().length < this.config.maxFiles){
      const fileNumber = this.getItems().length + 1;
      const fileIndex = ++this.lastFileIndex;
      const itemParams = {
        fileNumber: fileNumber.toString(),
        fileIndex: fileIndex.toString()
      }
      const item = parseHtml(this.itemTpl, itemParams) as HTMLElement;
      const input = parseHtml(this.inputTpl, itemParams) as HTMLElement;
      const fileInput = this.getFileInputFromItem(input);
      const label = this.getInputLabelElement(input);
      input.parentElement.removeChild(input);
      fileInput.parentElement.removeChild(fileInput);
      label.parentElement.removeChild(label);
      toggleElement(label, false);
      const itemContent = this.getItemContentElement(item);
      item.insertBefore(label, itemContent);
      label.after(fileInput);

      this.bindItemEvents(item);
      this.itemList.prepend(item);
      this.getDescriptionElement(item).textContent = this.messages.newFileDescription;
      this.updateButtonVisibility();

      return item;
    } else {
      return undefined;
    }
  }

  private addUploadInput(): HTMLElement {
    if(this.getInputs().length===0 && this.getItems().length < this.config.maxFiles){
      const fileNumber = this.getItems().length + 1;
      const fileIndex = ++this.lastFileIndex;
      const inputParams = {
        fileNumber: fileNumber.toString(),
        fileIndex: fileIndex.toString(),
        fileDescription: ''
      }
      const isFirstFileOfItsKind = fileNumber === 1 || !this.hasAlreadyFileWithDescription(this.messages.newFileDescription);
      if (isFirstFileOfItsKind && this.messages.chooseFirstFileLabel) {
        inputParams.fileDescription = this.messages.chooseFirstFileLabel;
      } else if (!isFirstFileOfItsKind && this.messages.chooseNextFileLabel) {
        inputParams.fileDescription = this.messages.chooseNextFileLabel;
      }
      const input = parseHtml(this.inputTpl, inputParams) as HTMLElement;
      this.bindInputEvents(input);
      this.inputList.append(input);
      const file  = this.getFileInputFromItem(input);
      file.focus();
      return input;
    } else {
      return this.getInputs()[0];
    }
  }

  private addUploadInputWithFileAndLabel(oldFileInput: HTMLInputElement, oldLabel: HTMLElement): HTMLElement {
    if(this.getInputs().length===0 && this.getItems().length < this.config.maxFiles){
      const fileNumber = this.getItems().length + 1;
      const fileIndex = ++this.lastFileIndex;
      const inputParams = {
        fileNumber: fileNumber.toString(),
        fileIndex: fileIndex.toString(),
        fileDescription: ''
      }
      const input = parseHtml(this.inputTpl, inputParams) as HTMLElement;
      const fileInput = this.getFileInputFromItem(input);
      const label = this.getInputLabelElement(input);
      fileInput.parentElement.removeChild(fileInput);
      label.parentElement.removeChild(label);
      input.appendChild(oldLabel);
      oldLabel.after(oldFileInput);
      const isFirstFileOfItsKind = fileNumber === 1 || !this.hasAlreadyFileWithDescription(this.messages.newFileDescription);
      if (isFirstFileOfItsKind && this.messages.chooseFirstFileLabel) {
        oldLabel.textContent = this.messages.chooseFirstFileLabel;
      } else if (!isFirstFileOfItsKind && this.messages.chooseNextFileLabel) {
        oldLabel.textContent = this.messages.chooseNextFileLabel;
      }
      this.bindInputEvents(input);
      this.inputList.append(input);
      return input;
    } else {
      return this.getInputs()[0];
    }
  }

  private updateInputLabel(fileInput: HTMLInputElement) {
    const label = this.getInputLabelElement(fileInput.parentElement);
    const description = this.messages.newFileDescription;
    if (!label || !description) return false;
    else { 
      const isFirstFileOfItsKind = !this.hasAlreadyFileWithDescription(description);
      if (isFirstFileOfItsKind && this.messages.chooseFirstFileLabel) {
        label.textContent = this.messages.chooseFirstFileLabel;
      } else if (!isFirstFileOfItsKind && this.messages.chooseNextFileLabel) {
        label.textContent = this.messages.chooseNextFileLabel;
      }
    }
  }


  private hasAlreadyFileWithDescription(description: String): Boolean {
    if (!description) return false;
    else { 
      const result = this.getItems().find((item) => {
        const descriptionElem = this.getDescriptionElement(item);
        return descriptionElem.textContent === description;
      });
      return result != undefined;
    }
  }

  private handleRemoveItem(e: Event): void {
    const target = e.target as HTMLElement;
    const item = target.closest(`.${this.classes.item}`) as HTMLElement;
    const file = this.getFileInputFromItem(item);
    const ref = file.dataset.multiFileUploadFileRef;

    if (this.isUploading(item)) {
      if (this.uploadData[file.id].uploadHandle) {
        this.uploadData[file.id].uploadHandle.abort();
      }
    }

    if (ref) {
      this.setItemState(item, UploadState.Removing);
      this.requestRemoveFile(file);
    }
    else {
      this.removeItem(item);
    }
    
  }

  private requestRemoveFile(fileInput: HTMLInputElement) {
    const item = this.getItemFromFile(fileInput);

    fetch(this.getRemoveUrl(fileInput.dataset.multiFileUploadFileRef), {
      method: 'POST'
    })
      .then(this.requestRemoveFileCompleted.bind(this, fileInput))
      .catch(() => {
        this.setItemState(item, UploadState.Uploaded);
        this.errorManager.addError(fileInput.id, this.messages.couldNotRemoveFile, this.getFileName(fileInput));
      });
  }

  private requestRemoveFileCompleted(fileInput: HTMLInputElement) {
    const item = fileInput.closest(`.${this.classes.item}`) as HTMLElement;
    const message = parseTemplate(this.messages.documentDeleted, {
      fileName: this.getFileName(fileInput)
    });
    this.addNotification(message);
    this.removeItem(item);
  }

  private removeItem(item: HTMLElement): void {
    const file = this.getFileInputFromItem(item);

    this.errorManager.removeAllErrors();
    item.remove();
    this.updateFileNumbers();
    this.updateButtonVisibility();
    this.updateFormStatusVisibility();
    if (this.getInputs().length === 0) { this.addUploadInput(); }

    delete this.uploadData[file.id];
    this.updateInputLabel(this.getFileInputFromItem(this.getInputs()[0]));
  }

  private provisionUpload(fileInput: HTMLInputElement): void {
    const item = this.getItemFromFile(fileInput);

    if (Object.prototype.hasOwnProperty.call(this.uploadData, fileInput.id)) {
      this.prepareFileUpload(fileInput);

      return;
    }

    this.uploadData[fileInput.id] = {};
    this.uploadData[fileInput.id].provisionPromise = this.requestProvisionUpload(fileInput);

    this.uploadData[fileInput.id].provisionPromise.then(() => {
      if (item.parentNode !== null && !this.isRemoving(item)) {
        this.prepareFileUpload(fileInput);
      }
    });
  }

  private requestProvisionUpload(fileInput: HTMLInputElement) {
    return fetch(this.getSendUrl(fileInput.id), {
      method: 'POST'
    })
      .then(response => response.json())
      .then(this.handleProvisionUploadCompleted.bind(this, fileInput))
      .catch(this.delayedProvisionUpload.bind(this, fileInput));
  }

  private delayedProvisionUpload(file: string): void {
    window.setTimeout(this.provisionUpload.bind(this, file), this.config.retryDelayMs);
  }

  private handleProvisionUploadCompleted(fileInput: HTMLInputElement, response: unknown): void {
    const fileRef = response['upscanReference'];

    fileInput.dataset.multiFileUploadFileRef = fileRef;

    this.uploadData[fileInput.id].reference = fileRef;
    this.uploadData[fileInput.id].fields = response['uploadRequest']['fields'];
    this.uploadData[fileInput.id].url = response['uploadRequest']['href'];
    this.uploadData[fileInput.id].retries = 0;
  }

  private handleFileInputEvent(e: Event): void {
    const fileInput = e.target as HTMLInputElement;
    this.handleFileInputChange(fileInput);
  }

  private handleFileInputChange(fileInput: HTMLInputElement): void {  

    this.errorManager.removeAllErrors();

    if (!fileInput.files.length) {
      return;
    }

    const file: File = fileInput.files[0];

    if (this.config.maxFileSize && file.size && file.size > this.config.maxFileSize) {
      this.errorManager.addError(fileInput.id, this.messages.invalidSizeLargeError, this.getFileName(fileInput));
      return;
    }

    if (file.size === 0) {
      this.errorManager.addError(fileInput.id, this.messages.invalidSizeSmallError, this.getFileName(fileInput));
      return;
    }

    const input = this.getInputFromFile(fileInput);
    const label = this.getInputLabelElement(input);
    const item = this.addItem();
    input.parentElement.removeChild(input);
    fileInput.parentElement.removeChild(fileInput);
    label.parentElement.removeChild(label);
    toggleElement(label, false);
    const itemContent = this.getItemContentElement(item);
    item.insertBefore(label, itemContent);
    label.after(fileInput);
    
    const fileName = this.extractFileName(fileInput.value)
    this.getFileNameElements(item).forEach((elem) => {elem.textContent = fileName});
    this.setItemState(item, UploadState.Waiting);
    this.updateButtonVisibility();
    this.uploadNext();
  }

  private uploadNext(): void {
    const nextItem = this.itemList.querySelector(`.${this.classes.waiting}`) as HTMLElement;

    if (!nextItem || this.isBusy()) {
      if (!this.config.showAddAnotherDocumentButton && !this.hasEmptyOrErrorItem() && this.getItems().length < this.config.maxFiles) {
        this.handleAddInput();
      }
      return;
    }

    const fileInput = this.getFileInputFromItem(nextItem);

    this.setItemState(nextItem, UploadState.Uploading);
    this.provisionUpload(fileInput);
  }

  private prepareFileUpload(fileInput: HTMLInputElement): void {
    const item = this.getItemFromFile(fileInput);
    const fileName = this.getFileName(fileInput);

    this.updateButtonVisibility();
    this.errorManager.removeError(fileInput.id);

    this.getFileNameElements(item).forEach((elem) => {elem.textContent = fileName});
    this.getFilePreviewElement(item).textContent = fileName;

    this.uploadData[fileInput.id].uploadHandle = this.uploadFile(fileInput);
  }

  private prepareFormData(fileInput: HTMLInputElement, data): FormData {
    const formData = new FormData();

    for (const [key, value] of Object.entries(data.fields)) {
      formData.append(key, value as string);
    }

    if(fileInput.files.length>0){
      formData.append('file', fileInput.files[0]);
    } else if(this.draggedFiles[fileInput.id]) {
      formData.append('file', this.draggedFiles[fileInput.id]);
    } else {
      console.error(`Missing file data for input ${fileInput.id}`)
    }

    return formData;
  }

  private uploadFile(fileInput: HTMLInputElement): XMLHttpRequest {
    const xhr = new XMLHttpRequest();
    const fileRef = fileInput.dataset.multiFileUploadFileRef;
    const data = this.uploadData[fileInput.id];
    const formData = this.prepareFormData(fileInput, data);
    const item = this.getItemFromFile(fileInput);

    xhr.upload.addEventListener('progress', this.handleUploadFileProgress.bind(this, item));
    xhr.addEventListener('load', this.handleUploadFileCompleted.bind(this, fileRef));
    xhr.addEventListener('error', this.handleUploadFileError.bind(this, fileRef));
    xhr.open('POST', data.url);
    xhr.send(formData);

    return xhr;
  }

  private handleUploadFileProgress(item: HTMLElement, e: ProgressEvent): void {
    if (e.lengthComputable) {
      this.updateUploadProgress(item, e.loaded / e.total * 95);
    }
  }

  private handleUploadFileCompleted(fileRef: string): void {
    const file = this.getFileByReference(fileRef);
    const item = this.getItemFromFile(file);

    this.setItemState(item, UploadState.Verifying);
    this.delayedRequestUploadStatus(fileRef);
  }

  private handleUploadFileError(fileRef: string): void {
    const fileInput = this.getFileByReference(fileRef);
    const item = this.getItemFromFile(fileInput);

    this.setItemState(item, UploadState.Default);
    this.errorManager.addError(fileInput.id, this.messages.genericError, this.getFileName(fileInput));
  }

  private requestUploadStatus(fileRef: string): void {
    const file = this.getFileByReference(fileRef);

    if (!file || !Object.prototype.hasOwnProperty.call(this.uploadData, file.id)) {
      return;
    }

    fetch(this.getStatusUrl(fileRef), {
      method: 'GET'
    })
      .then(response => response.json())
      .then(this.handleRequestUploadStatusCompleted.bind(this, fileRef))
      .catch(e => {
        console.error(e);
        this.delayedRequestUploadStatus.bind(this, fileRef)
      });
  }

  private delayedRequestUploadStatus(fileRef: string): void {
    window.setTimeout(this.requestUploadStatus.bind(this, fileRef), this.config.retryDelayMs);
  }

  private handleRequestUploadStatusCompleted(fileRef: string, response: unknown): void {
    const fileInput = this.getFileByReference(fileRef);
    const data = this.uploadData[fileInput.id];
    const error = response['errorMessage'] || this.messages.genericError;

    switch (response['fileStatus']) {
      case 'ACCEPTED':
        this.handleFileStatusSuccessful(fileInput, response['previewUrl'], response['description']);
        this.uploadNext();
        break;

      case 'FAILED':
      case 'REJECTED':
        this.handleFileStatusFailed(fileInput, error);
        this.uploadNext();
        break;

      case 'DUPLICATE':
        this.handleFileStatusDuplicate(fileInput, error);
        this.uploadNext();
        break;

      case 'NOT_UPLOADED':
      case 'WAITING':
      default:
        data.retries++;

        if (data.retries > this.config.maxRetries) {
          this.uploadData[fileInput.id].retries = 0;

          this.handleFileStatusFailed(fileInput, this.messages.genericError);
          this.uploadNext();
        }
        else {
          this.delayedRequestUploadStatus(fileRef);
        }

        break;
    }
  }

  private handleFileStatusSuccessful(fileInput: HTMLInputElement, previewUrl: string, description: string) {
    const item = this.getItemFromFile(fileInput);

    this.addNotification(parseTemplate(this.messages.documentUploaded, {
      fileName: this.getFileName(fileInput)
    }));

    this.getFilePreviewElement(item).href = previewUrl;
    this.getDescriptionElement(item).textContent = description;
    this.setItemState(item, UploadState.Uploaded);
    this.updateButtonVisibility();
    this.updateFormStatusVisibility();
  }

  private handleFileStatusFailed(fileInput: HTMLInputElement, errorMessage: string) {
    const item = this.getItemFromFile(fileInput);
    const label = this.getItemLabelElement(item);

    this.setItemState(item, UploadState.Default);
    this.updateFormStatusVisibility();

    this.addUploadInputWithFileAndLabel(fileInput, label);

    this.errorManager.addError(fileInput.id, errorMessage, this.getFileName(fileInput));

    item.remove();
    this.updateFileNumbers();
    this.updateButtonVisibility();
    this.updateFormStatusVisibility();
  }

  private handleFileStatusDuplicate(fileInput: HTMLInputElement, errorMessage: string) {
    const item = this.getItemFromFile(fileInput);

    this.errorManager.addSummaryOnlyError(fileInput.id, errorMessage, this.getFileName(fileInput));

    item.remove();
    this.updateFileNumbers();
    this.updateButtonVisibility();
    this.updateFormStatusVisibility();
  }

  private updateFileNumbers(): void {
    let fileNumber = this.getItems.length;

    this.getItems().forEach(item => {
      Array.from(item.querySelectorAll(`.${this.classes.fileNumber}`)).forEach(span => {
        span.textContent = fileNumber.toString();
      });

      //this.updateItemLabel(item, fileNumber);

      fileNumber--;
    });
  }

  private updateButtonVisibility(): void {
    const itemCount = this.getItems().length;

    this.toggleRemoveButtons(itemCount >= this.config.minFiles);
    this.toggleAddButton(this.config.showAddAnotherDocumentButton && itemCount < this.config.maxFiles);
    this.toggleUploadMoreMessage(itemCount === this.config.maxFiles && this.config.maxFiles > 1);
  }

  private updateFormStatusVisibility(forceState = undefined) {
    if (forceState !== undefined) {
      toggleElement(this.formStatus, forceState);
    }
    else if (!this.isBusy()) {
      toggleElement(this.formStatus, false);
    }
  }

  private updateUploadProgress(item, value): void {
    item.querySelector(`.${this.classes.progressBar}`).style.width = `${value}%`;
  }

  private toggleRemoveButton(item: HTMLElement, state: boolean): void {
    const button = this.getRemoveButtonFromItem(item);

    if (this.isWaiting(item) || this.isUploading(item) || this.isVerifying(item) || this.isUploaded(item)) {
      state = true;
    } else if (this.isEmpty(item)) {
      state = false;
    }

    toggleElement(button, state);
  }

  private toggleRemoveButtons(state: boolean): void {
    this.getItems().forEach(item => this.toggleRemoveButton(item, state));
  }

  private addNotification(message: string): void {
    const element = document.createElement('p');
    element.textContent = message;

    this.notifications.append(element);

    window.setTimeout(() => {
      element.remove();
    }, 1000);
  }

  private toggleAddButton(state: boolean): void {
    toggleElement(this.addAnotherBtn, state);
  }

  private toggleInputLabel(item: HTMLElement, state: boolean): void {
    toggleElement(this.getInputLabelElement(item), state);
  }

  private toggleUploadMoreMessage(state: boolean): void {
    toggleElement(this.uploadMoreMessage, state);
  }

  private getItems(): HTMLElement[] {
    return Array.from(this.itemList.querySelectorAll(`.${this.classes.item}`));
  }

  private getInputs(): HTMLElement[] {
    return Array.from(this.inputList.querySelectorAll(`.${this.classes.input}`));
  }

  private removeAllItems(): void {
    this.getItems().forEach(item => item.remove());
  }

  private getSendUrl(fileId: string): string {
    return parseTemplate(this.config.sendUrlTpl, { fileId: fileId });
  }

  private getStatusUrl(fileRef: string): string {
    return parseTemplate(this.config.statusUrlTpl, { fileRef: fileRef });
  }

  private getRemoveUrl(fileRef: string): string {
    return parseTemplate(this.config.removeUrlTpl, { fileRef: fileRef });
  }

  private getFileByReference(fileRef: string): HTMLInputElement {
    return this.itemList.querySelector(`[data-multi-file-upload-file-ref="${fileRef}"]`);
  }

  private getFileInputFromItem(item: HTMLElement): HTMLInputElement {
    return item.querySelector(`.${this.classes.file}`) as HTMLInputElement;
  }

  private getItemFromFile(file: HTMLInputElement): HTMLElement {
    return file.closest(`.${this.classes.item}`) as HTMLElement;
  }

  private getInputFromFile(file: HTMLInputElement): HTMLElement {
    return file.closest(`.${this.classes.input}`) as HTMLElement;
  }

  private getRemoveButtonFromItem(item: HTMLElement): HTMLButtonElement {
    return item.querySelector(`.${this.classes.remove}`) as HTMLButtonElement;
  }

  private getFileName(fileInput: HTMLInputElement): string {
    const item = this.getItemFromFile(fileInput);
    if(item){
      const fileName = this.getFileNameElement(item)?.textContent.trim();

      if (fileName && fileName.length) {
        return this.extractFileName(fileName);
      }
    }

    if (fileInput.value.length) {
      return this.extractFileName(fileInput.value);
    }

    return null;
  }

  private getFileNameElement(item: HTMLElement): HTMLElement {
    return item?.querySelector(`.${this.classes.fileName}`);
  }

  private getFileNameElements(item: HTMLElement): NodeListOf<HTMLElement> {
    return item.querySelectorAll(`.${this.classes.fileName}`);
  }

  private getFilePreviewElement(item: HTMLElement): HTMLLinkElement {
    return item.querySelector(`.${this.classes.filePreview}`);
  }

  private getDescriptionElement(item: HTMLElement): HTMLElement {
    return item.querySelector(`.${this.classes.description}`);
  }

  private getItemLabelElement(item: HTMLElement): HTMLElement {
    return item.querySelector(`.${this.classes.inputLabel}`);
  }

  private getItemContentElement(item: HTMLElement): HTMLElement {
    return item.querySelector(`.${this.classes.itemContent}`);
  }

  private getInputLabelElement(input: HTMLElement): HTMLElement {
    return input.querySelector(`.${this.classes.inputLabel}`);
  }

  private extractFileName(fileName: string): string {
    return fileName.split(/([\\/])/g).pop();
  }

  private isInProgress(): boolean {
    const stillWaiting = this.container.querySelector(`.${this.classes.waiting}`) !== null;

    return stillWaiting || this.isBusy();
  }

  private isBusy(): boolean {
    const stillUploading = this.container.querySelector(`.${this.classes.uploading}`) !== null;
    const stillVerifying = this.container.querySelector(`.${this.classes.verifying}`) !== null;
    const stillRemoving = this.container.querySelector(`.${this.classes.removing}`) !== null;

    return stillUploading || stillVerifying || stillRemoving;
  }

  private getEmptyItems(): HTMLElement[] {
    return this.getItems().filter(item => item && this.isEmpty(item));
  }

  private hasEmptyItem(): boolean {
    return this.getEmptyItems().length > 0;
  }

  private getEmptyOrErrorItems(): HTMLElement[] {
    return this.getItems().filter(item => item && this.isEmptyOrError(item));
  }

  private hasEmptyOrErrorItem(): boolean {
    return this.getEmptyOrErrorItems().length > 0;
  }

  private isEmpty(item: HTMLElement): boolean {
    return !(this.isWaiting(item)
      || this.isUploading(item)
      || this.isVerifying(item)
      || this.isUploaded(item)
      || this.isRemoving(item)
      || (this.getFileInputFromItem(item) && this.errorManager.hasError(this.getFileInputFromItem(item).id))
    );
  }

  private isEmptyOrError(item: HTMLElement): boolean {
    return !(this.isWaiting(item)
      || this.isUploading(item)
      || this.isVerifying(item)
      || this.isUploaded(item)
      || this.isRemoving(item)
    );
  }

  private isWaiting(item: HTMLElement): boolean {
    return item.classList.contains(this.classes.waiting);
  }

  private isUploading(item: HTMLElement): boolean {
    return item.classList.contains(this.classes.uploading);
  }

  private isVerifying(item: HTMLElement): boolean {
    return item.classList.contains(this.classes.verifying);
  }

  private isUploaded(item: HTMLElement): boolean {
    return item.classList.contains(this.classes.uploaded);
  }

  private isRemoving(item: HTMLElement): boolean {
    return item.classList.contains(this.classes.removing);
  }

  private setItemState(item: HTMLElement, uploadState: UploadState): void {
    const file = this.getFileInputFromItem(item);

    file.disabled = uploadState !== UploadState.Default;

    switch (uploadState) {
      case UploadState.Waiting:
        item.classList.add(this.classes.waiting);
        item.classList.remove(this.classes.uploading, this.classes.verifying, this.classes.uploaded, this.classes.removing);
        break;
      case UploadState.Uploading:
        item.classList.add(this.classes.uploading);
        item.classList.remove(this.classes.waiting, this.classes.verifying, this.classes.uploaded, this.classes.removing);
        break;
      case UploadState.Verifying:
        item.classList.add(this.classes.verifying);
        item.classList.remove(this.classes.waiting, this.classes.uploading, this.classes.uploaded, this.classes.removing);
        break;
      case UploadState.Uploaded:
        item.classList.add(this.classes.uploaded);
        item.classList.remove(this.classes.waiting, this.classes.uploading, this.classes.verifying, this.classes.removing);
        break;
      case UploadState.Removing:
        item.classList.add(this.classes.removing);
        item.classList.remove(this.classes.waiting, this.classes.uploading, this.classes.verifying, this.classes.uploaded);
        break;
      case UploadState.Default:
        item.classList.remove(this.classes.waiting, this.classes.uploading, this.classes.verifying, this.classes.uploaded, this.classes.removing);
        break;
    }
  }

  private handleFileDrop(event: DragEvent): void {

    // Prevent default behavior (Prevent file from being opened)
    event.preventDefault();

    if (event.dataTransfer.items) {
      this.errorManager.removeAllErrors();
      // Use DataTransferItemList interface to access the file(s)
      [...event.dataTransfer.items].forEach((item, i) => {
        // If dropped items aren't files, reject them
        if (item.kind === "file") {
          const file = item.getAsFile();
          this.createWaitingItem(file);
        }
      });
      this.uploadNext();
    } else {
      this.errorManager.removeAllErrors();
      // Use DataTransfer interface to access the file(s)
      [...event.dataTransfer.files].forEach((file, i) => {
        this.createWaitingItem(file);
      });
      this.uploadNext();
    }
  }

  private handleFileDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  private handleDragEnter(event: DragEvent): void {
    event.preventDefault();
  }

  private handleDragLeave(event: DragEvent): void {
    event.preventDefault();
  }
}
