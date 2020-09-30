import React, { useState } from 'react';
import styled from 'styled-components';
import { InputLabel } from '@material-ui/core';
import SwitchComponent from '../../../../../components/FormFields/SwitchComponent';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ServiceAcoountHelp from '../ServiceAccountHelp';
import { TitleTwo } from '../../../../../styles/GlobalStyles';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';

const Container = styled('section')`
  overflow: auto;
  height: 100%;
  position: relative;
`;
const InputFieldLabelWrapper = styled('div')`
  margin: 2rem 0 1.2rem;
`;
const ToggleWrap = styled('div')`
  display: flex;
  align-items: center;
  color: ${(props) => props.theme.customColor.collapse.color};
`;
const OnBoardFormContainer = styled('div')`
  padding: 2.4rem 4rem;
`;
const InfoLine = styled('p')`
  color: ${(props) => props.theme.customColor.collapse.color};
  span {
    color: ${(props) => props.theme.customColor.secondary.color};
    fontsize: ${(props) => props.theme.typography.body2.fontSze};
  }
`;
const Asteristick = styled.span``;
const InfoContainer = styled.div`
  padding: 1rem 4rem;
`;
const Span = styled('span')`
  color: ${(props) => props.theme.customColor.secondary.color};
  fontsize: ${(props) => props.theme.typography.body2.fontSze};
`;
const CollapsibleContainer = styled('div')``;
const AcionButtons = styled.div`
  display: flex;
  justify-content: flex-end;
  position: sticky;
  bottom: 0;
  padding: 1.7rem 4rem;
  background: ${(props) => props.theme.customColor.secondary.backgroundColor};
`;
const ActionButtonWrap = styled.div`
  display: flex;
`;
const CancelButton = styled.div`
  margin-right: 0.8rem;
`;

// Render component goes here
const OnBoardForm = () => {
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
  const handleSwitch = (e) => {
    setIsSwitchOn(e.target.checked);
  };
  const handleCancelClick = () => {};
  const handleSaveClick = () => {};
  return (
    <ComponentError>
      <Container>
        <InfoContainer>
          <InfoLine>
            T-Vault can be used to manage the life cycle of Corporate (CORP)
            active directory service accounts, for features like password resets
            and expiry.
          </InfoLine>
          <ServiceAcoountHelp title="view more">
            <CollapsibleContainer>
              <InfoLine>
                <Span>
                  <strong>On-Boarding:</strong>
                </Span>
                This step links a service account from active directory to be
                self-service managed into T-Vault. This is a one-time operation.
              </InfoLine>

              <InfoLine>
                <Span>
                  <strong>Service Account Activation:</strong>
                </Span>
                In order for you to activate the account, the service account
                owner needs to rotate the account password once after
                on-boarding to sync with the Active Directory.
              </InfoLine>

              <InfoLine>
                <Span>
                  <strong>Granting Permissions:</strong>
                </Span>
                When a service account is activated in T-Vault, the account
                owner can grant specific permissions to other users and groups,
                allowing other read, write and reset the associated password.
              </InfoLine>

              <InfoLine>
                T-Vault will rotate the Passwords lazily based on password
                expiration time (known as TTL). Rotation only occurs when first
                requests it after the set expiray time
              </InfoLine>
            </CollapsibleContainer>
          </ServiceAcoountHelp>
        </InfoContainer>
        <OnBoardFormContainer>
          {' '}
          <InputFieldLabelWrapper>
            <InputLabel>
              Service Account Name
              <Asteristick>*</Asteristick>
            </InputLabel>
            <TextFieldComponent
              placeholder="Search"
              icon="search"
              fullWidth
              onChange={(e) => onServiceAccountNameChange(e.target.value)}
              value={inputSearchValue || ''}
            />
            <ServiceAcoountHelp />
          </InputFieldLabelWrapper>
          <ToggleWrap>
            <TitleTwo extraCss="display:flex;justify-content:space-between">
              {' '}
              <SwitchComponent
                checked={isSwitchOn}
                handleChange={handleSwitch}
                name="rotate password"
              />
              <TitleTwo>Enable Auto Password Rotation</TitleTwo>
            </TitleTwo>
            <InputFieldLabelWrapper>
              <InputLabel>Password Expiration Time</InputLabel>
              <TextFieldComponent
                placeholder="AD Group Name"
                icon="search"
                fullWidth
                onChange={(e) => onADGroupChange(e.target.value)}
                value={inputSearchValue || ''}
                helperText="Enter the date you would like your password to expire. "
              />
            </InputFieldLabelWrapper>
          </ToggleWrap>
          <InputFieldLabelWrapper>
            <InputLabel>
              AD Group Name
              <Asteristick>*</Asteristick>
            </InputLabel>
            <TextFieldComponent
              placeholder="AD Group Name"
              icon="search"
              fullWidth
              onChange={(e) => onADGroupChange(e.target.value)}
              value={inputSearchValue || ''}
              helperText="Please provide the AD group for which read or reset permission to be granted later"
            />
          </InputFieldLabelWrapper>
          <InputFieldLabelWrapper>
            <InputLabel>
              Application Name
              <Asteristick>*</Asteristick>
            </InputLabel>
            <TextFieldComponent
              placeholder="Application Name"
              icon="search"
              fullWidth
              onChange={(e) => onApplicationNameChange(e.target.value)}
              value={inputSearchValue || ''}
              helperText="Please choose the application name to associate with this service account. Search application from below autocomplete box"
            />
          </InputFieldLabelWrapper>
        </OnBoardFormContainer>
        <AcionButtons>
          <ActionButtonWrap>
            <CancelButton>
              <ButtonComponent
                label="Cancel"
                color="primary"
                onClick={() => handleCancelClick(false)}
              />
            </CancelButton>
            <ButtonComponent
              label="Onboard"
              color="secondary"
              buttonType="containedSecondary"
              //   disabled={!inputValue || errorMessage}
              onClick={() => handleSaveClick()}
            />
          </ActionButtonWrap>
        </AcionButtons>
      </Container>
    </ComponentError>
  );
};
OnBoardForm.propTypes = {};
OnBoardForm.defaultProps = {};
export default OnBoardForm;
