import React from 'react';
import styled from 'styled-components';
import EditOutlinedIcon from '@material-ui/icons/EditOutlined';
import DeleteOutlineOutlinedIcon from '@material-ui/icons/DeleteOutlineOutlined';
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
        <EditOutlinedIcon />
        <DeleteOutlineOutlinedIcon />
      </IconWrap>
    </ComponentError>
  );
};

export default PsudoPopper;
