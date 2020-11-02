/* eslint-disable no-console */
import React, { useState, useCallback, useEffect, useReducer } from 'react';
import PropTypes from 'prop-types';
import { debounce } from 'lodash';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import styled, { css } from 'styled-components';
import {
  Backdrop,
  InputLabel,
  Modal,
  Fade,
  Typography,
  Tooltip,
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import { useHistory } from 'react-router-dom';
import { useStateValue } from '../../../../contexts/globalState';
import SwitchComponent from '../../../../components/FormFields/SwitchComponent';
import TextFieldComponent from '../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../components/FormFields/ActionButton';
import ServiceAcoountHelp from '../components/ServiceAccountHelp';
import mediaBreakpoints from '../../../../breakpoints';
import leftArrowIcon from '../../../../assets/left-arrow.svg';
import {
  TitleTwo,
  TitleThree,
  RequiredCircle,
} from '../../../../styles/GlobalStyles';
import { customColor } from '../../../../theme';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../apiService';
import AutoCompleteComponent from '../../../../components/FormFields/AutoComplete';
import LoaderSpinner from '../../../../components/Loaders/LoaderSpinner';
import SnackbarComponent from '../../../../components/Snackbar';
import ConfirmationModal from '../../../../components/ConfirmationModal';
import BackdropLoader from '../../../../components/Loaders/BackdropLoader';
import svcHeaderBgimg from '../../../../assets/icon-service-account.svg';
// eslint-disable-next-line import/named
import { formatSecondsToTime } from '../../../../services/helper-function';

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
  position: relative;
  background-color: ${(props) => props.theme.palette.background.modal};
  padding: 5.5rem 6rem 6rem 6rem;
  border: none;
  outline: none;
  width: 69.6rem;
  margin: auto 0;
  display: ${(props) => (props.isShowOnBoardModal ? 'none' : 'flex')};
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
  ${mediaBreakpoints.small} {
    width: 100%;
  }
  ${(props) => props.customCss}
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
  display: flex;
  flex-direction: column;
`;
const HeaderInfoWrapper = styled.div`
  display: flex;
  align-items: center;
`;

const InfoLine = styled('p')`
  color: ${(props) => props.theme.customColor.collapse.color};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
`;

const InfoContainer = styled.div`
  padding: 1rem 0;
`;
const Span = styled('span')`
  color: ${(props) => props.theme.customColor.collapse.title};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
  ${(props) => props.extraStyles}
`;
const CollapsibleContainer = styled('div')``;
const AcionButtons = styled.div`
  display: flex;
  justify-content: flex-end;
`;
const ActionButtonWrap = styled.div`
  display: flex;
`;
const BtnWrap = styled.div`
  margin-right: 0.8rem;
`;
const OwnerActionsWrap = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-left: 0.8rem;
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

const ViewMoreStyles = css`
  display: flex;
  align-items: center;
  font-weight: 600;
  cursor: pointer;
  margin-left: 5rem;
`;
const SvcDetailsStyles = css`
  display: flex;
  align-items: center;
  font-weight: 600;
  cursor: pointer;
`;
const useStylesBootstrap = makeStyles((theme) => ({
  tooltip: {
    fontSize: theme.typography.subtitle2.fontSize,
  },
}));

// Render component goes here
const OnBoardForm = (props) => {
  const { refresh } = props;
  const [timeError, setTimeError] = useState(null);
  const [svcPasswordDetails, setSvcPasswordDetails] = useState(null);
  const [isAppNameFetchig, setIsAppNameFetching] = useState(false);
  const [isServiceFetching, setIsServiceFetching] = useState(false);
  const [isActivateSvc, setIsActivateSvc] = useState(false);
  const [isActiveServiceAccount, setIsActiveServiceAccount] = useState(true);

  const [onBoardConfirmationModal, setOnBoardConfirmationModal] = useState(
    false
  );
  const [status, setStatus] = useState({});

  const [postOnBoardModal, setPostOnBoardModal] = useState(false);
  const [isAutoExpand, setIsAutoExpand] = useState(false);
  const [serviceAccountsList, setServiceAccountsList] = useState([]);
  const [applicationList, setApplicationList] = useState([]);

  const [open, setOpen] = useState(true);
  const [isSwitchOn, setIsSwitchOn] = useState(false);

  const history = useHistory();
  const classes = useStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const [userState] = useStateValue();
  const initialState = {
    inputServiceName: '',
    inputAdGroupName: '',
    inputApplicationName: '',
    inputExpiryTime: '',
    selectedApplication: {},
    serviceAccountDetails: {},
  };
  // eslint-disable-next-line consistent-return
  const reducer = (state, { type, payload, field, value }) => {
    switch (type) {
      case 'INPUT_FORM_FIELDS':
        return {
          ...state,
          [field]: value,
        };
      case 'UPDATE_FORM_FIELDS':
        return {
          ...state,
          ...payload,
        };
      default:
        break;
    }
  };
  const [state, dispatch] = useReducer(reducer, initialState);
  const onChange = (e) => {
    // setInputServiceName(name);
    dispatch({
      type: 'INPUT_FORM_FIELDS',
      field: e?.target?.name,
      value: e?.target?.value,
    });
  };

  const {
    inputApplicationName,
    inputExpiryTime,
    inputAdGroupName,
    inputServiceName,
    serviceAccountDetails,
    selectedApplication,
  } = state;
  const tooltipStyles = useStylesBootstrap();
  /**
   * Fetch all available application names by approles for the associated user
   * @param {String} name value of the application name  to search
   */

  const fetchAppRoles = useCallback(
    debounce(
      () => {
        setIsAppNameFetching(true);
        apiService
          .getAppRoles()
          .then((res) => {
            setIsServiceFetching(false);
            setIsAppNameFetching(false);
            if (res?.data?.length > 0) {
              const array = res.data.filter((item) => item.appName);
              setApplicationList([...array]);
            }
          })
          .catch((err) => {
            setIsAppNameFetching(false);
            console.log('error fetching list ---- ', err);
          });
      },
      1000,
      true
    ),
    []
  );

  /**
   * Fetch all available service accounts of the associated user
   * @param {String} Name of the service account to fetch
   */

  const fetchServiceAccounts = useCallback(
    debounce(
      (name) => {
        setIsServiceFetching(true);
        apiService
          .getUsersServiceAccounts(name)
          .then((res) => {
            setIsServiceFetching(false);
            if (res?.data?.data?.values?.length > 0) {
              const array = res.data.data.values.filter((item) => item.userId);
              setServiceAccountsList([...array]);
            }
          })
          .catch((err) => {
            console.log('error fetching list ---- ', err);
            setIsServiceFetching(false);
          });
      },
      1000,
      true
    ),
    []
  );
  /**
   * fetch/update service account details after it has been onboarded
   */
  const updateServiceAccountDetails = useCallback(async (name) => {
    const fetchServiceAccountDetails = await apiService.fetchServiceAccountDetails(
      name
    );
    const callServiceAccount = await apiService.callServiceAccount(name);
    const updateMetaPath = await apiService.updateMetaPath(name);
    const allApiResponse = Promise.all([
      fetchServiceAccountDetails,
      callServiceAccount,
      updateMetaPath,
    ]);
    allApiResponse
      .then((res) => {
        setStatus({});
        setIsAutoExpand(true);
        setIsActiveServiceAccount(res[2]?.data?.data?.initialPasswordReset);
        setIsSwitchOn(
          res[1]?.data?.ttl <= res[0]?.data?.data?.values[0]?.maxPwdAge
        );
        dispatch({
          type: 'UPDATE_FORM_FIELDS',
          payload: {
            inputServiceName: res[0]?.data?.data?.values[0]?.userId,
            inputAdGroupName: res[2]?.data?.data?.adGroup,
            inputApplicationName: `${res[2]?.data?.data?.appName} (AppId:${res[2]?.data?.data?.appID},AppTag:${res[2]?.data?.data?.appTag})`,
            inputExpiryTime: res[1]?.data?.ttl,
            selectedApplication: res[2]?.data?.data,
            serviceAccountDetails: res[0]?.data?.data?.values[0],
          },
        });
      })
      .catch((err) => {
        setStatus({
          status: 'failed',
          message: err?.response?.data?.errors[0],
        });
      });
  }, []);
  // Close on board from functionality
  const handleClose = () => {
    setOpen(false);
    history.goBack();
  };
  /**
   *
   * @param {*} e
   * @param {*} val
   */
  const onBoardServiceAccount = () => {
    setStatus({ status: 'loading', message: 'loading...' });
    setOnBoardConfirmationModal(false);
    const payload = {
      adGroup: inputAdGroupName,
      appID: selectedApplication.appID,
      appName: selectedApplication.appName,
      appTag: selectedApplication.appTag,
      autoRotate: isSwitchOn,
      max_ttl: serviceAccountDetails?.maxPwdAge,
      name: inputServiceName,
      ttl: inputExpiryTime || serviceAccountDetails?.maxPwdAge,
    };
    apiService
      .onBoardServiceAccount(payload)
      .then(async (res) => {
        setStatus({
          status: 'success',
          message: res.data.messages[0],
        });
        setPostOnBoardModal(true);
        updateServiceAccountDetails(inputServiceName);
        await refresh();
      })
      .catch((err) => {
        setStatus({
          status: 'failed',
          message: err?.response?.data?.errors[0],
        });
      });
  };

  /**
   * @function updateServiceAccount
   * To update the service account details
   */

  const updateServiceAccount = () => {
    setStatus({ status: 'loading', message: 'loading...' });
    setOnBoardConfirmationModal(false);
    const payload = {
      adGroup: inputAdGroupName,
      appID: selectedApplication.appID,
      appName: selectedApplication.appName,
      appTag: selectedApplication.appTag,
      autoRotate: isSwitchOn,
      max_ttl: serviceAccountDetails?.maxPwdAge,
      name: inputServiceName,
      ttl: inputExpiryTime || serviceAccountDetails?.maxPwdAge,
    };
    apiService
      .updateServiceAccount(payload)
      .then(async (res) => {
        setStatus({
          status: 'success',
          message: res?.data?.messages[0],
        });
        setPostOnBoardModal(true);
        updateServiceAccountDetails(inputServiceName);
        await refresh();
      })
      .catch((err) => {
        setStatus({
          status: 'failed',
          message: err?.response?.data?.errors[0],
        });
      });
  };

  /**
   *
   * @param {object} svcDetails
   * to copy the service account password to clipboard
   */
  const copyPassword = () => {
    setStatus({ status: 'success', message: 'Password copied to clip-board' });
  };
  const onServiceAccountSelected = (e, val) => {
    const svcObj = serviceAccountsList.find((item) => item.userId === val);
    setIsAutoExpand(true);
    dispatch({
      type: 'UPDATE_FORM_FIELDS',
      payload: {
        inputServiceName: val,
        serviceAccountDetails: { ...svcObj },
      },
    });
    if (svcObj?.accountStatus.toLowerCase() === 'expired') {
      setStatus({
        status: 'failed',
        message: 'Expired service accounts cannot be onboarded',
      });
    }
  };
  const onApplicationNameSelected = (e, val) => {
    const selectedApplicationClone = applicationList?.find((item) =>
      item.appName.includes(val?.split('(')[0].trim())
    );
    dispatch({
      type: 'UPDATE_FORM_FIELDS',
      payload: {
        inputApplicationName: val,
        selectedApplication: { ...selectedApplicationClone },
      },
    });
  };

  const onServiceAccountNameChange = (e) => {
    fetchServiceAccounts(e.target.value);
    onChange(e);
  };
  const onApplicationNameChange = (e) => {
    // fetchAppRoles();
    onChange(e);
  };
  /**
   *@function validateTime
   * @param {string} value value of input time in seconds
   */
  const validateTime = (value) => {
    setTimeError(value.match(/^[a-zA-Z]*$/g));
  };
  const onExpiryTimeChange = (e) => {
    setTimeError(null);
    validateTime(e.target.value);
    onChange(e);
  };
  useEffect(() => {
    if (
      history?.location?.pathname ===
        '/service-accounts/edit-service-accounts' &&
      history?.location?.state
    ) {
      setStatus({ status: 'loading' });
      updateServiceAccountDetails(
        history.location.state.serviceAccountDetails.name
      );
    }
    fetchAppRoles();
  }, [history, updateServiceAccountDetails, fetchAppRoles]);

  const handleSwitch = (e) => {
    setIsSwitchOn(e.target.checked);
  };
  const handleCancelClick = () => {
    handleClose();
  };
  const handleSaveClick = (e) => {
    e.preventDefault();
    setOnBoardConfirmationModal(true);
  };
  const handleConfirmationModalClose = () => {
    setOnBoardConfirmationModal(false);
    setIsActivateSvc(false);
  };
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setStatus({});
  };
  /**
   *Handle confirmation modal after service account onboarding
   */
  const handlePostOnboardModalClose = () => {
    setPostOnBoardModal(false);
    handleClose();
  };
  /**
   * @function activateServiceAccount
   * @description Activates service accounts onboarded by admin
   */

  const activateServiceAccount = () => {
    setIsActivateSvc(true);
  };

  const onActivateServiceAccount = (svcName) => {
    setIsActivateSvc(false);
    setStatus({ status: 'loading', message: 'loading' });
    apiService
      .activateServiceAccount(svcName)
      .then(async (res) => {
        setStatus({
          status: 'success',
          message: 'Service Account Activated Successfully',
        });
        setSvcPasswordDetails(res?.data);
        setIsActiveServiceAccount(true);
        setPostOnBoardModal(true);
        await refresh();
      })
      .catch((err) => {
        setStatus({
          status: 'failed',
          message: err?.response?.data?.errors[0],
        });
      });
  };

  const getDisabledStatus = () => {
    return (
      timeError ||
      !inputServiceName ||
      !inputApplicationName ||
      serviceAccountDetails?.accountStatus.toLowerCase() === 'expired'
    );
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
      <div>
        {status.status === 'loading' && <BackdropLoader />}
        <ConfirmationModal
          open={onBoardConfirmationModal || isActivateSvc}
          handleClose={handleConfirmationModalClose}
          title={
            // eslint-disable-next-line no-nested-ternary
            isActivateSvc
              ? 'Activating Service Account'
              : history?.location?.pathname.includes('/edit-service-accounts')
              ? 'Updating Service Account'
              : 'Onboarding Service Account'
          }
          description={
            isActivateSvc
              ? "During the activation, the password of the service account will be reset to ensure Active Directory and T-Vault are in sync. If you want to continue with activation now please click the 'ACTIVATE' button below and make sure to update any services depending on the service account with its new password."
              : `The password for this service account will expire in ${
                  formatSecondsToTime(
                    inputExpiryTime || serviceAccountDetails?.maxPwdAge
                  ) || '365 days'
                } and will not be enabled for auto rotation by T-Vault. You need to makes sure the passwod for this service account is getting roated appropriately.`
          }
          cancelButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleConfirmationModalClose()}
              width={isMobileScreen ? '45%' : ''}
            />
          }
          confirmButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label={
                // eslint-disable-next-line no-nested-ternary
                isActivateSvc
                  ? 'Activate'
                  : history?.location?.pathname.includes(
                      '/edit-service-accounts'
                    )
                  ? 'Update'
                  : 'onboard'
              }
              color="secondary"
              onClick={
                // eslint-disable-next-line no-nested-ternary
                isActivateSvc
                  ? () => onActivateServiceAccount(inputServiceName)
                  : history?.location?.pathname.includes(
                      '/edit-service-accounts'
                    )
                  ? () => updateServiceAccount()
                  : () => onBoardServiceAccount()
              }
              width={isMobileScreen ? '45%' : ''}
            />
          }
        />
        <ConfirmationModal
          open={postOnBoardModal}
          handleClose={() => setPostOnBoardModal(false)}
          title={
            // eslint-disable-next-line no-nested-ternary
            svcPasswordDetails
              ? 'Activation Successfull'
              : history?.location?.pathname.includes('/edit-service-accounts')
              ? 'Update Successfull'
              : 'Onboarding Successfull'
          }
          description={
            // eslint-disable-next-line no-nested-ternary
            svcPasswordDetails
              ? `<p>Service account <strong>${svcPasswordDetails?.username}</strong> has been activated successfully!</br></br>
               Please click "Copy Password" button to copy the password and update the dependent services. You may also want to assign permissions for other users or groups to view or modify this service account. Please do so by visiting the "Permission" tab on the right screen.</p>`
              : history?.location?.pathname.includes('/edit-service-accounts')
              ? 'Password rotation configuration for the service account has been updated successfully.'
              : `<p> Onboarding
                of service account has been completed successfully. To continue, the service account needs to be activated by ${userState?.userEmail}. If you are owner of the service account, you need to log out and login again to activate it.</p>`
          }
          cancelButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label="CLOSE"
              color="secondary"
              onClick={() => handlePostOnboardModalClose()}
              width={isMobileScreen ? '100%' : ''}
            />
          }
          confirmButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            svcPasswordDetails ? (
              <CopyToClipboard text={svcPasswordDetails?.current_password}>
                <ButtonComponent
                  label="Copy Password"
                  color="secondary"
                  onClick={() => copyPassword()}
                  width={isMobileScreen ? '45%' : ''}
                />
              </CopyToClipboard>
            ) : (
              <></>
            )
          }
        />
        <Modal
          aria-labelledby="transition-modal-title"
          aria-describedby="transition-modal-description"
          className={classes.modal}
          open={open}
          onClose={handleClose}
          closeAfterTransition
          BackdropComponent={Backdrop}
          BackdropProps={{
            timeout: 500,
          }}
        >
          <Fade in={open}>
            <Container
              isShowOnBoardModal={onBoardConfirmationModal || postOnBoardModal}
            >
              <HeaderWrapper>
                <LeftIcon
                  src={leftArrowIcon}
                  alt="go-back"
                  onClick={() => handleClose()}
                />
                <Typography variant="h5">
                  {history?.location?.pathname.includes(
                    '/edit-service-accounts'
                  )
                    ? 'Edit Create Safe'
                    : 'Onboard Service Account'}
                </Typography>
              </HeaderWrapper>
              <InfoContainer>
                <HeaderInfoWrapper>
                  <SvcIcon alt="safe-icon" src={svcHeaderBgimg} />
                  <InfoLine>
                    T-Vault can be used to manage the life cycle of Corporate
                    (CORP) active directory service accounts, for features like
                    password resets and expiry.
                  </InfoLine>
                </HeaderInfoWrapper>
                <ServiceAcoountHelp
                  titleMore="View More"
                  titleLess="View Less"
                  collapseStyles="background:none"
                  titleCss={ViewMoreStyles}
                >
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
                <Span extraStyles="align-self:flex-end">
                  <RequiredCircle />
                  <Span extraStyles="margin-left:0.4rem">required</Span>
                </Span>
                <InputFieldLabelWrapper>
                  <InputLabel>
                    Service Account Name
                    <RequiredCircle />
                  </InputLabel>
                  <AutoCompleteComponent
                    options={[
                      ...serviceAccountsList.map((item) => item.userId),
                    ]}
                    icon="search"
                    classes={classes}
                    name="inputServiceName"
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
                    <ServiceAcoountHelp
                      titleMore="Service Account Details"
                      isAutoExpand={isAutoExpand}
                      setIsAutoExpand={setIsAutoExpand}
                      titleCss={SvcDetailsStyles}
                    >
                      <Grid container>
                        <GridColumn customStyles={GridColumnStyles}>
                          <GridItem>
                            <CollapseTitle color={customColor.collapse.title}>
                              Owner (Managed By):
                            </CollapseTitle>
                            <CollapseTitle color="#fff">
                              {serviceAccountDetails?.managedBy?.displayName}
                            </CollapseTitle>
                          </GridItem>
                          <GridItem>
                            {' '}
                            <CollapseTitle>Date Created in AD</CollapseTitle>
                            <CollapseTitle color="#fff">
                              {serviceAccountDetails?.creationDate}
                            </CollapseTitle>
                          </GridItem>
                          <GridItem>
                            {' '}
                            <CollapseTitle>Account Expiry</CollapseTitle>
                            <CollapseTitle color="#fff">
                              {serviceAccountDetails?.accountExpiresFormatted}
                            </CollapseTitle>
                          </GridItem>
                          <GridItem>
                            <CollapseTitle>Lock Status</CollapseTitle>
                            <CollapseTitle color="#fff">
                              {serviceAccountDetails?.lockStatus}
                            </CollapseTitle>
                          </GridItem>
                        </GridColumn>
                        <GridColumn customStyles={GridColumnStyles}>
                          <GridItem>
                            {' '}
                            <CollapseTitle>Owner Email</CollapseTitle>
                            <CollapseTitle color="#fff">
                              {serviceAccountDetails?.managedBy?.userEmail}
                            </CollapseTitle>
                          </GridItem>
                          <GridItem>
                            {' '}
                            <CollapseTitle>Password Expiry</CollapseTitle>
                            <CollapseTitle color="#fff">
                              {serviceAccountDetails?.passwordExpiry}
                            </CollapseTitle>
                          </GridItem>
                          <GridItem>
                            {' '}
                            <CollapseTitle>Account Status</CollapseTitle>
                            <CollapseTitle color="#fff">
                              {serviceAccountDetails?.accountStatus}
                            </CollapseTitle>
                          </GridItem>
                        </GridColumn>
                      </Grid>
                    </ServiceAcoountHelp>
                  </ServiceAccountDetailWrap>
                </InputFieldLabelWrapper>
                <ToggleWrap>
                  <TitleTwo extraCss="display:flex;align-items:center">
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
                  <InputFieldLabelWrapper customCss="width:50%">
                    <Tooltip
                      classes={tooltipStyles}
                      title="This value needs to be between 1 and 31536000.
                      15min=900s, 1h=3600s,1d=86400s,90d=7776000s,365d=31536000s. Once the TTL is passed, password will be rotated the next time it is requested"
                      placement="top"
                      arrow
                    >
                      <InputLabel>Password Expiration Time</InputLabel>
                    </Tooltip>

                    <TextFieldComponent
                      placeholder={
                        serviceAccountDetails?.maxPwdAge === 7776000
                          ? 'TTL in seconds(max: 7776000)'
                          : 'TTL in seconds(max: 31536000)'
                      }
                      icon="search"
                      name="inputExpiryTime"
                      readOnly={!isSwitchOn}
                      fullWidth
                      onChange={(val, e) => onExpiryTimeChange(val, e)}
                      value={inputExpiryTime || ''}
                      error={timeError}
                      helperText={
                        timeError
                          ? 'Please enter valid expiry time in seconds'
                          : 'Enter your custom password expiration time here. Once the expiration time has passed, the password will be rotated the next time it is requested.'
                      }
                    />
                  </InputFieldLabelWrapper>
                </ToggleWrap>
                <InputFieldLabelWrapper>
                  <InputLabel>AD Group Name</InputLabel>
                  <TextFieldComponent
                    placeholder="AD Group Name"
                    name="inputAdGroupName"
                    fullWidth
                    onChange={(val, e) => onChange(val, e)}
                    value={inputAdGroupName || ''}
                    helperText="Please provide the AD group for which read or reset permission to be granted later"
                  />
                </InputFieldLabelWrapper>
                <InputFieldLabelWrapper>
                  <InputLabel>
                    Application Name
                    <RequiredCircle />
                  </InputLabel>
                  <AutoCompleteComponent
                    options={[
                      ...applicationList.map(
                        (item) =>
                          `${item.appName} (AppId: ${item.appID},AppTag:${item.appTag})`
                      ),
                    ]}
                    icon="search"
                    name="inputApplicationName"
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
                  <BtnWrap>
                    <ButtonComponent
                      label="Cancel"
                      color="primary"
                      onClick={() => handleCancelClick()}
                    />
                  </BtnWrap>

                  <ButtonComponent
                    label={
                      history?.location?.pathname.includes(
                        '/edit-service-accounts'
                      )
                        ? 'Update'
                        : 'Onboard'
                    }
                    disabled={getDisabledStatus()}
                    color="secondary"
                    buttonType="containedSecondary"
                    onClick={(e) => handleSaveClick(e)}
                  />
                  {userState?.username.toLowerCase() ===
                    serviceAccountDetails?.managedBy?.userId.toLowerCase() &&
                  !isActiveServiceAccount ? (
                    <OwnerActionsWrap>
                      <BtnWrap>
                        {' '}
                        <ButtonComponent
                          label="Activate service Account"
                          disabled={getDisabledStatus()}
                          color="secondary"
                          buttonType="containedSecondary"
                          onClick={() => activateServiceAccount()}
                        />
                      </BtnWrap>
                    </OwnerActionsWrap>
                  ) : (
                    <></>
                  )}
                </ActionButtonWrap>
              </AcionButtons>
            </Container>
          </Fade>
        </Modal>
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
      </div>
    </ComponentError>
  );
};
OnBoardForm.propTypes = { refresh: PropTypes.func };
OnBoardForm.defaultProps = { refresh: () => {} };
export default OnBoardForm;
