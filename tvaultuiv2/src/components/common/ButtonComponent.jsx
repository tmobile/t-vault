import React from 'react';
import styled from 'styled-components';
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Icon from '@material-ui/core/Icon';
import PropTypes from 'prop-types';

const StyledButton = styled(Button)`
  border-radius: ${(props) => !props.borderRadius && '0'};
`;

const styles = (theme) => ({
  contained: {
    padding: '0.5rem 1rem',
    height: '3.6rem',
    boxShadow: 'none',
  },
  startIcon: {
    fontSize: '1.2rem',
    fontWeight: 'bold',
    marginRight: '0.5rem',
  },
  containedPrimary: {
    backgroundColor: '#fff',
    color: theme.palette.secondary.main,
    '&:disabled': {
      backgroundColor: '#ddd',
    },
  },
  containedSecondary: {
    backgroundColor: theme.palette.secondary.main,
    color: '#fff',
    '&:disabled': {
      backgroundColor: '#454c5e',
      color: '#fff',
    },
  },
});

const onButtonClick = () => {};

const ButtonComponent = (props) => {
  const {
    icon,
    classes,
    label,
    onClick,
    size,
    disabled,
    buttonType,
    borderRadius,
  } = props;
  return (
    <StyledButton
      borderRadius={borderRadius}
      classes={classes}
      variant="contained"
      size={size || 'small'}
      className={classes[buttonType]}
      disabled={disabled || false}
      onClick={onClick || ((e) => onButtonClick(e))}
    >
      {icon && <Icon className={classes.startIcon}>{icon}</Icon>}
      {label}
    </StyledButton>
  );
};

ButtonComponent.propTypes = {
  icon: PropTypes.string,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
  label: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  size: PropTypes.string,
  disabled: PropTypes.bool,
  buttonType: PropTypes.string,
  borderRadius: PropTypes.bool,
};

ButtonComponent.defaultProps = {
  icon: '',
  size: 'small',
  disabled: false,
  buttonType: '',
  borderRadius: false,
};

export default withStyles(styles)(ButtonComponent);
