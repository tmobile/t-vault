import React from 'react';
import PropTypes from 'prop-types';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

const Applications = (props) => {
  const { message } = props;
  return (
    <ComponentError>
      <section>
        <h1>Welcome to Applications!!</h1>
        <div>{`Message is ${message}`}</div>
      </section>
    </ComponentError>
  );
};

Applications.propTypes = {
  message: PropTypes.string,
};

Applications.defaultProps = {
  message: 'Welcome',
};

export default Applications;
