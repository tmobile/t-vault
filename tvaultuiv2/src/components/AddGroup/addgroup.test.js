import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import Group from './index';

test('renders add users permissions component', () => {
  const { getAllByPlaceholderText, getByTestId } = render(<Group />);
  const groupNameInput = getAllByPlaceholderText(
    /Groupname - Enter min 3 characters/i
  );
  expect(groupNameInput).not.toBeNull();
  const contentInput = getByTestId('grpVal');
  fireEvent.change(contentInput, {
    target: { value: 'GroupName' },
  });
  expect(contentInput).toHaveValue('GroupName');
});
