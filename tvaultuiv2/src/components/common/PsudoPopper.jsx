import React from 'react';
import styled from 'styled-components';
import EditIcon from '@material-ui/icons/Edit';
import DeleteIcon from '@material-ui/icons/Delete';

const IconWrap = styled('div')`
  display: flex;
  width: 5rem;
`;

const PsudoPopper = () => {
  return (
    <IconWrap>
      <EditIcon />
      <DeleteIcon />
    </IconWrap>
  );
};

export default PsudoPopper;
