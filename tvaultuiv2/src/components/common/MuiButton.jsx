import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Button } from '@material-ui/core';

const ActionButton = styled('div')``;

const MuiButton = (props) => {
  const { label, icon } = props;
  return (
    <ActionButton>
      <Button variant="contained" color="secondary" startIcon={icon}>
        {label}
      </Button>
    </ActionButton>
  );
};
MuiButton.propTypes = {
  label: PropTypes.string,
  icon: PropTypes.node,
};
export default MuiButton;
