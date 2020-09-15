import React from 'react';
import { render } from '@testing-library/react';
import Permissions from './index';

test('renders add permissions component', () => {
  const { getByText } = render(<Permissions />);
  expect(getByText(/Add User/i)).not.toBeNull();
});
