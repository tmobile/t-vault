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
  justify-content: center;
  margin-left: 0.75rem;
  :hover {
    background-color: #5a637a;
    border-radius: 50%;
  }
`;

const TransferOwnerWrap = styled.div`
  :hover {
    background-color: #5a637a;
    border-radius: 0.4rem;
  }
`;

const PsudoPopper = (props) => {
  const {
    onDeletListItemClicked,
    onEditListItemClicked,
    admin,
    onTransferOwnerClicked,
  } = props;

  return (
    <ComponentError>
      <IconWrap>
        {admin && (
          <TransferOwnerWrap onClick={onTransferOwnerClicked}>
            Transfer Owner
          </TransferOwnerWrap>
        )}
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
  onTransferOwnerClicked: PropTypes.func,
  admin: PropTypes.bool,
};

PsudoPopper.defaultProps = {
  onDeletListItemClicked: () => {},
  onEditListItemClicked: () => {},
  onTransferOwnerClicked: () => {},
  admin: true,
};

export default PsudoPopper;
