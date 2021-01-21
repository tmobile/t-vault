import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import Secrets from './index';

test('renders add secrets component', () => {
  const { getAllByText, getByTestId } = render(<Secrets />);
  expect(getAllByText(/Add Secret/i)).not.toBeNull();
  const keyLabel = getAllByText(/Key Id/i);
  expect(keyLabel).not.toBeNull();
  const keyInput = getByTestId('keyId');
  fireEvent.change(keyInput, {
    target: { value: 'secret' },
  });
  expect(keyInput).toHaveValue('secret');
  const secretLabel = getAllByText(/Secret/i);
  expect(secretLabel).not.toBeNull();
  const contentInput = getByTestId('safeSecret');
  fireEvent.change(contentInput, {
    target: { value: 'password' },
  });
  expect(contentInput).toHaveValue('password');
});
