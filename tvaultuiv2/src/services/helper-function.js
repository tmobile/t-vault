/* eslint-disable import/no-cycle */
/* eslint-disable guard-for-in */
/* eslint-disable array-callback-return */
/* eslint-disable consistent-return */
// eslint-disable-next-line import/prefer-default-export

import apiService from '../views/private/safe/apiService';

const FileDownload = require('js-file-download');

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
    arr.find((d) => d?.id?.toLowerCase() === id?.toLowerCase()) ||
    findElementById(
      arr.flatMap((d) => d[nestingKey] || []),
      id,
      nestingKey
    ) ||
    'Not found'
  );
}
export const findItemAndRemove = (arr, key, id) => {
  if (arr?.length === 0) return;
  const tempArr = [...arr];
  const indexofItem =
    tempArr[0] && tempArr[0][key].findIndex((item) => item.id === id);
  if (indexofItem >= 0) tempArr[0][key].splice(indexofItem, 1);
  return tempArr;
};

export const findElementAndUpdate = (arr, parentId, item) => {
  if (arr?.length === 0) return;
  const tempArr = [...arr];
  const itemToUpdate = findElementById(tempArr, parentId, 'children');
  if (Array.isArray(item)) {
    if (!itemToUpdate?.children?.length) {
      itemToUpdate.children = [...itemToUpdate.children, ...item];
      return tempArr;
    }
    itemToUpdate.children = [...item];
  } else {
    itemToUpdate.children = itemToUpdate?.children && [
      ...itemToUpdate.children,
      item,
    ];
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
  const currentSecretData = currentParentSecret?.children?.filter(
    (sec) => sec.type.toLowerCase() === 'secret'
  )[0];
  const obj = currentSecretData && JSON.parse(currentSecretData.value);
  return obj;
};

export const convertObjectToArray = (data) => {
  const array = [];
  // eslint-disable-next-line no-restricted-syntax
  for (const [key, value] of Object.entries(data?.data)) {
    if (key?.toLowerCase() !== 'default') array.push({ [key]: value });
  }
  return array;
};

const setSafeType = (type) => {
  let safeType = '';
  if (type === 'users') {
    safeType = 'User safe';
  } else if (type === 'apps') {
    safeType = 'Application safe';
  } else if (type === 'shared') {
    safeType = 'Shared safe';
  }
  return safeType;
};

export const makeSafesList = (array, type) => {
  const safeArray = [];
  array.map((item) => {
    const data = {
      name: Object.keys(item)[0],
      access: Object.values(item)[0],
      path: `${type}/${Object.keys(item)[0]}`,
      manage: true,
      safeType: setSafeType(type),
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
      safeType: setSafeType(type),
      access: '',
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
  return false;
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

export const formatSecondsToTime = (seconds) => {
  const date = Number(seconds);
  if (date) {
    // const duration = moment.duration(date, 'seconds');
    const days = Math.round(date / 86400);
    const hours = Math.round(date / 3600);
    const minutes = Math.round(date / 60);
    if (days >= 1) {
      return `${days} day/s`;
    }
    if (hours >= 1 && hours < 24) {
      return `${hours} hours`;
    }
    if (days === 0 || hours === 0) {
      return 'few seconds';
    }
    return `${minutes} minutes`;
  }
};
export const checkAccess = (access, type) => {
  let val = '';
  if (access === 'write' || access === 'reset') {
    if (type?.toLowerCase() === 'iamsvcaccount') {
      val = 'rotate';
      return val;
    }
    val = 'reset';
  } else {
    val = access;
  }
  return val;
};

export const getDaysDifference = (end) => {
  const date1 = new Date();
  const date2 = new Date(end);
  const diffInTime = Math.abs(date2.getTime() - date1.getTime());
  const diffInTimeDays = diffInTime / (1000 * 3600 * 24);
  return Math.ceil(diffInTimeDays);
};

export const convertToCSV = (objArray) => {
  const array = typeof objArray !== 'object' ? JSON.parse(objArray) : objArray;
  let str = '';
  // eslint-disable-next-line no-plusplus
  for (let i = 0; i < array.length; i++) {
    let line = '';
    // eslint-disable-next-line no-restricted-syntax
    for (const index in array[i]) {
      if (line !== '') line += ',';

      line += array[i][index];
    }

    str += `${line}\r\n`;
  }
  return str;
};
export const exportCSVFile = (headers, items, fileTitle) => {
  if (headers) {
    items.unshift(headers);
  }
  // Convert Object to JSON
  const jsonObject = JSON.stringify(items);
  const csv = convertToCSV(jsonObject);
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  FileDownload(blob, fileTitle);
};

export const addLeadingZeros = (value) => {
  let val = value?.toString();
  if (val?.length < 2) {
    val = `0${value}`;
  }
  return val;
};

export const getEachUsersDetails = (data) => {
  if (data && Object.keys(data).length > 0) {
    const userNameArray = [];
    Object.keys(data).map((item) => {
      return userNameArray.push(item);
    });
    return apiService
      .getUsersDetails(userNameArray?.join())
      .then((res) => {
        if (res.data.data.values) {
          return res.data.data.values;
        }
        return null;
      })
      .catch(() => {
        return null;
      });
  }
  return null;
};
