import React from 'react';
import { render } from '@testing-library/react';
import TypeAheadComponent from '.';

test('renders TypeAheadComponent component', () => {
  let userValue = 'John';
  const onSelected = (e, v) => {
    userValue = v;
  };
  const onChange = (e) => {
    userValue = e.target.value;
  };
  const { getByPlaceholderText } = render(
    <TypeAheadComponent
      options={['John']}
      userInput={userValue}
      onSelected={onSelected}
      onChange={onChange}
      placeholder="Text Field"
    />
  );
  expect(getByPlaceholderText(/Text Field/i).value).toBe('John');
});
