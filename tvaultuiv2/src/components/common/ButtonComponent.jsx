import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Icon from '@material-ui/core/Icon';
import PropTypes from 'prop-types';

const styles = () => ({
  contained: {
    marginRight: '1rem',
    padding: '0.5rem 1rem',
    boxShadow: 'none',
    color: '#fff',
    borderRadius: 'none',
  },
  iconStyle: {
    fontSize: '1.2rem',
    fontWeight: 'bold',
    marginRight: '0.5rem',
  },
  primary: {
    backgroundColor: '#fff',
    color: '#e20074',
  },
});
const setIcon = (props) => {
  const { classes, icon } = props;
  return <Icon className={classes.iconStyle}>{icon}</Icon>;
};

const onButtonClick = () => {};

const ButtonComponent = (props) => {
  const { icon, classes, label, onClick, size, disabled, classApplied } = props;
  return (
    <Button
      classes={classes}
      variant="contained"
      size={size || 'small'}
      className={classes[classApplied]}
      disabled={disabled || false}
      onClick={onClick || ((e) => onButtonClick(e))}
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
  onClick: PropTypes.func.isRequired,
  size: PropTypes.string,
  disabled: PropTypes.bool,
  classApplied: PropTypes.string,
};

ButtonComponent.defaultProps = {
  icon: '',
  size: 'small',
  disabled: false,
  classApplied: '',
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
};
export default withStyles(styles)(ButtonComponent);
