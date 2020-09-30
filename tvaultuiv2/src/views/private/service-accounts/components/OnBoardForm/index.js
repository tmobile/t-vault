import React, { useState } from 'react';
import styled from 'styled-components';
import { InputLabel } from '@material-ui/core';
import SwitchComponent from '../../../../../components/FormFields/SwitchComponent';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ServiceAcoountHelp from '../ServiceAccountHelp';
import { TitleTwo } from '../../../../../styles/GlobalStyles';

const Container = styled('div')``;
const InputFieldLabelWrapper = styled('div')``;
const ToggleWrap = styled('div')``;
const onBoardForm = () => {
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [isSwitchOn, setIsSwitchOn] = useState(false);
  const onServiceAccountNameChange = (name) => {
    setInputSearchValue(name);
  };
  const onADGroupChange = (name) => {
    setInputSearchValue(name);
  };
  const onApplicationNameChange = (name) => {
    setInputSearchValue(name);
  };
  const handleSwitch = (e) => {};
  return (
    <Container>
      <InputFieldLabelWrapper>
        <InputLabel>Service Account Name</InputLabel>
        <TextFieldComponent
          placeholder="Search"
          icon="search"
          fullWidth
          onChange={(e) => onServiceAccountNameChange(e.target.value)}
          value={inputSearchValue || ''}
          color="secondary"
        />
        <ServiceAcoountHelp />
      </InputFieldLabelWrapper>
      <ToggleWrap>
        <TitleTwo>
          {' '}
          <SwitchComponent
            checked={isSwitchOn}
            handleChange={handleSwitch}
            name="rotate password"
          />
          Enable Auto Password Rotation
        </TitleTwo>
        <InputFieldLabelWrapper>
          <InputLabel>Password Expiration Time</InputLabel>
          <TextFieldComponent
            placeholder="AD Group Name"
            icon="search"
            fullWidth
            onChange={(e) => onADGroupChange(e.target.value)}
            value={inputSearchValue || ''}
            color="secondary"
            helperText="Enter the date you would like your password to expire. "
          />
        </InputFieldLabelWrapper>
      </ToggleWrap>

      <InputFieldLabelWrapper>
        <InputLabel>AD Group Name</InputLabel>
        <TextFieldComponent
          placeholder="AD Group Name"
          icon="search"
          fullWidth
          onChange={(e) => onADGroupChange(e.target.value)}
          value={inputSearchValue || ''}
          color="secondary"
          helperText="Please provide the AD group for which read or reset permission to be granted later"
        />
        <ServiceAcoountHelp />
      </InputFieldLabelWrapper>
      <InputFieldLabelWrapper>
        <InputLabel>Application Name</InputLabel>
        <TextFieldComponent
          placeholder="Application Name"
          icon="search"
          fullWidth
          onChange={(e) => onApplicationNameChange(e.target.value)}
          value={inputSearchValue || ''}
          color="secondary"
        />
        <ServiceAcoountHelp />
      </InputFieldLabelWrapper>
    </Container>
  );
};
onBoardForm.propTypes = {};
onBoardForm.defaultProps = {};
export default onBoardForm;
