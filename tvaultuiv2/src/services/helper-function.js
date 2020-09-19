/* eslint-disable array-callback-return */
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

// export function mockAddSecrets

export function findElementById(arr, id, nestingKey) {
  // if empty array then return
  if (arr.length === 0) return;
  // return element if found else collect all children(or other nestedKey) array and run this function
  return (
    arr.find((d) => d.labelText === id) ||
    findElementById(
      arr.flatMap((d) => d[nestingKey] || []),
      id,
      nestingKey
    ) ||
    'Not found'
  );
}

export const findElementAndUpdate = (arr, parentId, item) => {
  if (arr.length === 0) return;
  const tempArr = [...arr];
  const itemToUpdate = findElementById(tempArr, parentId, 'children');
  itemToUpdate.children = [...itemToUpdate.children, item];

  return tempArr;
};

export const makeSafesList = (array, type) => {
  const safeArray = [];
  array.map((item) => {
    const data = {
      name: Object.keys(item)[0],
      access: Object.values(item)[0],
      path: `${type}/${Object.keys(item)[0]}`,
    };
    safeArray.push(data);
  });
  return safeArray;
};

export const createArrayOfObject = (array, type) => {
  const safeArray = [];
  array.map((item) => {
    const data = {
      name: item,
      path: `${type}/${item}`,
    };
    safeArray.push(data);
  });
  return safeArray;
};

export const removeDuplicate = (arr) => {
  const filteredArr = arr.reduce((acc, current) => {
    const x = acc.find((item) => item.name === current.name);
    if (!x) {
      return acc.concat([current]);
    }
    return acc;
  }, []);
  return filteredArr;
};
