import React from 'react';
import PropTypes from 'prop-types';
// eslint-disable-next-line import/no-unresolved
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

const Certificates = (props) => {
  const { message } = props;
  return (
    <ComponentError>
      <section>
        <h1>Welcome to Certificates!!</h1>
        <div>{`Message is ${message}`}</div>
      </section>
    </ComponentError>
  );
};

Certificates.propTypes = {
  message: PropTypes.string,
};

Certificates.defaultProps = {
  message: 'Welcome',
};

export default Certificates;
