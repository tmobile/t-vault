import React from 'react';
import PropTypes from 'prop-types';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

const HomePage = (props) => {
  const { message } = props;
  return (
    <ComponentError>
      <section>
        <h1>Welcome to home page</h1>
        <div>{`Message is ${message}`}</div>
      </section>
    </ComponentError>
  );
};

HomePage.propTypes = {
  message: PropTypes.string,
};

HomePage.defaultProps = {
  message: 'Welcome',
};

export default HomePage;
