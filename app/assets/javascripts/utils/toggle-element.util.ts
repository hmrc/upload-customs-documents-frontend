const hiddenClass = 'hidden';

export default function toggleElement(element: HTMLElement, state: boolean): void {
  //if(element && element.classList){
    if(state)
      element.classList.remove(hiddenClass);
    else
      element.classList.add(hiddenClass);
  //}
}
