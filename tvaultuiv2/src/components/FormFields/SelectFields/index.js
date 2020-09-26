import React from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import Select from '@material-ui/core/Select';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const SelectStyle = styled(Select)``;

const MenuItemList = styled(MenuItem)`
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
      MenuProps={{ classes: { paper: classes.dropdownStyle } }}
    >
      {menu.map((item) => {
        return (
          <MenuItemList value={item} selected={value} key={item}>
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
