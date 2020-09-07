// eslint-disable-next-line import/prefer-default-export
export function findElementById(arr, id, nestingKey) {
  // if empty array then return
  if (arr.length === 0) return;

  // return element if found else collect all children(or other nestedKey) array and run this function
  // eslint-disable-next-line consistent-return
  return (
    arr.find((d) => d.id === id) ||
    findElementById(
      arr.flatMap((d) => d[nestingKey] || []),
      id
    ) ||
    'Not found'
  );
}

export function findElementAndUpdate(arr, id, parentId, item) {
  const tempArr = [...arr];
  // if empty array then return
  if (arr.length === 0) return;
  // return element if found else collect all children(or other nestedKey) array and run this function
  // eslint-disable-next-line consistent-return
  for (let index = 0; index < tempArr.length; index++) {
    const element = tempArr[index];
    if (element.labelText === id) {
      element.children = [...element.children, item];
      return tempArr;
    } else {
      if (element.children) {
        findElementAndUpdate(element.children, id, parentId, item);
      }
    }
  }
}
