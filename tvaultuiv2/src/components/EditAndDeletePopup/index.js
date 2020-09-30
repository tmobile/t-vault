import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { IconDeleteActive, IconEdit } from '../../assets/SvgIcons';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';

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

const PsudoPopper = (props) => {
  const { onDeletListItemClicked, item } = props;

  return (
    <ComponentError>
      <IconWrap>
        <Link
          to={{
            pathname: '/safe/edit-safe',
            state: { item },
          }}
        >
          <Icon>
            {' '}
            <IconEdit />
          </Icon>
        </Link>
        <Icon onClick={onDeletListItemClicked}>
          {' '}
          <IconDeleteActive />
        </Icon>
      </IconWrap>
    </ComponentError>
  );
};

PsudoPopper.propTypes = {
  onDeletListItemClicked: PropTypes.func,
  item: PropTypes.objectOf(PropTypes.any),
};

PsudoPopper.defaultProps = {
  onDeletListItemClicked: () => {},
  item: {},
};

export default PsudoPopper;
