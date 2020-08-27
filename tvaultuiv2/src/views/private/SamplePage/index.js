import React from 'react';
import PropTypes from 'prop-types';

const SamplePage = (props) => {
  const { salutation } = props;
  return (
    <div>
      <h1>This is a sample page in private folder!!!</h1>
      <div>{salutation}</div>
    </div>
  );
};
SamplePage.propTypes = {
  salutation: PropTypes.string,
};

SamplePage.defaultProps = {
  salutation: 'Welcome',
};
export default SamplePage;
