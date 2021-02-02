/* eslint-disable react/jsx-curly-newline */
import React, { useEffect, useState } from 'react';
import PropType from 'prop-types';
import styled from 'styled-components';
import OutsideClickHandler from 'react-outside-click-handler';
import TextFieldComponent from '../FormFields/TextField';

const Wrapper = styled.div`
  position: relative;
  display: inline-block;
  width: 100%;
`;

const OptionList = styled.ul`
  margin: 0.3rem 0;
  background-color: #fff;
  color: #000;
  padding: 0.5rem;
  list-style: none;
  position: absolute;
  bottom: ${(props) => props.bottom || null};
  max-height: ${(props) => props.maxHeight || '20rem'};
  overflow-y: auto;
  width: 100%;
  z-index: 2;
  box-shadow: 0px 1px 3px;
`;

const OptionSelected = styled.li`
  padding: ${(props) => props.padding || '1rem'};
  font-size: 110%;
  :hover {
    background-color: #fff;
    color: #e20074;
    cursor: pointer;
  }
`;

const NoOption = styled.li`
  padding: ${(props) => props.padding || '1rem'};
  font-size: 115%;
  background-color: '#fff';
  color: '#000';
`;

const TypeAheadComponent = ({
  options,
  onChange,
  onSelected,
  loader,
  disabled,
  userInput,
  icon,
  placeholder,
  onKeyDownClick,
  name,
  styling,
  error,
  helperText,
  onInputBlur,
}) => {
  const [position, setPosition] = useState(0);
  const [filteredList, setFilteredList] = useState([]);
  const [showOptions, setShowOptions] = useState(false);

  const handleChange = (e) => {
    onChange(e);
  };

  useEffect(() => {
    if (loader && userInput.length > 2) {
      setShowOptions(true);
    }
  }, [loader, userInput]);

  useEffect(() => {}, [error, helperText]);
  useEffect(() => {
    setFilteredList(options);
  }, [options, userInput]);

  const onClick = (e) => {
    onSelected(e, e.target.innerText);
    setShowOptions(false);
  };

  const onKeyDown = (e) => {
    if (onKeyDownClick) {
      onKeyDownClick(e);
      return;
    }
    setPosition(0);
    if (e.keyCode === 'Enter') {
      onSelected(e, filteredList[position]);
    } else if (e.key === 'ArrowUp') {
      if (position === 0) {
        setPosition(filteredList?.length - 1);
        return;
      }
      setPosition(position - 1);
    } else if (e.key === 'ArrowDown') {
      if (position === filteredList.length - 1) {
        setPosition(0);
        return;
      }
      setPosition(position + 1);
    }
  };

  let optionsComponent;
  if (showOptions && userInput) {
    if (filteredList.length) {
      optionsComponent = (
        <OptionList bottom={styling?.bottom} maxHeight={styling?.maxHeight}>
          {filteredList.map((suggestion, index) => {
            let style;
            // Flag the active suggestion with a class
            if (index === position) {
              style = {
                backgroundColor: '#fff',
                color: '#e20074',
                cursor: 'pointer',
              };
            }
            return (
              <OptionSelected
                padding={styling?.padding}
                style={style}
                key={suggestion}
                onClick={(e) => onClick(e)}
              >
                <span>{suggestion}</span>
              </OptionSelected>
            );
          })}
        </OptionList>
      );
    } else {
      optionsComponent = (
        <OptionList bottom={styling?.bottom} maxHeight={styling?.maxHeight}>
          <NoOption padding={styling?.padding}>
            <span>No option</span>
          </NoOption>
        </OptionList>
      );
    }
  }

  return (
    <OutsideClickHandler onOutsideClick={() => setShowOptions(false)}>
      <Wrapper>
        <TextFieldComponent
          value={userInput}
          icon={icon}
          placeholder={placeholder}
          fullWidth
          name={name}
          readOnly={disabled}
          onKeyDown={(e) =>
            onKeyDownClick !== undefined ? onKeyDownClick(e) : onKeyDown(e)
          }
          onChange={(e) => handleChange(e)}
          onInputBlur={onInputBlur}
          error={error && !showOptions}
          helperText={!showOptions && error ? helperText : ''}
        />
        {optionsComponent}
      </Wrapper>
    </OutsideClickHandler>
  );
};

TypeAheadComponent.propTypes = {
  options: PropType.arrayOf(PropType.any).isRequired,
  onChange: PropType.func.isRequired,
  onSelected: PropType.func.isRequired,
  userInput: PropType.string.isRequired,
  icon: PropType.string,
  placeholder: PropType.string,
  error: PropType.bool,
  helperText: PropType.string,
  name: PropType.string,
  styling: PropType.objectOf(PropType.any),
  disabled: PropType.bool,
  onInputBlur: PropType.func,
  onKeyDownClick: PropType.func,
  loader: PropType.bool,
};

TypeAheadComponent.defaultProps = {
  icon: '',
  placeholder: '',
  name: '',
  styling: {},
  error: false,
  disabled: false,
  helperText: '',
  onInputBlur: () => {},
  onKeyDownClick: undefined,
  loader: false,
};
export default TypeAheadComponent;
