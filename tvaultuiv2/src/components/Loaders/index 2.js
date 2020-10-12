import React from 'react';
import styled from 'styled-components';

// styled components
const LoaderContainer = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
`;

const Loader = () => {
  return (
    <LoaderContainer>
      <div>Loading...</div>
    </LoaderContainer>
  );
};

export default Loader;
