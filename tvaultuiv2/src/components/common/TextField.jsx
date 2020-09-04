import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import TextField from '@material-ui/core/TextField';
import InputAdornment from '@material-ui/core/InputAdornment';
import Icon from '@material-ui/core/Icon';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const StyleTextField = styled(TextField)`
  input {
    padding: 0;
  }
`;

const styles = () => ({
  iconStyle: {
    fontSize: '1.2rem',
    fontWeight: 'bold',
  },
  input: {
    background: '#fff',
    padding: '1rem',
  },
});

const setIcon = (props) => {
  const { classes, icon } = props;
  return <Icon className={classes.iconStyle}>{icon}</Icon>;
};

const onInputChange = () => {};

const TextFieldComponent = (props) => {
  const {
    icon,
    classes,
    classApplied,
    placeholder,
    disabled,
    onChange,
  } = props;
  return (
    <StyleTextField
      placeholder={placeholder}
      disabled={disabled || false}
      InputProps={{
        disableUnderline: true,
        className: classes[classApplied],
        startAdornment: (
          <InputAdornment position="start">
            {icon && setIcon({ ...props })}
          </InputAdornment>
        ),
      }}
      onChange={onChange || ((e) => onInputChange(e))}
    />
  );
};

TextFieldComponent.propTypes = {
  icon: PropTypes.string,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
  placeholder: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  classApplied: PropTypes.string,
  disabled: PropTypes.string,
};

TextFieldComponent.defaultProps = {
  icon: '',
  classApplied: '',
  disabled: false,
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
};
export default withStyles(styles)(TextFieldComponent);
