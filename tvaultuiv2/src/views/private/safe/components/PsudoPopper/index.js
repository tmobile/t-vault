import React from 'react';
import styled from 'styled-components';
import { IconDeleteActive, IconEdit } from '../../../../../assets/SvgIcons';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';

const IconWrap = styled('div')`
  display: flex;
  justify-content: space-between;
`;

const Icon = styled('div')`
    // width: 2.5rem;
    // height:2.5rem;
    display: flex;
    align-items: center;
    justify-content: center
    :hover {
    background-color: #5a637a;
    border-radius: 50%;
    
  }
`;

const PsudoPopper = () => {
  return (
    <ComponentError>
      <IconWrap>
        <Icon>
          {' '}
          <IconEdit />
        </Icon>
        <Icon>
          {' '}
          <IconDeleteActive />
        </Icon>
      </IconWrap>
    </ComponentError>
  );
};

export default PsudoPopper;
