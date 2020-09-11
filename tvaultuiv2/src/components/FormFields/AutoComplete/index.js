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
  .MuiFilledInput-root.Mui-focused {
    background-color: #fff;
  }
`;
const setIcon = (props) => {
  const { classes, icon } = props;
  return <Icon className={classes.icon}>{icon}</Icon>;
};

const AutoCompleteComponent = (props) => {
  const { options, onChange, classes, icon, onSelected } = props;
  return (
    <AutoCompleteField
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
};

AutoCompleteComponent.defaultProps = {
  icon: '',
  classes: {},
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default AutoCompleteComponent;
