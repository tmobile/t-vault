import React from 'react';
import PropTypes from 'prop-types';

const HomePage = (props) => {
  const { message } = props;
  return (
    <section>
      <h1>Welcome to home page</h1>
      <div>{`Message is ${message}`}</div>
    </section>
  );
};

HomePage.propTypes = {
  message: PropTypes.string,
};

HomePage.defaultProps = {
  message: 'Welcome',
};

export default HomePage;
