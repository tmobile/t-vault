/* eslint-disable consistent-return */
// eslint-disable-next-line import/prefer-default-export
export function mockApi(response) {
  return new Promise((resolve, reject) =>
    setTimeout(() => {
      if (response === 'error') {
        reject(new Error('Failed to fetch Data.'));
      }
      resolve({ data: response });
    }, 1000)
  );
}

export function mockCreateSafe(response) {
  return new Promise((resolve, reject) =>
    setTimeout(() => {
      if (response === 'error') {
        reject(new Error('failed to create data'));
      }
      resolve({ data: [response] });
    })
  );
}

export function findElementById(arr, id, nestingKey) {
  // if empty array then return
  if (arr.length === 0) return;
  // return element if found else collect all children(or other nestedKey) array and run this function
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
  for (let i = 0; i < tempArr.length; i += 1) {
    const element = tempArr[i];
    if (element.labelText === id) {
      element.children = [...element.children, item];
      return tempArr;
    }
    if (element.children)
      findElementAndUpdate(element.children, id, parentId, item);
  }
}
