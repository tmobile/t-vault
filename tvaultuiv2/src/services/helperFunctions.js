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
