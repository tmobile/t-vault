/* eslint-disable guard-for-in */
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
    arr.find((d) => d.value.toLowerCase() === id.toLowerCase()) ||
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
  if (Array.isArray(item)) {
    const isItemExist = itemToUpdate.children.filter((itm) =>
      item.includes(itm)
    );
    if (!itemToUpdate.children.length) {
      itemToUpdate.children = [...itemToUpdate.children, ...item];
      return;
    }
    if (!isItemExist) {
      itemToUpdate.children = [
        ...itemToUpdate.children.filter((itm) => item.indexOf(itm) === -1),
      ];
      // [...itemToUpdate.children, ...item];
    }
  } else {
    itemToUpdate.children = [...itemToUpdate.children, item];
  }

  return tempArr;
};

export const findSecretAndUpdate = (arr, parentId, secrets) => {
  if (arr.length === 0) return;
  const tempArr = [...arr];
  const itemToUpdate = findElementById(tempArr, parentId, 'children');
  const currentSecretData = itemToUpdate.children.filter(
    (sec) => sec.type.toLowerCase() === 'secret'
  )[0];
  currentSecretData.value = JSON.stringify({ secrets });

  return tempArr;
};

export const findElementAndDelete = (arr, parent, key) => {
  const currentParentSecret = findElementById(arr, parent, 'children');
  const currentSecretData = currentParentSecret.children.filter(
    (sec) => sec.type.toLowerCase() === 'secret'
  )[0];

  const obj = JSON.parse(currentSecretData.value);
  delete obj.data[key];
  return obj;
};

export const findElementAndReturnSecrets = (arr, idToFind) => {
  const currentParentSecret = findElementById(arr, idToFind, 'children');
  const currentSecretData = currentParentSecret.children.filter(
    (sec) => sec.type.toLowerCase() === 'secret'
  )[0];
  const obj = JSON.parse(currentSecretData.value);
  return obj;
};

export const convertObjectToArray = (data) => {
  const array = [];
  // eslint-disable-next-line no-restricted-syntax
  for (const [key, value] of Object.entries(data.data)) {
    array.push({ [key]: value });
  }
  return array;
};

export const makeSafesList = (array, type) => {
  const safeArray = [];
  array.map((item) => {
    const data = {
      name: Object.keys(item)[0],
      access: Object.values(item)[0],
      path: `${type}/${Object.keys(item)[0]}`,
      manage: true,
    };
    safeArray.push(data);
  });
  return safeArray;
};

export const createSafeArray = (arr, type) => {
  const safeArray = [];
  arr.map((item) => {
    const data = {
      name: item,
      path: `${type}/${item}`,
      manage: true,
    };
    safeArray.push(data);
  });
  return safeArray;
};

export const validateEmail = (email) => {
  if (email) {
    const res = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/; //eslint-disable-line
    return res.test(email);
  }
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
