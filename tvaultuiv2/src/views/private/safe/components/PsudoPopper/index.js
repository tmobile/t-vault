import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
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

const PsudoPopper = (props) => {
  const { onDeleteSafeClicked, safe, path } = props;

  return (
    <ComponentError>
      <IconWrap>
        <Link
          to={{
            pathname: path,
            state: { safe },
          }}
        >
          <Icon>
            {' '}
            <IconEdit />
          </Icon>
        </Link>
        <Icon onClick={onDeleteSafeClicked}>
          {' '}
          <IconDeleteActive />
        </Icon>
      </IconWrap>
    </ComponentError>
  );
};

PsudoPopper.propTypes = {
  onDeleteSafeClicked: PropTypes.func,
  safe: PropTypes.objectOf(PropTypes.any),
  path: PropTypes.string,
};

PsudoPopper.defaultProps = {
  onDeleteSafeClicked: () => {},
  safe: {},
  path: '',
};

export default PsudoPopper;
