import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import SyncAltIcon from '@material-ui/icons/SyncAlt';
import { IconDeleteActive, IconEdit } from '../../assets/SvgIcons';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';

const IconWrap = styled('div')`
  display: flex;
  justify-content: space-between;
`;

const Icon = styled('div')`
  display: flex;
  width: 3rem;
  height: 3rem;
  align-items: center;
  justify-content: center;
  margin-left: 0.75rem;
  padding: 0.5rem 0.4rem 0.5rem 0.8rem;
  border-radius: 50%;
  :hover {
    background-color: #5a637a;
  }
`;

const TransferOwnerWrap = styled.div`
  display: flex;
  align-items: center;
  width: 3rem;
  height: 3rem;
  justify-content: center;
  padding: 0.5rem;
  border-radius: 50%;
  :hover {
    background-color: #5a637a;
  }
`;

const PsudoPopper = (props) => {
  const {
    onDeletListItemClicked,
    onEditListItemClicked,
    admin,
    onTransferOwnerClicked,
    isTransferOwner,
  } = props;

  return (
    <ComponentError>
      <IconWrap>
        {admin && isTransferOwner && (
          <TransferOwnerWrap onClick={onTransferOwnerClicked}>
            <SyncAltIcon style={{ fill: '#fff' }} />
          </TransferOwnerWrap>
        )}
        <Icon onClick={onEditListItemClicked}>
          <IconEdit />
        </Icon>
        {admin && (
          <Icon onClick={onDeletListItemClicked}>
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
  isTransferOwner: PropTypes.bool,
};

PsudoPopper.defaultProps = {
  onDeletListItemClicked: () => {},
  onEditListItemClicked: () => {},
  onTransferOwnerClicked: () => {},
  admin: true,
  isTransferOwner: false,
};

export default PsudoPopper;
