import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import TransferOwner from './index';

test('renders transfer safe owner component', () => {
  const transferData = {
    owner: 'tester@t-mobile.com',
    type: 'userSafe',
    name: 'testSafe1',
  };
  const onCancleClick = () => {};
  const onTransferClick = () => {};

  const { getAllByText, getByPlaceholderText } = render(
    <TransferOwner
      transferData={transferData}
      onTransferCancelClicked={onCancleClick}
      onTransferOwnerConfirmationClicked={onTransferClick}
    />
  );
  expect(getAllByText(/Transfer/i)).not.toBeNull();
  expect(getAllByText(/Current Owner:/i).value).not.toBeNull();
  const ownerId = getByPlaceholderText(/Search by NTID, Email or Name/i);

  expect(ownerId.value).toBe('');
  fireEvent.change(ownerId, {
    target: { value: 'tester@email.com' },
  });
  expect(ownerId.value).toBe('tester@email.com');
});
