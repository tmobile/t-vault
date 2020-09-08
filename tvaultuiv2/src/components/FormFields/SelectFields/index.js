import React from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import Select from '@material-ui/core/Select';
import PropTypes from 'prop-types';

const SelectComponent = (props) => {
  const { menu, onChange, value, variant, classes } = props;
  return (
    <Select
      value={value}
      onChange={onChange}
      fullWidth
      className={classes.select || ''}
      variant={variant || 'filled'}
    >
      {menu.map((item) => {
        return (
          <MenuItem value={item} selected={value}>
            {item}
          </MenuItem>
        );
      })}
    </Select>
  );
};

SelectComponent.propTypes = {
  onChange: PropTypes.func.isRequired,
  variant: PropTypes.string,
  value: PropTypes.string.isRequired,
  menu: PropTypes.arrayOf(PropTypes.any).isRequired,
  classes: PropTypes.objectOf(PropTypes.any),
};

SelectComponent.defaultProps = {
  variant: 'filled',
  classes: '',
};

export default SelectComponent;
