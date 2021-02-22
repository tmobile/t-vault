/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { TextField } from '@material-ui/core';
import Autocomplete from '@material-ui/lab/Autocomplete';
import InputAdornment from '@material-ui/core/InputAdornment';
import Icon from '@material-ui/core/Icon';
import styled from 'styled-components';
import PropTypes from 'prop-types';

const AutoCompleteField = styled(Autocomplete)`
  .MuiInputAdornment-filled.MuiInputAdornment-positionStart:not(.MuiInputAdornment-hiddenLabel) {
    margin-top: 0;
  }
  .MuiAutocomplete-inputRoot[class*='MuiFilledInput-root']
    .MuiAutocomplete-input {
    padding-left: 0;
  }
  .MuiAutocomplete-inputRoot[class*='MuiFilledInput-root'] {
    padding-left: 1.5rem;
  }
  .MuiFilledInput-root.Mui-focused {
    background-color: #fff;
  }
  .MuiInputAdornment-positionStart {
    margin-right: ${(props) => (props.icon ? '8px' : '0px')};
  }
  .MuiFormHelperText-root.Mui-error {
    font-size: 1.2rem;
    margin: 1rem 0 0;
  }
`;
const setIcon = (props) => {
  const { classes, icon } = props;
  return <Icon className={classes.icon}>{icon}</Icon>;
};

const AutoCompleteComponent = (props) => {
  const {
    options,
    onChange,
    classes,
    icon,
    onSelected,
    placeholder,
    searchValue,
    error,
    onInputBlur,
    name,
    helperText,
    onKeyDown,
    disabled,
  } = props;

  return (
    <AutoCompleteField
      icon={icon}
      options={options}
      getOptionLabel={(option) => option}
      forcePopupIcon={false}
      className={classes || ''}
      onChange={onSelected}
      disabled={disabled}
      inputValue={searchValue === null ? '' : searchValue}
      onInputChange={(e) => onChange(e)}
      renderInput={(params) => (
        <TextField
          {...params}
          variant="filled"
          placeholder={placeholder}
          fullWidth
          required
          onBlur={onInputBlur}
          name={name}
          error={error}
          helperText={helperText}
          onKeyDown={onKeyDown}
          InputProps={{
            ...params.InputProps,
            startAdornment: (
              <InputAdornment position="start">
                {icon && setIcon({ ...props })}
              </InputAdornment>
            ),
          }}
        />
      )}
    />
  );
};

AutoCompleteComponent.propTypes = {
  options: PropTypes.arrayOf(PropTypes.any).isRequired,
  onChange: PropTypes.func.isRequired,
  classes: PropTypes.objectOf(PropTypes.any),
  onSelected: PropTypes.func.isRequired,
  icon: PropTypes.string,
  placeholder: PropTypes.string,
  searchValue: PropTypes.string,
  onInputBlur: PropTypes.func,
  helperText: PropTypes.string,
  error: PropTypes.bool,
  name: PropTypes.string,
  onKeyDown: PropTypes.func,
  disabled: PropTypes.bool,
};

AutoCompleteComponent.defaultProps = {
  icon: '',
  classes: {},
  placeholder: '',
  searchValue: '',
  helperText: '',
  name: '',
  error: false,
  onInputBlur: () => {},
  onKeyDown: () => {},
  disabled: false,
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default AutoCompleteComponent;
