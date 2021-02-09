import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import AddFolder from './index';

test('renders add users permissions component', () => {
  const { getAllByPlaceholderText, getByTestId } = render(<AddFolder />);
  const deviceNameInput = getAllByPlaceholderText(/Add folder/i);
  expect(deviceNameInput).not.toBeNull();
  const contentInput = getByTestId('folderName');
  fireEvent.change(contentInput, {
    target: { value: 'FolderName' },
  });
  expect(contentInput).toHaveValue('FolderName');
});
