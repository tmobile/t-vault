import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import Permissions from './index';

test('renders add users permissions component', () => {
  const { getAllByPlaceholderText, getByTestId } = render(<Permissions />);
  const deviceNameInput = getAllByPlaceholderText(
    /Search by NTID, Email or Name/i
  );
  expect(deviceNameInput).not.toBeNull();
  const contentInput = getByTestId('userVal');
  fireEvent.change(contentInput, {
    target: { value: 'UserName' },
  });
  expect(contentInput).toHaveValue('UserName');
});
