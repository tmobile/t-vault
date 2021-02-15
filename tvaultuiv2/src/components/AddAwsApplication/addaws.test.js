import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import AddAws from './index';

test('renders add secrets component', () => {
  const { getAllByText, getByTestId } = render(<AddAws />);
  expect(getAllByText(/Create AWS Configuration/i)).not.toBeNull();

  expect(getAllByText(/Role Name/i)).not.toBeNull();
  const roleNameInput = getByTestId('roleName');
  fireEvent.change(roleNameInput, {
    target: { value: 'role-name' },
  });
  expect(roleNameInput).toHaveValue('role-name');

  expect(getAllByText(/IAM Principal ARN/i)).not.toBeNull();
  const iamPrincipalArnInput = getByTestId('iamPrincipalArn');
  fireEvent.change(iamPrincipalArnInput, {
    target: { value: 'iamPrincipalArnVal' },
  });
  expect(iamPrincipalArnInput).toHaveValue('iamPrincipalArnVal');

  expect(getAllByText(/VPC ID*/i)).not.toBeNull();
  const contentInput = getByTestId('vpcId');
  fireEvent.change(contentInput, {
    target: { value: '12345' },
  });
  expect(contentInput).toHaveValue('12345');

  expect(getAllByText(/Subnet ID*/i)).not.toBeNull();
  const subnetIdInput = getByTestId('subnetId');
  fireEvent.change(subnetIdInput, {
    target: { value: 'subnetid' },
  });
  expect(subnetIdInput).toHaveValue('subnetid');

  expect(getAllByText(/AMI ID*/i)).not.toBeNull();
  const amiIdInput = getByTestId('amiId');
  fireEvent.change(amiIdInput, {
    target: { value: 'amiIdVal' },
  });
  expect(amiIdInput).toHaveValue('amiIdVal');

  expect(getAllByText(/Instance Profile ARN*/i)).not.toBeNull();
  const profileArnInput = getByTestId('profileArn');
  fireEvent.change(profileArnInput, {
    target: { value: 'profileArnVal' },
  });
  expect(profileArnInput).toHaveValue('profileArnVal');
});
