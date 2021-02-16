import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import CreateSafe from './index';

test('renders Create safe component', () => {
  const { getAllByText, getByPlaceholderText } = render(<CreateSafe />);
  expect(getAllByText(/Create Safe/i)).not.toBeNull();
  const safeName = getByPlaceholderText(/Safe Name- Enter min 3 characters/i);
  expect(safeName.value).toBe('');
  fireEvent.change(safeName, { target: { value: 'testsafe' } });
  expect(safeName.value).toBe('testsafe');
  const ownerName = getByPlaceholderText(/Search by NTID, Email or Name/i);
  expect(ownerName.value).toBe('');
  fireEvent.change(ownerName, { target: { value: 'tester@email.com' } });
  expect(ownerName.value).toBe('tester@email.com');
  const description = getByPlaceholderText(/Add some details about this safe/i);
  expect(description.value).toBe('');
  fireEvent.change(description, { target: { value: 'testing description' } });
  expect(description.value).toBe('testing description');
});
