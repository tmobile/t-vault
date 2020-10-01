/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Icon from '@material-ui/core/Icon';
import PropTypes from 'prop-types';
import theme from '../../../theme';

const withStylesProps = (styles) => (Component) => (props) => {
  const Comp = withStyles(styles(props, theme))(Component);
  return <Comp {...props} />;
};
const styles = (props) => ({
  contained: {
    height: '3.6rem',
    boxShadow: 'none',
    fontSize: '1.4rem',
    width: props.width,
    [theme.breakpoints.down('xs')]: {
      height: '4.5rem',
    },
  },
  startIcon: {
    marginRight: '0.5rem',
  },
  ...props.muiButtonOverrides,
});
const setIcon = (props) => {
  const { icon, classes } = props;
  return <Icon className={classes.startIcon}>{icon}</Icon>;
};

const ButtonComponent = (props) => {
  const { icon, classes, label, onClick, size, disabled, color } = props;

  return (
    <Button
      classes={classes}
      variant="contained"
      size={size || 'small'}
      disabled={disabled || false}
      onClick={onClick}
      color={color}
    >
      {icon && setIcon({ ...props })}
      {label}
    </Button>
  );
};

ButtonComponent.propTypes = {
  icon: PropTypes.string,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
  label: PropTypes.string.isRequired,
  onClick: PropTypes.func,
  size: PropTypes.string,
  disabled: PropTypes.bool,
  color: PropTypes.string.isRequired,
};

ButtonComponent.defaultProps = {
  icon: '',
  size: 'small',
  disabled: false,
  onClick: () => {},
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default withStylesProps(styles)(ButtonComponent);
