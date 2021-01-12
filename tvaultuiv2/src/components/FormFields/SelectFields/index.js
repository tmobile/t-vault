/* eslint-disable react/no-array-index-key */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import OutsideClickHandler from 'react-outside-click-handler';
import styled from 'styled-components';

const Wrapper = styled.div`
  position: relative;
`;

const SelectField = styled.div`
  height: 5rem;
  background-color: ${(props) =>
    props.readOnly ? 'rgba(0, 0, 0, 0.12)' : '#fff'};
  color: ${(props) => (props.readOnly ? 'rgba(255, 255, 255, 0.38)' : '#000')};
  display: flex;
  align-items: center;
  padding-left: 1.5rem;
  cursor: ${(props) => (!props.readOnly ? 'pointer' : '')};
  ::after {
    content: '';
    position: absolute;
    right: 1rem;
    top: ${(props) => (props.showOptionList ? '1.6rem' : '2.4rem')};
    border: 6px solid transparent;
    border-color: ${(props) =>
      !props.showOptionList
        ? '#000 transparent transparent transparent'
        : 'transparent transparent #000 transparent'};
  }
`;

const SelectOption = styled.ul`
  margin: 0.3rem 0;
  background-color: #fff;
  color: #000;
  padding: 0.5rem 0;
  list-style: none;
  position: absolute;
  height: 10rem;
  overflow-y: auto;
  width: 100%;
  z-index: 2;
  box-shadow: 0px 1px 3px;
`;

const CustomSelectOtion = styled.li`
  padding: 0.5rem 0;
  background-color: ${(props) => (props.selected ? '#e20074' : '#fff')};
  color: ${(props) => (props.selected ? '#fff' : '#000')};
  :hover {
    background-color: #e20074;
    color: #fff;
    cursor: pointer;
  }
  span {
    padding-left: 1.5rem;
  }
`;

const SelectComponent = (props) => {
  const { menu, onChange, value, readOnly, filledText } = props;
  const [showOptionList, setShowOptionList] = useState(false);

  const handleListDisplay = () => {
    if (!readOnly) {
      setShowOptionList(!showOptionList);
    }
  };

  return (
    <OutsideClickHandler onOutsideClick={() => setShowOptionList(false)}>
      <Wrapper>
        <SelectField
          readOnly={readOnly}
          showOptionList={showOptionList}
          onClick={() => handleListDisplay()}
        >
          {value === '' ? filledText : value}
        </SelectField>
        {showOptionList && (
          <SelectOption>
            {menu.map((option) => {
              return (
                <CustomSelectOtion
                  key={option}
                  onClick={() => {
                    onChange(option);
                    setShowOptionList(false);
                  }}
                  selected={value === option}
                >
                  <span>{option}</span>
                </CustomSelectOtion>
              );
            })}
          </SelectOption>
        )}
      </Wrapper>
    </OutsideClickHandler>
  );
};

SelectComponent.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
  menu: PropTypes.arrayOf(PropTypes.any).isRequired,
  readOnly: PropTypes.bool,
  filledText: PropTypes.string,
};

SelectComponent.defaultProps = {
  readOnly: false,
  filledText: '',
};

export default SelectComponent;
