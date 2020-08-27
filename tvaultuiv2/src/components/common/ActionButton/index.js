import React from 'react';

import styled from 'styled-components';
import { Button } from '@material-ui/core';
import Icon from '@material-ui/core/Icon';

const ActionButton = styled('div')``;
const MuiButton = (props) => {
  const { label } = props;
  return (
    <ActionButton>
      <Button
        variant="contained"
        color="secondary"
        startIcon={<Icon>add_circle</Icon>}
      >
        {label}
      </Button>
    </ActionButton>
  );
};

export default MuiButton;
