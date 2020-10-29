/* eslint-disable react/no-array-index-key */
import React from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import Select from '@material-ui/core/Select';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const SelectStyle = styled(Select)`
  text-transform: uppercase;
`;

const MenuItemList = styled(MenuItem)`
  text-transform: uppercase;
  &.Mui-selected {
    color: #fff;
  }
`;

const SelectComponent = (props) => {
  const { menu, onChange, value, variant, classes, color, readOnly } = props;

  return (
    <SelectStyle
      value={value}
      onChange={onChange}
      fullWidth
      disabled={readOnly}
      className={classes.select}
      variant={variant || 'filled'}
      color={color}
      MenuProps={{
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
      }}
    >
      {menu.map((item, index) => {
        return (
          <MenuItemList value={item} selected={value} key={index}>
            {item}
          </MenuItemList>
        );
      })}
    </SelectStyle>
  );
};

SelectComponent.propTypes = {
  onChange: PropTypes.func.isRequired,
  variant: PropTypes.string,
  value: PropTypes.string.isRequired,
  menu: PropTypes.arrayOf(PropTypes.any).isRequired,
  classes: PropTypes.objectOf(PropTypes.any),
  color: PropTypes.string,
  readOnly: PropTypes.bool,
};

SelectComponent.defaultProps = {
  variant: 'filled',
  classes: {},
  color: 'primary',
  readOnly: false,
};

export default SelectComponent;
