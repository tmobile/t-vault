/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect, useCallback } from 'react';
import { debounce } from 'lodash';
import ReactHtmlParser from 'react-html-parser';
import { makeStyles } from '@material-ui/core/styles';
import { useMatomo } from '@datapunt/matomo-tracker-react';
import { useHistory } from 'react-router-dom';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import KeyboardReturnIcon from '@material-ui/icons/KeyboardReturn';
import PropTypes from 'prop-types';
import { validateEmail } from '../../../../services/helper-function';
import ConfirmationModal from '../../../../components/ConfirmationModal';
import TextFieldComponent from '../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../components/FormFields/ActionButton';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';
import leftArrowIcon from '../../../../assets/left-arrow.svg';
import removeIcon from '../../../../assets/close.svg';
import mediaBreakpoints from '../../../../breakpoints';
import SnackbarComponent from '../../../../components/Snackbar';
import LoaderSpinner from '../../../../components/Loaders/LoaderSpinner';
import BackdropLoader from '../../../../components/Loaders/BackdropLoader';
import { useStateValue } from '../../../../contexts/globalState';
import apiService from '../apiService';
import PreviewCertificate from './preview';
import SwitchComponent from '../../../../components/FormFields/SwitchComponent';
import RadioButtonComponent from '../../../../components/FormFields/RadioButton';
import CertificateHeader from '../components/CertificateHeader';
import configData from '../../../../config/config';
import {
  GlobalModalWrapper,
  RequiredCircle,
  RequiredText,
} from '../../../../styles/GlobalStyles';
import AutoCompleteComponent from '../../../../components/FormFields/AutoComplete';
import TypeAheadComponent from '../../../../components/TypeAheadComponent';

const { small } = mediaBreakpoints;

const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  ${small} {
    margin-top: 1rem;
  }
`;

const StyledModal = styled(Modal)`
  @-moz-document url-prefix() {
    .MuiBackdrop-root {
      position: absolute;
      height: 95rem;
    }
  }
`;

const LeftIcon = styled.img`
  display: none;
  ${small} {
    display: block;
    margin-right: 1.4rem;
    margin-top: 0.3rem;
  }
`;

const CreateCertificateForm = styled.form`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
  flex-direction: column;
  margin-top: 2rem;
`;

const PreviewWrap = styled.div`
  display: ${(props) => (props.showPreview ? 'block' : 'none')};
`;

const InputFieldLabelWrapper = styled.div`
  margin-bottom: 3rem;
  .MuiSelect-icon {
    top: auto;
    color: ${(props) => props.theme.customColor.primary.color};
  }
`;

const AutoInputFieldLabelWrapper = styled.div`
  position: relative;
  width: 100%;
  display: flex;
  .MuiTextField-root {
    width: 100%;
  }
`;
const ContainerOwnerWrap = styled.div`
  font-size: 1.4rem;
  margin-bottom: 1rem;
  display: ${(props) => (!props.showPreview ? 'block' : 'none')};
`;

const Container = styled.div`
  margin-bottom: 0.8rem;
`;
const Owner = styled.div``;
const Label = styled.span`
  color: ${(props) => props.theme.customColor.label.color};
  margin-right: 0.3rem;
`;

const RadioWrap = styled.div`
  margin: 0 0 3rem;
`;

const Value = styled.span``;

const FieldInstruction = styled.p`
  color: ${(props) => props.theme.customColor.label.color};
  font-size: 1.3rem;
  margin-top: 1.2rem;
  margin-bottom: 0;
`;

const ArrayList = styled.div`
  display: flex;
  flex-wrap: wrap;
  margin-top: 1rem;
`;
const EachItem = styled.div`
  background-color: #454c5e;
  padding: 1rem;
  display: flex;
  align-items: center;
  margin: 0.3rem 0.5rem 0.3rem 0;
`;

const Name = styled.span`
  font-size: 1.4rem;
