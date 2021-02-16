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
  display: flex;
  width: 3rem;
  height: 3rem;
  align-items: center;
  justify-content: center;
  margin-left: 0.75rem;
  padding: 0.66rem 0.5rem 0.34rem 0.9rem;
  border-radius: 50%;
  :hover {
    background-color: rgb(90, 99, 122);
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
