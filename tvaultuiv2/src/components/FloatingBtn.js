import React from 'react';
import styled from 'styled-components';
import { withStyles } from '@material-ui/core/styles';
import { Avatar } from '@material-ui/core';
import PropTypes from 'prop-types';
import createIcon from '../assets/icon-create-newsafe.svg';

const AvatarWrapper = styled.div`
  cursor: pointer;
`;

const styles = () => ({
  float: {
    position: 'absolute',
    bottom: '1rem',
    right: '1rem',
    zIndex: '2',
  },
});

const FloatingButtonComponent = (props) => {
  const { classes, onClick, type } = props;
  return (
    <AvatarWrapper onClick={onClick}>
      <Avatar alt="Create safe" src={createIcon} className={classes[type]} />
    </AvatarWrapper>
  );
};

FloatingButtonComponent.propTypes = {
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
  onClick: PropTypes.func.isRequired,
  type: PropTypes.string,
};

FloatingButtonComponent.defaultProps = {
  type: '',
};

export default withStyles(styles)(FloatingButtonComponent);
