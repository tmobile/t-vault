import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { IconDeleteActive, IconEdit } from '../../assets/SvgIcons';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';

const IconWrap = styled('div')`
  display: flex;
  justify-content: space-between;
`;

const Icon = styled('div')`
    display: flex;
    align-items: center;
    justify-content: center
    :hover {
    background-color: #5a637a;
    border-radius: 50%;
    
  }
`;

const PsudoPopper = (props) => {
  const { onDeletListItemClicked, onEditListItemClicked, admin } = props;

  return (
    <ComponentError>
      <IconWrap>
        <Icon onClick={onEditListItemClicked}>
          <IconEdit />
        </Icon>

        {admin && (
          <Icon onClick={onDeletListItemClicked}>
            {' '}
            <IconDeleteActive />
          </Icon>
        )}
      </IconWrap>
    </ComponentError>
  );
};

PsudoPopper.propTypes = {
  onDeletListItemClicked: PropTypes.func,
  onEditListItemClicked: PropTypes.func,
  admin: PropTypes.bool,
};

PsudoPopper.defaultProps = {
  onDeletListItemClicked: () => {},
  onEditListItemClicked: () => {},
  admin: true,
};

export default PsudoPopper;
