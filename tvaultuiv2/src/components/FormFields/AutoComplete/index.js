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
  .MuiFilledInput-root.Mui-focused {
    background-color: #fff;
  }
  .MuiInputAdornment-positionStart {
    margin-right: ${(props) => (props.icon ? '8px' : '0px')};
  }
`;
const setIcon = (props) => {
  const { classes, icon } = props;
  return <Icon className={classes.icon}>{icon}</Icon>;
};

const AutoCompleteComponent = (props) => {
  const { options, onChange, classes, icon, onSelected, placeholder } = props;
  return (
    <AutoCompleteField
      icon={icon}
      options={options}
      getOptionLabel={(option) => option}
      freeSolo
      forcePopupIcon={false}
      className={classes || ''}
      onChange={onSelected}
      onInputChange={(e) => onChange(e.target.value)}
      renderInput={(params) => (
        <TextField
          {...params}
          variant="filled"
          placeholder={placeholder}
          fullWidth
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
};

AutoCompleteComponent.defaultProps = {
  icon: '',
  classes: {},
  placeholder: '',
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default AutoCompleteComponent;
