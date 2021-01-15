import React, { useEffect, useState } from 'react';
import PropType from 'prop-types';
import styled from 'styled-components';
import OutsideClickHandler from 'react-outside-click-handler';
import TextFieldComponent from '../FormFields/TextField';

const Wrapper = styled.div`
  position: relative;
`;

const OptionList = styled.ul`
  margin: 0.3rem 0;
  background-color: #fff;
  color: #000;
  padding: 0.5rem;
  text-size: 125%;
  list-style: none;
  position: absolute;
  max-height: 10rem;
  overflow-y: auto;
  width: 100%;
  z-index: 2;
  box-shadow: 0px 1px 3px;
`;

const OptionSelected = styled.li`
  padding: 1rem;
  font-size: 110%;
  :hover {
    background-color: #fff;
    color: #e20074;
    cursor: pointer;
  }
`;

const NoOption = styled.li`
  padding: 1rem 0.5rem;
  font-size: 115%;
  background-color: '#fff';
  color: '#000';
`;

const TypeAheadComponent = ({
  options,
  onChange,
  onSelected,
  disabled,
  userInput,
  icon,
  placeholder,
  name,
  error,
  helperText,
  onInputBlur,
}) => {
  const [position, setPosition] = useState(-1);
  const [filteredList, setFilteredList] = useState([]);
  const [showOptions, setShowOptions] = useState(false);

  const handleChange = (e) => {
    onChange(e);
    setShowOptions(true);
  };

  useEffect(() => {}, [error, helperText]);
  useEffect(() => {
    const filteredOption = options.filter((option) =>
      option.toLowerCase().includes(userInput.toLowerCase())
    );
    setFilteredList(filteredOption);
  }, [options, userInput]);

  const onClick = (e) => {
    onSelected(e, e.target.innerText);
    setShowOptions(false);
  };

  const onKeyDown = (e) => {
    if (e.keyCode === 13) {
      onSelected(e, filteredList[position]);
    } else if (e.keyCode === 38) {
      if (position === 0) {
        return;
      }
      setPosition(position - 1);
    }
    // User pressed the down arrow, increment the index
    else if (e.keyCode === 40) {
      if (position - 1 === filteredList.length) {
        return;
      }
      setPosition(position + 1);
    }
    setShowOptions(true);
  };

  let optionsComponent;
  if (showOptions && userInput && options?.length) {
    if (filteredList.length) {
      optionsComponent = (
        <OptionList>
          {filteredList.map((suggestion) => {
            return (
              <OptionSelected key={suggestion} onClick={(e) => onClick(e)}>
                <span>{suggestion}</span>
              </OptionSelected>
            );
          })}
        </OptionList>
      );
    } else {
      optionsComponent = (
        <OptionList>
          <NoOption>
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
          onKeyDown={(e) => onKeyDown(e)}
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
  disabled: PropType.bool,
  onInputBlur: PropType.func,
};

TypeAheadComponent.defaultProps = {
  icon: '',
  placeholder: '',
  name: '',
  error: false,
  disabled: false,
  helperText: '',
  onInputBlur: () => {},
};
export default TypeAheadComponent;
