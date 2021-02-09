/* eslint-disable react/no-array-index-key */
import React from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import TextField from '@material-ui/core/TextField';
import styled from 'styled-components';
import PropTypes from 'prop-types';

const StyleTextField = styled(TextField)`
  .MuiInputLabel-filled {
    transform: translate(12px, 18px) scale(1);
    color: #000;
  }
  .MuiSelect-icon {
    color: #000;
    top: calc(50% - 7px);
  }
  .MuiFormHelperText-root {
    font-size: 1.3rem;
    margin: 0.8rem 0;
  }
  .MuiInputAdornment-filled.MuiInputAdornment-positionStart:not(.MuiInputAdornment-hiddenLabel) {
    margin-top: 0;
  }
  .MuiFilledInput-root.Mui-focused {
    background-color: ${(props) =>
      props.primary === 'primary' ? '#fff' : '#20232e'};
  }
`;

export default function TextFieldSelect(props) {
  const {
    color,
    fullWidth,
    variant,
    classes,
    menu,
    handleChange,
    value,
    filledText,
    disabled,
  } = props;

  return (
    <StyleTextField
      primary={color}
      select
      label={value === '' ? filledText : ''}
      value={value}
      className={classes.select}
      onChange={handleChange}
      InputLabelProps={{ shrink: false }}
      fullWidth={fullWidth}
      variant={variant}
      color={color}
      disabled={disabled}
      SelectProps={{
        MenuProps: {
          classes: { paper: classes.dropdownStyle },
          anchorOrigin: {
            vertical: 'bottom',
            horizontal: 'center',
          },
          transformOrigin: {
            vertical: 'top',
            horizontal: 'center',
          },
          getContentAnchorEl: null,
        },
      }}
    >
      {menu.map((item, index) => (
        <MenuItem key={index} value={item}>
          {item}
        </MenuItem>
      ))}
    </StyleTextField>
  );
}

TextFieldSelect.propTypes = {
  fullWidth: PropTypes.bool,
  color: PropTypes.string,
  variant: PropTypes.string,
  classes: PropTypes.objectOf(PropTypes.any),
  menu: PropTypes.arrayOf(PropTypes.any),
  handleChange: PropTypes.func,
  value: PropTypes.string,
  filledText: PropTypes.string,
  disabled: PropTypes.bool,
};

TextFieldSelect.defaultProps = {
  fullWidth: true,
  color: 'primary',
  variant: 'filled',
  classes: {},
  handleChange: () => {},
  menu: [],
  value: '',
  filledText: '',
  disabled: false,
};
