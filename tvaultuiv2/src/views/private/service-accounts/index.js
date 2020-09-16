import React from 'react';
import PropTypes from 'prop-types';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

const ServiceAccounts = (props) => {
  const { message } = props;
  return (
    <ComponentError>
      <section>
        <h1>Welcome to Service Accounts!!</h1>
        <div>{`Message is ${message}`}</div>
      </section>
    </ComponentError>
  );
};

ServiceAccounts.propTypes = {
  message: PropTypes.string,
};

ServiceAccounts.defaultProps = {
  message: 'Welcome',
};

export default ServiceAccounts;