`;

const RemoveIcon = styled.img`
  width: 1.5rem;
  margin-left: 1rem;
  cursor: pointer;
`;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
  justify-content: flex-end;
  margin-top: 3rem;
  ${small} {
    margin-top: 5.3rem;
  }
  button {
    ${small} {
      height: 4.5rem;
    }
  }
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${small} {
    margin-right: 1rem;
    width: 100%;
  }
`;
const InputEndWrap = styled.div`
  display: flex;
`;

const InputRequiredWrap = styled.div`
  display: flex;
  justify-content: space-between;
`;

const EndingBox = styled.div`
  background-color: ${(props) =>
    props.theme.customColor.primary.backgroundColor};
  color: ${(props) => props.theme.customColor.primary.color};
  width: ${(props) => props.width};
  display: flex;
  align-items: center;
  height: 5rem;
`;

const ReturnIcon = styled.span`
  margin-left: 1rem;
  margin-top: 0.5rem;
  cursor: pointer;
`;

const IncludeDnsWrap = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 3rem;
  label {
    margin-bottom: 0rem;
  }
  > span {
    margin-right: 1rem;
  }
`;

const NotificationEmailsWrap = styled.div``;

const FetchingWrap = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 2rem;
  span {
    margin-right: 2rem;
  }
`;

const SearchInputFieldLabelWrapper = styled.div`
  margin-bottom: 2rem;
`;

const NotificationAutoWrap = styled.div`
  display: flex;
`;

const autoLoaderStyle = css`
  position: absolute;
  top: 1rem;
  right: 4rem;
`;

const TypeAheadWrap = styled.div`
  width: 100%;
`;
const InputFieldError = styled.div`
  font-size: 1.2rem;
  margin: 1rem 0 0;
  color: #ee4e4e;
  a {
    color: #ee4e4e;
  }
`;

