import React from 'react';
import { Link } from 'react-router-dom';
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

const TransferOwnerWrap = styled.div``;

const PsudoPopper = (props) => {
  const {
    onDeletListItemClicked,
    onEditListItemClicked,
    item,
    admin,
    onTransferOwnerClicked,
  } = props;

  return (
    <ComponentError>
      <IconWrap>
        <Icon onClick={onEditListItemClicked}>
          <IconEdit />
        </Icon>

        {admin && (
          <TransferOwnerWrap onClick={onTransferOwnerClicked}>
            Transfer Owner
          </TransferOwnerWrap>
        )}
        <Link
          to={{
            pathname: `/${item.name}`,
            state: { item },
          }}
        >
          <Icon>
            {' '}
            <IconEdit />
          </Icon>
        </Link>
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
  item: PropTypes.objectOf(PropTypes.any),
  onTransferOwnerClicked: PropTypes.func,
  admin: PropTypes.bool,
};

PsudoPopper.defaultProps = {
  onDeletListItemClicked: () => {},
  onEditListItemClicked: () => {},
  item: {},
  onTransferOwnerClicked: () => {},
  admin: true,
};

export default PsudoPopper;
