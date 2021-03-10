/* eslint-disable react/no-array-index-key */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import OutsideClickHandler from 'react-outside-click-handler';
import TextFieldComponent from '../TextField';

const Wrapper = styled.div`
  position: relative;
`;

const SelectOption = styled.ul`
  margin: 0.3rem 0;
  background-color: #20232e;
  color: #fff;
  padding: 0;
  list-style: none;
  position: absolute;
  max-height: 15rem;
  overflow-y: auto;
  width: 100%;
  z-index: 2;
  box-shadow: 0px 1px 3px #000;
  overflow-x: hidden;
  ${(props) => props.extraCss}
`;

const CustomSelectOption = styled.li`
  padding: 1rem 0;
  background-image: #20232e;
  color: #fff;
  display: flex;
  flex-direction: column;
  border-bottom: 0.1rem solid #1d212c;
  :hover {
    background-image: linear-gradient(to right, #72134b, #1d212c);
    color: #fff;
    cursor: pointer;
  }
  span {
    padding-left: 1.5rem;
  }
`;

const Name = styled.span`
  font-size: 1.6rem;
`;

const Type = styled.span`
  color: #a2a1a1;
  font-size: 1.3rem;
`;

const SearchboxWithDropdown = (props) => {
  const { onSearchChange, value, menu, onChange, noResultFound } = props;
  const [showOptions, setShowOptions] = useState(false);

  useEffect(() => {
    if (menu.length > 0 && value !== '') {
      setShowOptions(true);
    } else {
      setShowOptions(false);
    }
  }, [value, menu]);
  return (
    <Wrapper>
      <OutsideClickHandler
        onOutsideClick={() => {
          setShowOptions(false);
        }}
      >
        <TextFieldComponent
          placeholder="Search - Enter min 3 characters"
          icon="search"
          fullWidth
          onChange={(e) => onSearchChange(e)}
          value={value || ''}
          color="secondary"
          characterLimit={40}
        />
        {showOptions && (
          <SelectOption>
            {menu.map((option) => {
              return (
                <CustomSelectOption
                  key={option.name + option.type}
                  selected={value === option.name}
                  onClick={() => {
                    setShowOptions(false);
                    onChange(option);
                  }}
                >
                  <Name>{option.name}</Name>
                  <Type>{option.type}</Type>
                </CustomSelectOption>
              );
            })}
          </SelectOption>
        )}
        {noResultFound !== '' && (
          <SelectOption>
            <CustomSelectOption>
              <Name>{noResultFound}</Name>
            </CustomSelectOption>
          </SelectOption>
        )}
      </OutsideClickHandler>
    </Wrapper>
  );
};

SearchboxWithDropdown.propTypes = {
  onSearchChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
  menu: PropTypes.arrayOf(PropTypes.any).isRequired,
  onChange: PropTypes.func.isRequired,
  noResultFound: PropTypes.string,
};

SearchboxWithDropdown.defaultProps = {
  noResultFound: '',
};

export default SearchboxWithDropdown;
