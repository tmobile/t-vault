/* eslint-disable react/no-array-index-key */
import React from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import Select from '@material-ui/core/Select';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const SelectStyle = styled(Select)`
  text-transform: capitalize;
`;

const MenuItemList = styled(MenuItem)`
  &.Mui-selected {
    color: #fff;
  }
`;

const SelectWithCountComponent = (props) => {
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
      {menu?.map((item, index) => {
        return (
          <MenuItemList value={item.name} selected={value} key={index}>
            {item.name}
            <span>{` (${item.count})`}</span>
          </MenuItemList>
        );
      })}
    </SelectStyle>
  );
};

SelectWithCountComponent.propTypes = {
  onChange: PropTypes.func.isRequired,
  variant: PropTypes.string,
  value: PropTypes.string.isRequired,
  menu: PropTypes.arrayOf(PropTypes.any).isRequired,
  classes: PropTypes.objectOf(PropTypes.any),
  color: PropTypes.string,
  readOnly: PropTypes.bool,
};

SelectWithCountComponent.defaultProps = {
  variant: 'filled',
  classes: {},
  color: 'primary',
  readOnly: false,
};

export default SelectWithCountComponent;
