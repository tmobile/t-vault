/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import Radio from '@material-ui/core/Radio';
import RadioGroup from '@material-ui/core/RadioGroup';
import { TextField, InputLabel } from '@material-ui/core';
import Autocomplete from '@material-ui/lab/Autocomplete';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import FormControl from '@material-ui/core/FormControl';
import styled from 'styled-components';
import ButtonComponent from 'components/FormFields/ActionButton';

const PermissionWrapper = styled.div`
  padding: 2rem;
  width: 50%;
  border: 0.1rem solid #000;
  display: flex;
  flex-direction: column;
`;

const UserHeader = styled.h1`
  margin-bottom: 1.4rem;
  margin-top: 0;
`;

const InputRadioWrapper = styled.div`
  margin-bottom: 1rem;
`;

const InputWrapper = styled.div`
  .MuiFormControl-fullWidth {
    background-color: #eee;
    padding: 0.5rem;
  }
  .MuiInput-underline:before,
  .MuiInput-underline:after,
  .MuiInput-underline:hover:not(.Mui-disabled):before {
    border-bottom: none;
  }
  .MuiSvgIcon-root {
    display: none;
  }
  .MuiFormLabel-root {
    margin-bottom: 1.2rem;
  }
`;

const CancelSaveWrapper = styled.div`
  display: flex;
  align-self: flex-end;
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
`;

const Permissions = () => {
  const [value, setValue] = useState('read');
  const [, setSearchValue] = useState('');

  const handleChange = (event) => {
    setValue(event.target.value);
  };
  const data = [{ title: 'abc@tmobile.com' }, { title: 'xyz@tmobile.com' }];

  return (
    <PermissionWrapper>
      <UserHeader>Add User</UserHeader>
      <InputRadioWrapper>
        <InputWrapper>
          <InputLabel>User Email</InputLabel>
          <Autocomplete
            id="combo-box-demo"
            options={data}
            getOptionLabel={(option) => option.title}
            renderInput={(params) => (
              <TextField
                // eslint-disable-next-line react/jsx-props-no-spreading
                {...params}
                onChange={(e) => setSearchValue(e.target.value)}
              />
            )}
          />
        </InputWrapper>
        <FormControl component="fieldset">
          <RadioGroup
            row
            aria-label="permissions"
            name="permissions1"
            value={value}
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
      </InputRadioWrapper>
      <CancelSaveWrapper>
        <CancelButton>
          <ButtonComponent label="Cancel" buttonType="containedPrimary" />
        </CancelButton>
        <ButtonComponent
          label="Create"
          icon="add"
          buttonType="containedSecondary"
        />
      </CancelSaveWrapper>
    </PermissionWrapper>
  );
};

export default Permissions;
