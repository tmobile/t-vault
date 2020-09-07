import React from 'react';
import styled from 'styled-components';
import EditIcon from '@material-ui/icons/Edit';
import DeleteIcon from '@material-ui/icons/Delete';
// eslint-disable-next-line import/no-unresolved
import ComponentError from 'errorBoundaries/ComponentError/component-error';

const IconWrap = styled('div')`
  display: flex;
  width: 5rem;
`;

const PsudoPopper = () => {
  return (
    <ComponentError>
      <IconWrap>
        <EditIcon />
        <DeleteIcon />
      </IconWrap>
    </ComponentError>
  );
};

export default PsudoPopper;