const useStyles = makeStyles((theme) => ({
  select: {
    '&.MuiFilledInput-root.Mui-focused': {
      backgroundColor: '#fff',
    },
  },
  dropdownStyle: {
    backgroundColor: '#fff',
    maxHeight: '20rem',
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

const CreateCertificates = (props) => {
  const { refresh } = props;
  const classes = useStyles();
  const [open, setOpen] = useState(true);
  const [applicationName, setApplicationName] = useState('');
  const [certName, setCertName] = useState('');
  const [disabledSave, setDisabledSave] = useState(true);
  const [dnsName, setDnsName] = useState('');
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [ownerEmail, setOwnerEmail] = useState('N/A');
  const [certificateType, setCertificateType] = useState('internal');
  const [dnsArray, setDnsArray] = useState([]);
  const [showPreview, setShowPreview] = useState(false);
  const [dnsError, setDnsError] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [errorDnsMessage, setErrorDnsMessage] = useState('');
  const [emailErrorMsg, setEmailErrorMsg] = useState('');
  const [certNameError, setCertNameError] = useState(false);
  const [responseDesc, setResponseDesc] = useState('');
  const [responseTitle, setResponseTitle] = useState('');
  const [autoLoader, setAutoLoader] = useState(false);
  const [allApplication, setAllApplication] = useState([]);
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [notificationEmailList, setNotificationEmailList] = useState([]);
  const [isValidEmail, setIsValidEmail] = useState(false);
  const [options, setOptions] = useState([]);
  const [notifyEmail, setNotifyEmail] = useState('');
  const [searchBy, setSearchBy] = useState('User');
  const [notifyUserSelected, setNotifyUserSelected] = useState(false);
  const [emailError, setEmailError] = useState(false);
  const [isDns, setIsDns] = useState(false);
  const [applicationNameError, setApplicationNameError] = useState(false);
  const [notifyEmailStatus, setNotifyEmailStatus] = useState({
    status: 'not-available',
  });
  const [selfserviceAppName, setSelfserviceAppName] = useState([]);
  const [applicationNameErrorMsg, setApplicationNameErrorMsg] = useState('');
  const isMobileScreen = useMediaQuery(small);
  const history = useHistory();
  const [state] = useStateValue();

  const { trackPageView, trackEvent } = useMatomo();

  useEffect(() => {
    trackPageView();
    return () => {
      trackPageView();
    };
  }, [trackPageView]);

  const handleClose = () => {
    if (responseType !== 0) {
      setOpen(false);
      history.goBack();
    }
  };
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };
  useEffect(() => {
    if (!autoLoader && notifyEmail?.length > 2) {
      if (notifyUserSelected?.userEmail && searchBy !== 'GroupEmail') {
        if (notifyEmail !== notifyUserSelected?.userEmail.toLowerCase()) {
          setIsValidEmail(false);
          setEmailErrorMsg('Please enter a valid user or not available!');
        } else {
          setIsValidEmail(true);
        }
      } else if (
        searchBy === 'GroupEmail' &&
        !options?.find(
          (item) => item?.toLowerCase() === notifyEmail?.toLowerCase()
        )
      ) {
        setIsValidEmail(false);
        setEmailErrorMsg('Please enter a valid group email or not available!');
      } else {
        setIsValidEmail(true);
      }
    }
  }, [notifyEmail, notifyUserSelected, autoLoader, options, searchBy]);

  useEffect(() => {
    if (allApplication?.length > 0) {
      allApplication.sort((first, sec) =>
        first.appName?.localeCompare(sec.appName)
      );
    }
  }, [allApplication]);

  useEffect(() => {
    setResponseType(0);
    if (state) {
      setResponseType(null);
      if (state.userEmail) {
        setOwnerEmail(state.userEmail);
      }
      if (state.applicationNameList?.length > 0) {
        if (!JSON.parse(sessionStorage.getItem('isAdmin'))) {
          const stringVal = sessionStorage.getItem('selfServiceAppNames');
          setSelfserviceAppName(stringVal?.split(','));
        }
        const array = [];
        state.applicationNameList.map((item) => {
          if (item.appID !== 'oth') {
            array.push(item);
          }
          return null;
        });
        setAllApplication([...array]);
      } else if (state.applicationNameList === 'error') {
        setResponseType(-1);
        setToastMessage('Error occured while fetching the application name!');
      }
    }
  }, [state]);

  useEffect(() => {
    if (
      certName === '' ||
      applicationName === '' ||
      dnsError ||
      certNameError ||
      ownerEmail === 'N/A' ||
      notificationEmailList.length === 0
    ) {
      setDisabledSave(true);
    } else {
      setDisabledSave(false);
    }
  }, [
    certName,
    applicationName,
    certNameError,
    dnsError,
    ownerEmail,
    notificationEmailList,
  ]);

  const InputValidation = (text) => {
    if (text) {
      const res = /^[A-Za-z0-9.-]*?[a-z0-9]$/i;
      return (
        res.test(text) &&
        /^[A-z0-9]/.test(text) &&
        /[a-z0-9]$/.test(text) &&
        !/(--)/.test(text) &&
        !/(\.\.)/.test(text)
      );
    }
    return null;
  };

  const onCertificateNameChange = (e) => {
    setCertName(e.target.value);
    const { value } = e.target;
    if (!InputValidation(e.target.value)) {
      setCertNameError(true);
      setErrorMessage(
        'Certificate name can have alphabets, numbers, . and - characters only, and it should not start or end with special characters(-.)'
      );
    } else if (value.toLowerCase().includes('.t-mobile.com')) {
      setCertNameError(true);
      setErrorMessage('Please enter certificate name without .t-mobile.com.');
    } else {
      setCertNameError(false);
      setErrorMessage('');
    }
  };

  const checkDnsAlreadyIncluded = (val) => {
    if (dnsArray.includes(val)) {
      setDnsError(true);
      setErrorDnsMessage('DNS name already added!');
    } else {
      setDnsArray((prev) => [...prev, val.toLowerCase()]);
      setDnsName('');
      setDnsError(false);
      setErrorDnsMessage('');
    }
  };

  const onAddDnsClicked = (e) => {
    if (e.keyCode === 13 && e?.target?.value && !dnsError) {
      e.preventDefault();
      const val = `${e.target.value}.t-mobile.com`;
      checkDnsAlreadyIncluded(val);
    }
  };

  const onAddDnsKeyClicked = () => {
    if (dnsName && !dnsError) {
      const val = `${dnsName}.t-mobile.com`;
      checkDnsAlreadyIncluded(val);
    }
  };

  const onDnsNameChange = (e) => {
    setDnsName(e.target.value);
    const { value } = e.target;
    if (value && !InputValidation(value)) {
      setDnsError(true);
      setErrorDnsMessage(
        'DNS can have alphabets, numbers, . and - characters only, and it should not start or end with special characters(-.)'
      );
    } else if (value && value.toLowerCase().includes('.t-mobile.com')) {
      setDnsError(true);
      setErrorDnsMessage('Please enter DNS without .t-mobile.com.');
    } else {
      setDnsError(false);
      setErrorDnsMessage('');
    }
  };

  const onRemoveClicked = (dns) => {
    const array = dnsArray.filter((item) => item !== dns);
    setDnsArray([...array]);
  };

  const onRemoveEmailsClicked = (email) => {
    const array = notificationEmailList.filter((item) => item !== email);
    setNotificationEmailList([...array]);
  };

  const onPreviewClicked = () => {
    setShowPreview(true);
  };

  const onCreateClicked = () => {
    const obj = allApplication.find((item) => item.appName === applicationName);
    const dnsList = [];
    dnsArray.map((item) => dnsList.push(item.replace('.t-mobile.com', '')));

    if (obj) {
      const payload = {
        appName: obj.appID,
        certOwnerEmailId: ownerEmail,
        certOwnerNTId: state.username,
        certType: certificateType.toLowerCase(),
        certificateName: certName,
        dnsList,
        notificationEmail: notificationEmailList.toString(),
      };
      setResponseType(0);
      apiService
        .createCertificate(payload)
        .then(async (res) => {
          setResponseType(null);
          trackEvent({
            category: 'certificate-creation',
            action: 'click-event',
          });
          if (res.data.messages && res.data.messages[0]) {
            setOpenConfirmationModal(true);
            setResponseTitle('Successful');
            setResponseDesc(res.data.messages[0]);
            await refresh();
          }
        })
        .catch((err) => {
          if (err?.response?.data?.errors && err.response.data.errors[0]) {
            const msg = err.response.data.errors[0];
            setOpenConfirmationModal(true);
            setResponseTitle('Error');
            setResponseDesc(msg.charAt(0).toUpperCase() + msg.slice(1));
          }
          setResponseType(null);
        });
    }
  };

  const getNotificationEmailData = (selectedApp) => {
    if (selectedApp !== undefined) {
      setNotifyEmailStatus({ status: 'searching' });
      apiService
        .getNotificationEmails(selectedApp?.appID)
        .then((res) => {
          if (res?.data?.spec) {
            const array = [];
            array.push(ownerEmail.toLowerCase());
            if (
              res.data.spec.projectLeadEmail &&
              array.indexOf(res.data.spec.projectLeadEmail?.toLowerCase()) ===
                -1
            ) {
              array.push(res.data.spec.projectLeadEmail.toLowerCase());
            }
            if (
              res.data.spec.opsContactEmail &&
              array.indexOf(res.data.spec.opsContactEmail?.toLowerCase()) === -1
            ) {
              array.push(res.data.spec.opsContactEmail.toLowerCase());
            }
            setNotificationEmailList([...array]);
          }
          setNotifyEmailStatus({ status: 'available' });
        })
        .catch(() => {
          setResponseType(-1);
          setToastMessage('Something went wrong while fetching emails list!');
        });
    }
  };

  const onSelectedApplicationName = (e, appName) => {
    setApplicationName(appName);
    setNotificationEmailList([]);
    const selectedApp = allApplication.find((item) => appName === item.appName);
    if (!JSON.parse(sessionStorage.getItem('isAdmin'))) {
      if (selfserviceAppName.includes(selectedApp?.appID)) {
        getNotificationEmailData(selectedApp);
      }
    } else {
      getNotificationEmailData(selectedApp);
    }
  };
  const handleCloseConfirmationModal = () => {
    setOpenConfirmationModal(false);
    handleClose();
  };

  const errorHandleClose = () => {
    setOpenConfirmationModal(false);
  };

  const onSelected = (e, val) => {
    const notifyUserEmail = val?.split(', ')[0];
    setNotifyUserSelected(
      options.filter((i) => i?.userEmail?.toLowerCase() === notifyUserEmail)[0]
    );
    setNotifyEmail(notifyUserEmail);
    setEmailError(false);
  };

  const onChangeAppilcationName = (value) => {
    setApplicationName(value);
  };

  const callSearchByGroupemailApi = useCallback(
    debounce(
      (value) => {
        setAutoLoader(true);
        apiService
          .searchByGroupEmail(value)
          .then((response) => {
            setOptions([]);
            const array = [];
            if (response?.data?.data?.values?.length > 0) {
              response.data.data.values.map((item) => {
                if (item.email) {
                  return array.push(item.email);
                }
                return null;
              });
              setOptions([...array]);
            }
            setAutoLoader(false);
          })
          .catch(() => {
            setAutoLoader(false);
          });
      },
      1000,
      true
    ),
    []
  );

  const callSearchApi = useCallback(
    debounce(
      (value) => {
        setAutoLoader(true);
        const userNameSearch = apiService.getUserName(value);
        const emailSearch = apiService.getOwnerTransferEmail(value);
        Promise.all([userNameSearch, emailSearch])
          .then((responses) => {
            setOptions([]);
            const array = new Set([]);
            if (responses[0]?.data?.data?.values?.length > 0) {
              responses[0].data.data.values.map((item) => {
                if (item.userName) {
                  return array.add(item);
                }
                return null;
              });
            }
            if (responses[1]?.data?.data?.values?.length > 0) {
              responses[1].data.data.values.map((item) => {
                if (item.userName) {
                  return array.add(item);
                }
                return null;
              });
            }
            setOptions([...array]);
            setAutoLoader(false);
          })
          .catch(() => {
            setAutoLoader(false);
          });
      },
      1000,
      true
    ),
    []
  );

  const onNotifyEmailChange = (e) => {
    if (e?.target?.value !== undefined) {
      setNotifyEmail(e.target.value);
      if (e.target.value && e.target.value?.length > 2) {
        if (searchBy === 'GroupEmail') {
          callSearchByGroupemailApi(e.target.value);
        } else {
          callSearchApi(e.target.value);
        }
      }
    }
  };

  const getName = (displayName) => {
    if (displayName?.match(/(.*)\[(.*)\]/)) {
      const lastFirstName = displayName?.match(/(.*)\[(.*)\]/)[1].split(', ');
      const name = `${lastFirstName[1]} ${lastFirstName[0]}`;
      const optionalDetail = displayName?.match(/(.*)\[(.*)\]/)[2];
      return `${name}, ${optionalDetail}`;
    }
    if (displayName?.match(/(.*), (.*)/)) {
      const lastFirstName = displayName?.split(', ');
      const name = `${lastFirstName[1]} ${lastFirstName[0]}`;
      return name;
    }
    return displayName;
  };

  const onAddEmailClicked = () => {
    const obj = notificationEmailList.find(
      (item) => item.toLowerCase() === notifyEmail.toLowerCase()
    );
    if (!emailError && isValidEmail && notifyEmail !== '') {
      if (!obj) {
        setNotificationEmailList((prev) => [...prev, notifyEmail]);
        setNotifyEmail('');
      } else {
        setEmailError(true);
        setEmailErrorMsg('Duplicate Email!');
      }
    }
  };

  const onEmailKeyDownClicked = (e) => {
    if (e?.keyCode === 13) {
      e.preventDefault();
      if (e?.keyCode === 13) {
        e.preventDefault();
        if (validateEmail(notifyEmail)) {
          onAddEmailClicked();
        } else {
          setIsValidEmail(false);
        }
      }
    }
  };

  useEffect(() => {
    const selectedApp = allApplication.find(
      (item) => applicationName === item.appName
    );
    if (
      applicationName !== '' &&
      (![...allApplication.map((item) => item.appName)].includes(
        applicationName
      ) ||
        (!JSON.parse(sessionStorage.getItem('isAdmin')) &&
          !selfserviceAppName.includes(selectedApp?.appID)))
    ) {
      if (
        !JSON.parse(sessionStorage.getItem('isAdmin')) &&
        !selfserviceAppName.includes(selectedApp?.appID)
      ) {
        setApplicationNameErrorMsg(
          'You do not have access to this group. Please go here (<a href="https://access.t-mobile.com" target="_blank">https://access.t-mobile.com</a>) to register yourself part of the group.'
        );
      } else {
        setApplicationNameErrorMsg(
          `Application ${applicationName} does not exist!`
        );
      }
      setApplicationNameError(true);
      setNotifyEmailStatus({ status: 'not-available' });
      setNotificationEmailList([]);
      setSearchBy('User');
    } else {
      setApplicationNameError(false);
    }
  }, [allApplication, applicationName, selfserviceAppName]);

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleCloseConfirmationModal}
          title={responseTitle}
          description={responseDesc}
          confirmButton={
            <ButtonComponent
              label="Close"
              color="secondary"
              onClick={() =>
                responseTitle === 'Error'
                  ? errorHandleClose()
                  : handleCloseConfirmationModal()
              }
            />
          }
        />
        {!openConfirmationModal && (
          <StyledModal
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
              <GlobalModalWrapper>
                {responseType === 0 && <BackdropLoader />}
                <HeaderWrapper>
                  <LeftIcon
                    src={leftArrowIcon}
                    alt="go-back"
                    onClick={() => handleClose()}
                  />
                  {!showPreview ? (
                    <Typography variant="h5">Create Certificate</Typography>
                  ) : (
                    <Typography variant="h5">Certificate Preview</Typography>
                  )}
                </HeaderWrapper>
                <CertificateHeader />
                <ContainerOwnerWrap showPreview={showPreview}>
                  <Container>
                    <Label>Container:</Label>
                    <Value>VenafiBin_12345</Value>
                  </Container>
                  <Owner>
                    <Label>Owner Email:</Label>
                    <Value>{ownerEmail}</Value>
                  </Owner>
                </ContainerOwnerWrap>
                <PreviewWrap showPreview={showPreview}>
                  <PreviewCertificate
                    dns={dnsArray}
                    certificateType={certificateType}
                    applicationName={applicationName}
                    owner={ownerEmail}
                    container="VenafiBin_12345"
                    certName={certName}
                    handleClose={handleClose}
                    onEditClicked={() => setShowPreview(false)}
                    onCreateClicked={() => onCreateClicked()}
                    isMobileScreen={isMobileScreen}
                    notificationEmails={notificationEmailList}
                    responseType={responseType}
                  />
                </PreviewWrap>
                <CreateCertificateForm showPreview={showPreview}>
                  <RadioWrap>
                    <InputRequiredWrap>
                      <InputLabel>
                        Certificate Type
                        <RequiredCircle margin="1.3rem" />
                      </InputLabel>
                      <div>
                        <RequiredCircle />
                        <RequiredText>Required</RequiredText>
                      </div>
                    </InputRequiredWrap>
                    <RadioButtonComponent
                      menu={
                        configData.SSL_EXT_CERTIFICATE
                          ? ['internal', 'external']
                          : ['internal']
                      }
                      handleChange={(e) => setCertificateType(e.target.value)}
                      value={certificateType}
                    />
                  </RadioWrap>
                  <InputFieldLabelWrapper>
                    <InputLabel>
                      Certificate Name
                      <RequiredCircle margin="1.3rem" />
                    </InputLabel>
                    <InputEndWrap>
                      <TextFieldComponent
                        value={certName}
                        placeholder="Enter a name here..."
                        fullWidth
                        name="certName"
                        error={certNameError}
                        helperText={certNameError ? errorMessage : ''}
                        onChange={(e) => {
                          onCertificateNameChange(e);
                        }}
                      />
                      <EndingBox width="14rem">.t-mobile.com</EndingBox>
                    </InputEndWrap>
                  </InputFieldLabelWrapper>
                  <InputFieldLabelWrapper>
                    <InputLabel>
                      Application Name
                      <RequiredCircle margin="1.3rem" />
                    </InputLabel>
                    <AutoCompleteComponent
                      icon="search"
                      options={[...allApplication.map((item) => item.appName)]}
                      searchValue={applicationName}
                      classes={classes}
                      onChange={(e) =>
                        onChangeAppilcationName(e?.target?.value)
                      }
                      onSelected={(event, value) =>
                        onSelectedApplicationName(event, value)
                      }
                      placeholder="Search for Application Name"
                    />
                    {applicationNameError && (
                      <InputFieldError>
                        {ReactHtmlParser(applicationNameErrorMsg)}
                      </InputFieldError>
                    )}
                  </InputFieldLabelWrapper>
                  <IncludeDnsWrap>
                    <SwitchComponent
                      checked={isDns}
                      handleChange={(e) => setIsDns(e.target.checked)}
                      name="dns"
                    />
                    <InputLabel>Enable Additional DNS</InputLabel>
                  </IncludeDnsWrap>
                  {isDns && (
                    <InputFieldLabelWrapper>
                      <InputLabel>Add DNS</InputLabel>
                      <InputEndWrap>
                        <TextFieldComponent
                          value={dnsName}
                          placeholder="Add DNS"
                          fullWidth
                          name="dnsName"
                          onChange={(e) => {
                            onDnsNameChange(e);
                          }}
                          error={dnsError}
                          helperText={dnsError ? errorDnsMessage : ''}
                          onKeyDown={(e) => onAddDnsClicked(e)}
                        />
                        <EndingBox width="17rem">
                          .t-mobile.com
                          <ReturnIcon onClick={() => onAddDnsKeyClicked()}>
                            <KeyboardReturnIcon />
                          </ReturnIcon>
                        </EndingBox>
                      </InputEndWrap>
                      <ArrayList>
                        {dnsArray.map((item) => {
                          return (
                            <EachItem key={item}>
                              <Name>{item}</Name>
                              <RemoveIcon
                                src={removeIcon}
                                alt="remove"
                                onClick={() => onRemoveClicked(item)}
                              />
                            </EachItem>
                          );
                        })}
                      </ArrayList>
                    </InputFieldLabelWrapper>
                  )}
                  <NotificationEmailsWrap>
                    {notifyEmailStatus.status === 'not-available' && (
                      <FieldInstruction>
                        Select application name and add/update notification
                        emails.
                      </FieldInstruction>
                    )}
                    {notifyEmailStatus.status === 'available' && (
                      <>
                        <SearchInputFieldLabelWrapper>
                          <InputLabel>Search By:</InputLabel>
                          <RadioButtonComponent
                            menu={['User', 'GroupEmail']}
                            handleChange={(e) => {
                              setSearchBy(e.target.value);
                              setOptions([]);
                            }}
                            value={searchBy}
                          />
                        </SearchInputFieldLabelWrapper>
                        <InputLabel>
                          Add User to Notify
                          <RequiredCircle margin="1.3rem" />
                        </InputLabel>
                      </>
                    )}
                    {notifyEmailStatus.status === 'searching' && (
                      <FetchingWrap>
                        <span>Fetching notification list...</span>
                        <LoaderSpinner />
                      </FetchingWrap>
                    )}
                  </NotificationEmailsWrap>
                  {notifyEmailStatus.status === 'available' && (
                    <NotificationAutoWrap>
                      <AutoInputFieldLabelWrapper>
                        <TypeAheadWrap>
                          <TypeAheadComponent
                            options={
                              searchBy === 'GroupEmail'
                                ? options
                                : options.map(
                                    (item) =>
                                      `${item?.userEmail?.toLowerCase()}, ${getName(
                                        item?.displayName?.toLowerCase()
                                      )}, ${item?.userName?.toLowerCase()}`
                                  )
                            }
                            loader={autoLoader}
                            userInput={notifyEmail}
                            icon="search"
                            name="notifyUser"
                            onSelected={(e, val) => onSelected(e, val)}
                            onKeyDownClick={(e) => onEmailKeyDownClicked(e)}
                            onChange={(e) => onNotifyEmailChange(e)}
                            placeholder={
                              searchBy === 'GroupEmail'
                                ? 'Search by GroupEmail'
                                : 'Search by NTID, Email or Name'
                            }
                            error={
                              notifyEmail?.length > 2 &&
                              (emailError || !isValidEmail)
                            }
                            helperText={
                              notifyEmail?.length > 2 &&
                              (emailError || !isValidEmail)
                                ? emailErrorMsg
                                : ''
                            }
                            styling={{ bottom: '5rem' }}
                          />
                          {autoLoader && (
                            <LoaderSpinner customStyle={autoLoaderStyle} />
                          )}
                        </TypeAheadWrap>

                        <EndingBox width="4rem">
                          <ReturnIcon onClick={() => onAddEmailClicked()}>
                            <KeyboardReturnIcon />
                          </ReturnIcon>
                        </EndingBox>
                      </AutoInputFieldLabelWrapper>
                    </NotificationAutoWrap>
                  )}
                  {notifyEmailStatus.status === 'available' &&
                    notificationEmailList.length > 0 && (
                      <ArrayList>
                        {notificationEmailList.map((item) => {
                          return (
                            <EachItem key={item}>
                              <Name>{item}</Name>
                              <RemoveIcon
                                src={removeIcon}
                                alt="remove"
                                onClick={() => onRemoveEmailsClicked(item)}
                              />
                            </EachItem>
                          );
                        })}
                      </ArrayList>
                    )}
                </CreateCertificateForm>
                <CancelSaveWrapper showPreview={showPreview}>
                  <CancelButton>
                    <ButtonComponent
                      label="Cancel"
                      color="primary"
                      onClick={() => handleClose()}
                    />
                  </CancelButton>
                  <ButtonComponent
                    label="Preview"
                    color="secondary"
                    disabled={disabledSave}
                    onClick={() => onPreviewClicked()}
                  />
                </CancelSaveWrapper>
                {responseType === -1 && (
                  <SnackbarComponent
                    open
                    onClose={() => onToastClose()}
                    severity="error"
                    icon="error"
                    message={toastMessage || 'Something went wrong!'}
                  />
                )}
              </GlobalModalWrapper>
            </Fade>
          </StyledModal>
        )}
      </>
    </ComponentError>
  );
};

CreateCertificates.propTypes = {
  refresh: PropTypes.func.isRequired,
};

export default CreateCertificates;
