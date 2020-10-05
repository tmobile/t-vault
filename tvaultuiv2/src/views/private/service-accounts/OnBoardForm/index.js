import React, { useState } from 'react';
import styled, { css } from 'styled-components';
import {
  Backdrop,
  InputLabel,
  Modal,
  Fade,
  Typography,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { useHistory } from 'react-router-dom';
import SwitchComponent from '../../../../components/FormFields/SwitchComponent';
import TextFieldComponent from '../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../components/FormFields/ActionButton';
import ServiceAcoountHelp from '../components/ServiceAccountHelp';
import mediaBreakpoints from '../../../../breakpoints';
import leftArrowIcon from '../../../../assets/left-arrow.svg';
import { TitleTwo, TitleThree } from '../../../../styles/GlobalStyles';
import { customColor } from '../../../../theme';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';

const useStyles = makeStyles((theme) => ({
  select: {
    '&.MuiFilledInput-root.Mui-focused': {
      backgroundColor: '#fff',
    },
  },
  dropdownStyle: {
    backgroundColor: '#fff',
  },
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflowY: 'auto',
    padding: '10rem 0',
    [theme.breakpoints.down('xs')]: {
      alignItems: 'unset',
      justifyContent: 'unset',
      padding: '0',
      height: '100%',
    },
  },
}));

const Container = styled('section')`
  overflow: auto;
  height: 100%;
  position: relative;
  background-color: #2a2e3e;
  padding: 5.5rem 6rem 6rem 6rem;
  border: none;
  outline: none;
  width: 69.6rem;
  margin: auto 0;
  display: flex;
  flex-direction: column;
  ${mediaBreakpoints.belowLarge} {
    padding: 2.7rem 5rem 3.2rem 5rem;
    width: 57.2rem;
  }
  ${mediaBreakpoints.small} {
    width: 100%;
    padding: 2rem;
    margin: 0;
    height: fit-content;
  }
`;
const InputFieldLabelWrapper = styled('div')`
  margin: 2rem 0 1.2rem;
  ${(props) => props.customCss}
  ${mediaBreakpoints.small} {
    width: 100%;
  }
`;
const ToggleWrap = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: ${(props) => props.theme.customColor.collapse.color};
  ${mediaBreakpoints.small} {
    flex-direction: column;
  }
`;
const OnBoardFormContainer = styled('div')`
  padding: 2.4rem 0rem;
`;
const HeaderInfoWrapper = styled.div`
  display: flex;
  align-items: center;
`;

const InfoLine = styled('p')`
  color: ${(props) => props.theme.customColor.collapse.color};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
`;
const Asteristick = styled.span``;
const InfoContainer = styled.div`
  padding: 1rem 4rem;
`;
const Span = styled('span')`
  color: ${(props) => props.theme.customColor.collapse.title};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
`;
const CollapsibleContainer = styled('div')``;
const AcionButtons = styled.div`
  display: flex;
  justify-content: flex-end;
`;
const ActionButtonWrap = styled.div`
  display: flex;
`;
const CancelButton = styled.div`
  margin-right: 0.8rem;
`;
const CollapseTitle = styled.div`
  color: ${(props) =>
    props.color ? props.color : props.theme.customColor.collapse.title};
  font-size: ${(props) => props.theme.customColor.collapse.fontSize};
`;
const Grid = styled.div`
  display: flex;
  justify-content: space-between;
  padding: 2rem 2.5rem;
`;
const GridColumn = styled.div`
  display: flex;
  flex-direction: column;
  align-items: left;
  text-align: left;
  ${(props) => props.customStyles}
`;
const GridItem = styled.div`
  margin-bottom: 2.4rem;
`;
const GridColumnStyles = css`
  width: 45%;
`;
const ServiceAccountDetailWrap = styled.div`
  margin-top: 1rem;
`;
const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  ${mediaBreakpoints.small} {
    margin-top: 1rem;
  }
`;
const LeftIcon = styled.img`
  display: none;
  ${mediaBreakpoints.small} {
    display: block;
    margin-right: 1.4rem;
    margin-top: 0.3rem;
  }
`;

const SvcIcon = styled.img`
  height: 5.7rem;
  width: 5rem;
  margin-right: 2rem;
`;

