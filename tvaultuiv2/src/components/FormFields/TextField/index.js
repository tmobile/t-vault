/* eslint-disable react/jsx-no-duplicate-props */
import React from 'react';
import TextField from '@material-ui/core/TextField';
import InputAdornment from '@material-ui/core/InputAdornment';
import Icon from '@material-ui/core/Icon';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const StyleTextField = styled(TextField)`
  .MuiFilledInput-adornedStart {
    font-size: 1.4rem;
  }
  .MuiFormHelperText-root {
    font-size: 1.3rem;
    margin: 0.8rem 0;
  }
  .MuiInputAdornment-filled.MuiInputAdornment-positionStart:not(.MuiInputAdornment-hiddenLabel) {
    margin-top: 0;
  }
  .MuiInputAdornment-positionStart {
    margin-right: ${(props) => (props.icon ? '8px' : '0px')};
  }
  .MuiFilledInput-root.Mui-focused {
    background-color: ${(props) =>
      props.primary === 'primary' ? '#fff' : '#20232e'};
  }
  .MuiFormHelperText-root.Mui-error {
    font-size: 1.3rem;
  }
  .MuiFilledInput-multiline {
    height: auto;
    padding: 1.5rem 1.5rem;
  }
`;

const setIcon = (props) => {
  const { icon } = props;
  return <Icon>{icon}</Icon>;
};

const onInputChange = () => {};

const TextFieldComponent = (props) => {
  const {
    icon,
    placeholder,
    onChange,
    value,
    fullWidth,
    multiline,
    color,
    variant,
    error,
    rows,
    type,
    helperText,
    onInputBlur,
    name,
    readOnly,
    onKeyDown,
    characterLimit,
  } = props;

  return (
    <StyleTextField
      disabled={readOnly}
      icon={icon}
      placeholder={placeholder}
      value={value}
      name={name}
      primary={color}
      fullWidth={fullWidth}
      multiline={multiline}
      color={color}
      rows={rows}
      error={error}
      helperText={helperText}
      variant={variant || 'filled'}
      type={type}
      onBlur={onInputBlur}
      onKeyDown={onKeyDown}
      autoComplete="off"
      inputProps={{ maxLength: characterLimit, 'data-testid': `${name}` }}
      InputProps={{
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
  placeholder: PropTypes.string.isRequired,
  onChange: PropTypes.func,
  onInputBlur: PropTypes.func,
  // eslint-disable-next-line react/forbid-prop-types
  value: PropTypes.any.isRequired,
  multiline: PropTypes.bool,
  fullWidth: PropTypes.bool,
  color: PropTypes.string,
  variant: PropTypes.string,
  rows: PropTypes.number,
  type: PropTypes.string,
  helperText: PropTypes.string,
  error: PropTypes.bool,
  name: PropTypes.string,
  readOnly: PropTypes.bool,
  onKeyDown: PropTypes.func,
  characterLimit: PropTypes.number,
};

TextFieldComponent.defaultProps = {
  icon: '',
  name: '',
  multiline: false,
  fullWidth: false,
  color: 'primary',
  variant: 'filled',
  rows: 5,
  type: 'text',
  helperText: '',
  error: false,
  onInputBlur: () => {},
  readOnly: false,
  onChange: () => {},
  onKeyDown: () => {},
  characterLimit: 1000,
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
};

export default TextFieldComponent;
