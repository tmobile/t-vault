import React from 'react';
import Fab from '@material-ui/core/Fab';
import Icon from '@material-ui/core/Icon';
import PropTypes from 'prop-types';

const setIcon = (props) => {
  const { icon } = props;
  return <Icon>{icon}</Icon>;
};

const FloatingActionButtonComponent = (props) => {
  const { href, size, disabled, color, icon } = props;
  return (
    <Fab
      color={color || 'default'}
      aria-label={icon}
      href={href}
      size={size || 'small'}
      disabled={disabled || false}
    >
      {setIcon({ ...props })}
    </Fab>
  );
};

FloatingActionButtonComponent.propTypes = {
  href: PropTypes.string,
  size: PropTypes.string,
  disabled: PropTypes.bool,
  color: PropTypes.string,
  icon: PropTypes.string.isRequired,
};

FloatingActionButtonComponent.defaultProps = {
  href: '',
  size: 'small',
  disabled: false,
  color: 'default',
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
};

export default FloatingActionButtonComponent;
