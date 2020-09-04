import React from 'react';
import { render } from '@testing-library/react';
import Secrets from './index';

test('renders add secrets component', () => {
  const { getByText, getByLabelText } = render(<Secrets />);
  expect(getByText(/Add Secrets/i)).not.toBeNull();
  expect(getByLabelText(/Key ID/i)).not.toBeNull();
  expect(getByLabelText(/Secret/i)).not.toBeNull();
});
