/* eslint-disable react/no-array-index-key */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import OutsideClickHandler from 'react-outside-click-handler';
import styled from 'styled-components';
import { Link as RRDLink } from 'react-router-dom';
import Link from '@material-ui/core/Link';

const Wrapper = styled.div`
  position: relative;
  height: 7rem;
`;

const SelectField = styled.div`
  width: ${(props) => props.width};
  background-image: ${(props) =>
    props.selectedNav
      ? 'linear-gradient(to top, rgb(123, 18, 78), rgba(123, 18, 78, 0))'
      : 'transparent'};
  color: #fff;
  font-size: 1.3rem;
  font-weight: bold;
  height: 100%;
  display: flex;
  align-items: center;
  cursor: ${(props) => (!props.readOnly ? 'pointer' : '')};
  padding-right: 1.5rem;
`;
const ArrowSpan = styled.span`
  margin-top: ${(props) => (!props.showOptionList ? '1rem' : '-0.3rem')};
  margin-left: 1rem;
  border: 6px solid transparent;
  border-color: ${(props) =>
    !props.showOptionList
      ? '#fff transparent transparent transparent'
      : 'transparent transparent #fff transparent'};
`;

const SelectOption = styled.div`
  background-color: #151820;
  color: #fff;
  padding: 0.5rem;
  position: absolute;
  height: ${(props) => props.height};
  overflow-y: auto;
  width: 100%;
  z-index: 2;
  display: flex;
  flex-direction: column;
`;

const CustomSelectOtion = styled(Link)`
  padding: 0.5rem 0rem;
  margin-bottom: 1rem;
  background-image: ${(props) =>
    props.selected ? 'linear-gradient(to top, #72134b, #1d212c)' : '#fff'};
  color: #fff;
  font-size: 1.3rem;
  font-weight: bold;
  :hover {
    background-image: linear-gradient(to top, #72134b, #1d212c);
    cursor: pointer;
    text-decoration: none;
  }
  span {
    padding-left: 1.5rem;
  }
`;
const SelectedOption = styled.div`
  padding-left: 1.5rem;
  width: calc(100% - 1.2rem);
`;

const HeaderSelectComponent = (props) => {
  const {
    menu,
    onChange,
    value,
    readOnly,
    filledText,
    selectedNav,
    height,
    width,
  } = props;
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
          selectedNav={selectedNav}
          width={width}
        >
          <SelectedOption>{value === '' ? filledText : value}</SelectedOption>
          <ArrowSpan showOptionList={showOptionList} />
        </SelectField>
        {showOptionList && (
          <SelectOption>
            {menu.map((option) => {
              return (
                <CustomSelectOtion
                  key={option.label}
                  to={`/${option.path}`}
                  onClick={() => {
                    onChange(option);
                    setShowOptionList(false);
                  }}
                  selected={value === option.label}
                  height={height}
                  component={RRDLink}
                >
                  <span>{option.label}</span>
                </CustomSelectOtion>
              );
            })}
          </SelectOption>
        )}
      </Wrapper>
    </OutsideClickHandler>
  );
};

HeaderSelectComponent.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
  menu: PropTypes.arrayOf(PropTypes.any).isRequired,
  readOnly: PropTypes.bool,
  filledText: PropTypes.string,
  selectedNav: PropTypes.bool,
  height: PropTypes.string,
  width: PropTypes.string.isRequired,
};

HeaderSelectComponent.defaultProps = {
  readOnly: false,
  filledText: '',
  selectedNav: false,
  height: '7rem',
};

export default HeaderSelectComponent;
