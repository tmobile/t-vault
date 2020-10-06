import React, { useState, useCallback, useEffect } from 'react';
import { debounce } from 'lodash';
import styled, { css } from 'styled-components';
import {
  Backdrop,
  InputLabel,
  Modal,
  Fade,
  Typography,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import useMediaQuery from '@material-ui/core/useMediaQuery';
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
import apiService from '../apiService';
import AutoCompleteComponent from '../../../../components/FormFields/AutoComplete';
import LoaderSpinner from '../../../../components/Loaders/LoaderSpinner';
import SnackbarComponent from '../../../../components/Snackbar';
import ConfirmationModal from '../../../../components/ConfirmationModal';

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
  icon: {
    color: '#5e627c',
    fontSize: '2rem',
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
  position: relative;
  .MuiInputLabel-root {
    display: flex;
    align-items: center;
  }
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
  padding: 1rem 0rem;
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
const customLoaderStyle = css`
  position: absolute;
  right: 1.2rem;
  top: 3.2rem;
  color: red;
`;
// Render component goes here
const OnBoardForm = () => {
  const [inputAdGroupName, setInputAdGroupName] = useState('');
  const [inputServiceName, setInputServiceName] = useState('');
  const [getResponseType, setGetResponseType] = useState(null);
  const [isAppNameFetchig, setIsAppNameFetching] = useState(false);

  const [onBoardConfirmationModal, setOnBoardConfirmationModal] = useState(
    false
  );
  const [status, setStatus] = useState({});

  const [isServiceFetching, setIsServiceFetching] = useState(false);
  const [postOnBoardModal, setPostOnBoardModal] = useState(false);
  const [serviceAccountsList, setServiceAccountsList] = useState([]);
  const [applicationList, setApplicationList] = useState([]);
  const [inputExpiryTime, setInputExpiryTime] = useState('');
  const [inputApplicationName, setInputApplicationName] = useState('');
  const [open, setOpen] = useState(true);
  const [isSwitchOn, setIsSwitchOn] = useState(false);

  const history = useHistory();
  const classes = useStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  // Close on board from functionality
  const handleClose = () => {
    setOpen(false);
    history.goBack();
  };
  /**
   * Fetch all available application names by approles for the associated user
   * @param {String} name value of the application name  to search
   */

  const fetchAppRoles = useCallback(
    debounce(() => {
      apiService
        .getAppRoles()
        .then((res) => {
          setGetResponseType(1);
          setIsServiceFetching(false);
          if (res?.data?.length > 0) {
            const array = [];
            res.data.map((item) => {
              if (item.appName) {
                return array.push(
                  `${item.appName} (AppId: ${item.appID},AppTag:${item.appTag})`
                );
              }
              return null;
            });
            setApplicationList([...array]);
          }
        })
        .catch((err) => {
          setGetResponseType(-1);
        });
    }),
    []
  );
  // fetch approles when page loads
  useEffect(() => {
    fetchAppRoles();
  }, [fetchAppRoles]);

  /**
   * Fetch all available service accounts of the associated user
   * @param {String} Name of the service account to fetch
   */

  const fetchServiceAccounts = useCallback(
    debounce((name) => {
      setIsServiceFetching(true);
      apiService
        .getUsersServiceAccounts(name)
        .then((res) => {
          setGetResponseType(1);
          setIsServiceFetching(false);
          if (res?.data?.data?.values?.length > 0) {
            const array = [];
            res.data.data.values.map((item) => {
              if (item.userId) {
                return array.push(item.userName);
              }
              return null;
            });
            setServiceAccountsList([...array]);
          }
        })
        .catch((err) => {
          setGetResponseType(-1);
          setIsServiceFetching(false);
        });
    }),
    []
  );

  /**
   *
   * @param {*} e
   * @param {*} val
   */
  const onBoardServiceAccount = () => {
    setStatus({ status: 'loading', message: 'loading...' });
    apiService
      .onBoardServiceAccount()
      .then((res) => {
        setStatus({
          status: 'success',
          message: res.data.messages[0],
        });
        setPostOnBoardModal(true);
      })
      .catch((err) => {
        setStatus({
          status: 'failed',
          message: err?.data?.messages[0],
        });
      });
  };

  const onServiceAccountSelected = (e, val) => {
    setInputServiceName(val);
  };
  const onApplicationNameSelected = (e, val) => {
    setInputApplicationName(val);
  };
  const onServiceAccountNameChange = (name) => {
    setInputServiceName(name);
    fetchServiceAccounts(name);
  };
  const onADGroupChange = (name) => {
    setInputAdGroupName(name);
  };
  const onApplicationNameChange = (e) => {
    setInputApplicationName(e);
  };

  const onExpiryTimeChange = (value) => {
    setInputExpiryTime(value);
  };
  const handleSwitch = (e) => {
    setIsSwitchOn(e.target.checked);
  };
  const handleCancelClick = () => {
    handleClose();
  };
  const handleSaveClick = () => {
    setOnBoardConfirmationModal(true);
  };
  const handleConfirmationModalClose = () => {
    setOnBoardConfirmationModal(false);
  };
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setStatus({});
  };
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
      <>
        <ConfirmationModal
          open={onBoardConfirmationModal}
          handleClose={handleConfirmationModalClose}
          title="Onboarding Service Account"
          description="The password for this service account will expire in 365 days and will not be enabled for auto rotation by T-Vault. You need to makes sure the passwod for this service account is getting roated appropriately."
          cancelButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => setOnBoardConfirmationModal(false)}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
          confirmButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label="onboard"
              color="secondary"
              onClick={() => onBoardServiceAccount()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
        <ConfirmationModal
          open={postOnBoardModal}
          handleClose={setPostOnBoardModal}
          title="Onboarding Successfull"
          description="The password for this service account will expire in 365 days and will not be enabled for auto rotation by T-Vault. You need to makes sure the passwod for this service account is getting roated appropriately."
          cancelButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => setOnBoardConfirmationModal(false)}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
          confirmButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label="onboard"
              color="secondary"
              onClick={() => onBoardServiceAccount()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
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
                      When a service account is activated in T-Vault, the
                      account owner can grant specific permissions to other
                      users and groups, allowing other read, write and reset the
                      associated password.
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
                  <AutoCompleteComponent
                    options={serviceAccountsList}
                    icon="search"
                    classes={classes}
                    searchValue={inputServiceName}
                    onSelected={(e, val) => onServiceAccountSelected(e, val)}
                    onChange={(e) => onServiceAccountNameChange(e)}
                    placeholder="Search for service account"
                  />
                  {isServiceFetching && (
                    <LoaderSpinner
                      customStyle={customLoaderStyle}
                      size="small"
                    />
                  )}
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
                      placeholder="TTL in seconds(max: 31536000)"
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
                  <AutoCompleteComponent
                    options={applicationList}
                    icon="search"
                    classes={classes}
                    searchValue={inputApplicationName}
                    onSelected={(e, val) => onApplicationNameSelected(e, val)}
                    onChange={(e) => onApplicationNameChange(e)}
                    placeholder="Search for Application Name"
                  />
                  {isAppNameFetchig && (
                    <LoaderSpinner
                      customStyle={customLoaderStyle}
                      size="small"
                    />
                  )}
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
              {status.status === 'failed' && (
                <SnackbarComponent
                  open
                  onClose={() => onToastClose()}
                  severity="error"
                  icon="error"
                  message={status.message || 'Something went wrong!'}
                />
              )}
              {status.status === 'success' && (
                <SnackbarComponent
                  open
                  onClose={() => onToastClose()}
                  message={status.message || 'Request Successfull'}
                />
              )}
            </Container>
          </Fade>
        </Modal>
      </>
    </ComponentError>
  );
};
OnBoardForm.propTypes = {};
OnBoardForm.defaultProps = {};
export default OnBoardForm;
