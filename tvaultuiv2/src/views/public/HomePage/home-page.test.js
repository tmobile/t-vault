import React from 'react';
import { render } from '@testing-library/react';
import HomePage from './index';

test('Loads home page with welcome text', () => {
  const { getByRole } = render(<HomePage>Hello!!</HomePage>);

  expect(getByRole('heading')).toHaveTextContent('Welcome to home page');
});