// Render component goes here
const OnBoardForm = () => {
  const [inputAdGroupName, setInputAdGroupName] = useState('');
  const [inputServiceName, setInputServiceName] = useState('');
  const [inputExpiryTime, setInputExpiryTime] = useState('');
  const [inputApplicationName, setInputApplicationName] = useState('');
  const [open, setOpen] = useState(true);
  const [isSwitchOn, setIsSwitchOn] = useState(false);

  const history = useHistory();
  const classes = useStyles();

  // Close on board from functionality
  const handleClose = () => {
    setOpen(false);
    history.goBack();
  };

  const onServiceAccountNameChange = (name) => {
    setInputServiceName(name);
  };
  const onADGroupChange = (name) => {
    setInputAdGroupName(name);
  };
  const onApplicationNameChange = (name) => {
    setInputApplicationName(name);
  };

  const onExpiryTimeChange = (value) => {
    setInputExpiryTime(value);
  };
  const handleSwitch = (e) => {
    setIsSwitchOn(e.target.checked);
  };
  const handleCancelClick = () => {};
  const handleSaveClick = () => {};

  // render grid row of service account details
  //   const renderGridRow = (data) => {
  //     data.map((item) => (
  //       <Grid>
  //         <div>{item.title}</div>
  //         <div>{item.info}</div>
  //       </Grid>
  //     ));
  //   };

  return (
    <ComponentError>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={open}
        onClose={() => handleClose()}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <Fade in={open}>
          <Container>
            <HeaderWrapper>
              <LeftIcon
                src={leftArrowIcon}
                alt="go-back"
                onClick={() => handleClose()}
              />
              <Typography variant="h5">Create Service Account</Typography>
            </HeaderWrapper>
            <InfoContainer>
              <HeaderInfoWrapper>
                <SvcIcon alt="safe-icon" />
                <InfoLine>
                  T-Vault can be used to manage the life cycle of Corporate
                  (CORP) active directory service accounts, for features like
                  password resets and expiry.
                </InfoLine>
              </HeaderInfoWrapper>
              <ServiceAcoountHelp title="view more">
                <CollapsibleContainer>
                  <InfoLine>
                    <Span>
                      <strong>On-Boarding:</strong>
                    </Span>
                    This step links a service account from active directory to
                    be self-service managed into T-Vault. This is a one-time
                    operation.
                  </InfoLine>

                  <InfoLine>
                    <Span>
                      <strong>Service Account Activation:</strong>
                    </Span>
                    In order for you to activate the account, the service
                    account owner needs to rotate the account password once
                    after on-boarding to sync with the Active Directory.
                  </InfoLine>

                  <InfoLine>
                    <Span>
                      <strong>Granting Permissions:</strong>
                    </Span>
                    When a service account is activated in T-Vault, the account
                    owner can grant specific permissions to other users and
                    groups, allowing other read, write and reset the associated
                    password.
                  </InfoLine>

                  <InfoLine>
                    T-Vault will rotate the Passwords lazily based on password
                    expiration time (known as TTL). Rotation only occurs when
                    first requests it after the set expiray time
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
                  value={inputServiceName || ''}
                />
                <ServiceAccountDetailWrap>
                  <ServiceAcoountHelp title="Service Account Details">
                    <Grid container>
                      <GridColumn customStyles={GridColumnStyles}>
                        <GridItem>
                          <CollapseTitle color={customColor.collapse.title}>
                            Owner (Managed By):
                          </CollapseTitle>
                          <CollapseTitle color="#fff">
                            Sivakumar Nagarajan
                          </CollapseTitle>
                        </GridItem>
                        <GridItem>
                          {' '}
                          <CollapseTitle>Date Created in AD</CollapseTitle>
                          <CollapseTitle color="#fff">
                            2020-01-13 12:40
                          </CollapseTitle>
                        </GridItem>
                        <GridItem>
                          {' '}
                          <CollapseTitle>Account Expiry</CollapseTitle>
                          <CollapseTitle color="#fff">Never</CollapseTitle>
                        </GridItem>
                        <GridItem>
                          {' '}
                          <CollapseTitle>Lock Satus</CollapseTitle>
                          <CollapseTitle color="#fff">unlocked</CollapseTitle>
                        </GridItem>
                      </GridColumn>
                      <GridColumn customStyles={GridColumnStyles}>
                        <GridItem>
                          {' '}
                          <CollapseTitle>Owner Email</CollapseTitle>
                          <CollapseTitle color="#fff">
                            Sivakumar.Nagarajan14 @T-Mobile.com
                          </CollapseTitle>
                        </GridItem>
                        <GridItem>
                          {' '}
                          <CollapseTitle>Password Expiry</CollapseTitle>
                          <CollapseTitle color="#fff">
                            2021-01-13 12:40 (365 days)
                          </CollapseTitle>
                        </GridItem>
                        <GridItem>
                          {' '}
                          <CollapseTitle>Account Satus</CollapseTitle>
                          <CollapseTitle color="#fff">active</CollapseTitle>
                        </GridItem>
                      </GridColumn>
                    </Grid>
                  </ServiceAcoountHelp>
                </ServiceAccountDetailWrap>
              </InputFieldLabelWrapper>
              <ToggleWrap>
                <TitleTwo extraCss="display:flex;justify-content:space-between;align-items:center">
                  {' '}
                  <SwitchComponent
                    checked={isSwitchOn}
                    handleChange={handleSwitch}
                    name="rotate password"
                  />
                  <TitleThree extraCss="margin-left:1rem;">
                    Enable Auto Password Rotation
                  </TitleThree>
                </TitleTwo>
                <InputFieldLabelWrapper customCss="width:60%">
                  <InputLabel>Password Expiration Time</InputLabel>
                  <TextFieldComponent
                    placeholder="AD Group Name"
                    icon="search"
                    fullWidth
                    onChange={(e) => onExpiryTimeChange(e.target.value)}
                    value={inputExpiryTime || ''}
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
                  value={inputAdGroupName || ''}
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
                  value={inputApplicationName || ''}
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
        </Fade>
      </Modal>
    </ComponentError>
  );
};
OnBoardForm.propTypes = {};
OnBoardForm.defaultProps = {};
export default OnBoardForm;
