/* eslint-disable import/no-unresolved */
import React from 'react';
import styled from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import PropTypes from 'prop-types';

const StyledFile = styled.div`
  padding-left: 20px;
  display: flex;
  align-items: center;
  span {
    margin-left: 5px;
  }
`;

const File = (props) => {
  const { name } = props;
  return (
    <ComponentError>
      <StyledFile>
        <span>{name}</span>
      </StyledFile>
    </ComponentError>
  );
};
File.propTypes = {
  name: PropTypes.string,
};
File.defaultProps = {
  name: '',
};
export default File;
