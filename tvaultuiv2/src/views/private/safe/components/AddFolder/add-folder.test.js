import React from 'react';
import { render } from '@testing-library/react';
import AddFolder from './index';

test('renders add folder component', () => {
  const { getByText } = render(<AddFolder />);
  expect(getByText(/Add Folder Name*/i)).not.toBeNull();
});
