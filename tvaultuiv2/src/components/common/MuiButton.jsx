/* eslint-disable react/require-default-props */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Button } from '@material-ui/core';
import ComponentError from 'errorBoundaries/ComponentError/component-error';

const ActionButton = styled('div')`
  display: flex;
  align-items: center;
  .MuiButton-root {
    ${(props) => props.customBtnStyle};
  }
`;

const MuiButton = (props) => {
  const { label, icon, customStyle } = props;
  return (
    <ComponentError>
      <ActionButton customBtnStyle={customStyle}>
        <Button variant="contained" color="secondary" startIcon={icon}>
          {label}
        </Button>
      </ActionButton>
    </ComponentError>
  );
};
MuiButton.propTypes = {
  // eslint-disable-next-line react/require-default-props
  label: PropTypes.string,
  // eslint-disable-next-line react/require-default-props
  icon: PropTypes.node,
  // eslint-disable-next-line react/forbid-prop-types
  customStyle: PropTypes.object,
};
export default MuiButton;
