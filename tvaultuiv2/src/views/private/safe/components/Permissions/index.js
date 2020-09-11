/* eslint-disable import/no-unresolved */
import React, { useState, useEffect } from 'react';
import { debounce } from 'lodash';
import Radio from '@material-ui/core/Radio';
import { makeStyles } from '@material-ui/core/styles';
import RadioGroup from '@material-ui/core/RadioGroup';
import { InputLabel, Typography } from '@material-ui/core';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import FormControl from '@material-ui/core/FormControl';
import styled from 'styled-components';
import ButtonComponent from 'components/FormFields/ActionButton';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import AutoCompleteComponent from 'components/FormFields/AutoComplete';
import apiService from '../../apiService';
import data from './__mock__/data';

const PermissionWrapper = styled.div`
  padding: 3.5rem 4rem;
  width: 50%;
  border: 0.1rem solid #000;
  display: flex;
  flex-direction: column;
`;
const HeaderWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  div {
    display: flex;
    align-items: center;
  }
`;
const RequiredText = styled.span`
  font-size: 1.6rem;
  color: #5e627c;
  margin-left: 0.5rem;
`;

const RequiredCircle = styled.span`
  width: 0.6rem;
  height: 0.6rem;
  background-color: #e20074;
  border-radius: 50%;
  margin-left: ${(props) => props.margin || '0'};
`;

const InputWrapper = styled.div`
  margin-top: 4rem;
  margin-bottom: 2.4rem;
  .MuiInputLabel-root {
    display: flex;
    align-items: center;
  }
`;
const InstructionText = styled.p`
  margin-top: 1.4rem;
  color: #bbbbbb;
  font-size: 1.2rem;
  margin-bottom: 0rem;
`;
const RadioButtonWrapper = styled.div`
  display: flex;
  justify-content: space-between;
`;
const CancelSaveWrapper = styled.div`
  display: flex;
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
`;

const useStyles = makeStyles(() => ({
  icon: {
    color: '#5e627c',
    fontSize: '2rem',
  },
}));

const Permissions = () => {
  const classes = useStyles();
  const [radioValue, setRadioValue] = useState('read');
  const [searchValue, setSearchValue] = useState('');
  const [options, setOptions] = useState([]);
  const [disabledSave, setDisabledSave] = useState(true);

  useEffect(() => {
    if (searchValue !== '') {
      setDisabledSave(false);
    } else {
      setDisabledSave(true);
    }
  }, [searchValue]);

  const handleChange = (event) => {
    setRadioValue(event.target.value);
  };

  const callSearchApi = debounce(() => {
    apiService
      .searchUser(data)
      .then((res) => {
        setOptions([]);
        res.data.values.map((item) => {
          return setOptions((prev) => [...prev, item.userEmail]);
        });
      })
      // eslint-disable-next-line no-console
      .catch((e) => console.error(e));
  }, 1000);

  const onSearchChange = (text) => {
    setOptions([]);
    setSearchValue(text);
    if (text !== '') {
      callSearchApi();
      // on api search replace with callSearchApi(text)
    }
  };

  const onSelected = (e, val) => {
    setSearchValue(val);
  };

  return (
    <ComponentError>
      <PermissionWrapper>
        <HeaderWrapper>
          <Typography variant="h5">Add User</Typography>
          <div>
            <RequiredCircle />
            <RequiredText>Required</RequiredText>
          </div>
        </HeaderWrapper>
        <InputWrapper>
          <InputLabel>
            User Email
            <RequiredCircle margin="0.5rem" />
          </InputLabel>
          <AutoCompleteComponent
            options={options}
            icon="search"
            classes={classes}
            searchValue={searchValue}
            onSelected={(e, val) => onSelected(e, val)}
            onChange={(e) => onSearchChange(e)}
          />
          <InstructionText>
            Search the T-Mobile system to add users
          </InstructionText>
        </InputWrapper>
        <RadioButtonWrapper>
          <FormControl component="fieldset">
            <RadioGroup
              row
              aria-label="permissions"
              name="permissions1"
              value={radioValue}
              onChange={handleChange}
            >
              <FormControlLabel
                value="read"
                control={<Radio color="default" />}
                label="Read"
              />
              <FormControlLabel
                value="write"
                control={<Radio color="default" />}
                label="Write"
              />
            </RadioGroup>
          </FormControl>
          <CancelSaveWrapper>
            <CancelButton>
              <ButtonComponent label="Cancel" color="primary" />
            </CancelButton>
            <ButtonComponent
              label="Save"
              color="secondary"
              disabled={disabledSave}
            />
          </CancelSaveWrapper>
        </RadioButtonWrapper>
      </PermissionWrapper>
    </ComponentError>
  );
};

export default Permissions;
