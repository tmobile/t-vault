import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

// styled components
const LoaderContainer = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  ${(props) => props.customStyle};
`;

const Loader = (props) => {
  const { customStyle } = props;

  return (
    <LoaderContainer customStyle={customStyle}>
      <div>Loading...</div>
    </LoaderContainer>
  );
};

Loader.propTypes = {
  customStyle: PropTypes.arrayOf(PropTypes.any),
};

Loader.defaultProps = {
  customStyle: [],
};
export default Loader;
