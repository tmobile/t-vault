/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useCallback, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { debounce } from 'lodash';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import KeyboardReturnIcon from '@material-ui/icons/KeyboardReturn';
import PropTypes from 'prop-types';
import { useStateValue } from '../../../../../contexts/globalState';
import removeIcon from '../../../../../assets/close.svg';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import TextFieldSelect from '../../../../../components/FormFields/TextFieldSelect';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import leftArrowIcon from '../../../../../assets/left-arrow.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import PreviewCertificate from '../../CreateCertificates/preview';
import AutoCompleteComponent from '../../../../../components/FormFields/AutoComplete';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import apiService from '../../apiService';
import { validateEmail } from '../../../../../services/helper-function';
import SnackbarComponent from '../../../../../components/Snackbar';
import {
  GlobalModalWrapper,
  RequiredCircle,
} from '../../../../../styles/GlobalStyles';

const { small } = mediaBreakpoints;

const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 3rem;
  ${small} {
    margin-top: 1rem;
  }
`;

const OnboardFormWrap = styled.form`
  display: ${(props) => (props.showPreviewData ? 'none' : 'flex')};
  flex-direction: column;
`;

const PreviewWrap = styled.div`
  display: ${(props) => (props.showPreviewData ? 'block' : 'none')};
`;

const LeftIcon = styled.img`
  display: none;
  ${small} {
    display: block;
    margin-right: 1.4rem;
    margin-top: 0.3rem;
  }
`;

const InputFieldLabelWrapper = styled.div`
  margin-bottom: 3rem;
  position: ${(props) => (props.postion ? 'relative' : '')};
  .MuiSelect-icon {
    top: auto;
    color: ${(props) => props.theme.customColor.primary.color};
  }
`;

const autoLoaderStyle = css`
  position: absolute;
  top: 3rem;
  right: 1rem;
  color: red;
`;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreviewData ? 'none' : 'flex')};
  justify-content: flex-end;
  margin-top: 4rem;
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

const FieldInstruction = styled.p`
  color: ${(props) => props.theme.customColor.label.color};
  font-size: 1.3rem;
  margin-top: ${(props) => (props.marginTop ? props.marginTop : '0')};
  margin-bottom: 0;
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

const NotificationAutoWrap = styled.div`
  display: flex;
`;

const AutoInputFieldLabelWrapper = styled.div`
  position: relative;
  width: 100%;
  display: flex%;
  .MuiAutocomplete-root {
    width: calc(100% - 4rem);
  }
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

const EachValueWrap = styled.div`
  display: flex;
  font-size: 1.4rem;
  margin: 0 0 3rem 0;
  p {
    margin: 0;
  }
`;
const Label = styled.p`
  color: ${(props) => props.theme.customColor.label.color};
  margin-right: 0.5rem !important;
`;

const Value = styled.p`
  text-transform: capitalize;
`;

const notifyAutoLoaderStyle = css`
  position: absolute;
  top: 1rem;
  right: 4rem;
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
  icon: {
    color: '#5e627c',
    fontSize: '2rem',
  },
}));

const OnboardCertificates = (props) => {
  const { onCloseModal, open, certificateData, onOboardCertClicked } = props;

  const [owner, setOwner] = useState('');
  const [options, setOptions] = useState([]);
  const [autoLoader, setAutoLoader] = useState(false);
  const classes = useStyles();
  const [responseType, setResponseType] = useState(null);
  const [allApplication, setAllApplication] = useState([]);
  const [state] = useStateValue();
  const [applicationName, setApplicationName] = useState('');
  const [notificationEmailList, setNotificationEmailList] = useState([]);
  const [notifyEmail, setNotifyEmail] = useState('');
  const [notifyEmailError, setNotifyEmailError] = useState(false);
  const [emailErrorMsg, setEmailErrorMsg] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [searchNotificationsEmail, setSearchNotificationsEmail] = useState(
    false
  );
  const [notifyOptions, setNotifyOptions] = useState([]);
  const [showPreviewData, setShowPreviewData] = useState(false);
  const [notifyAutoLoader, setNotifyAutoLoader] = useState(false);
  const [isValidEmail, setIsValidEmail] = useState(false);
  const [isValidNotifyEmail, setIsValidNotifyEmail] = useState(false);
  const [emailError, setEmailError] = useState(false);
  const [disabledTransfer, setDisabledTransfer] = useState(true);
  const [certOwnerNTId, setCertOwnerNTId] = useState('');

  useEffect(() => {
    if (owner?.length > 2) {
      if (!autoLoader) {
        if (
          options.length === 0 ||
          !options.find((item) => item.userEmail === owner)
        ) {
          setIsValidEmail(false);
        } else {
          setIsValidEmail(true);
        }
      }
    }
  }, [owner, autoLoader, options]);

  useEffect(() => {
    if (notifyEmail?.length > 2) {
      if (!notifyAutoLoader) {
        if (
          notifyOptions.length === 0 ||
          !notifyOptions.includes(notifyEmail)
        ) {
          setIsValidNotifyEmail(false);
        } else {
          setIsValidNotifyEmail(true);
        }
      }
    }
  }, [notifyEmail, notifyAutoLoader, notifyOptions]);

  useEffect(() => {
    if (
      emailError ||
      !isValidEmail ||
      applicationName === '' ||
      notificationEmailList.length === 0
    ) {
      setDisabledTransfer(true);
    } else {
      setDisabledTransfer(false);
    }
  }, [emailError, owner, isValidEmail, applicationName, notificationEmailList]);

  useEffect(() => {
    if (allApplication?.length > 0) {
      allApplication.sort((first, sec) =>
        first.appName?.localeCompare(sec.appName)
      );
    }
  }, [allApplication]);

  useEffect(() => {
    if (state) {
      if (state.applicationNameList?.length > 0) {
        if (!JSON.parse(localStorage.getItem('isAdmin'))) {
          const stringVal = localStorage.getItem('selfServiceAppNames');
          const selfserviceAppName = stringVal?.split(',');
          const array = [];
          if (selfserviceAppName.length > 0) {
            selfserviceAppName.map((item) => {
              const obj = state.applicationNameList.find(
                (ele) => item === ele.appID
              );
              return array.push(obj);
            });
            setAllApplication([...array]);
          }
        } else {
          setAllApplication([...state.applicationNameList]);
        }
      } else if (state.applicationNameList === 'error') {
        setResponseType(-1);
        setToastMessage('Error occured while fetching the application name!');
      }
    }
  }, [state]);

  const onPreviewClicked = () => {
    setShowPreviewData(true);
  };

  const callSearchApi = useCallback(
    debounce(
      (value) => {
        setAutoLoader(true);
        apiService
          .getOwnerTransferEmail(value)
          .then((res) => {
            setOptions([]);
            const array = [];
            setAutoLoader(false);
            if (res?.data?.data?.values?.length > 0) {
              res.data.data.values.map((item) => {
                if (item.userEmail) {
                  return array.push(item);
                }
                return null;
              });
              setOptions([...array]);
            }
          })
          .catch(() => {
            setAutoLoader(false);
            setResponseType(-1);
            setToastMessage('Something went wrong while fetching emails!');
          });
      },
      1000,
      true
    ),
    []
  );

  const callNotifySearchApi = useCallback(
    debounce(
      (value) => {
        setNotifyAutoLoader(true);
        setNotifyOptions([]);
        apiService
          .getOwnerTransferEmail(value)
          .then((res) => {
            const array = [];
            setNotifyAutoLoader(false);
            if (res?.data?.data?.values?.length > 0) {
              res.data.data.values.map((item) => {
                if (item.userEmail) {
                  return array.push(item.userEmail);
                }
                return null;
              });
              setNotifyOptions([...array]);
            }
          })
          .catch(() => {
            setNotifyAutoLoader(false);
            setResponseType(-1);
            setToastMessage('Something went wrong while fetching emails!');
          });
      },
      1000,
      true
    ),
    []
  );

  const onOwnerChange = (e) => {
    if (e) {
      setOwner(e.target.value);
      if (e.target.value && e.target.value?.length > 2) {
        callSearchApi(e.target.value);
        if (validateEmail(owner)) {
          setEmailError(false);
        } else {
          setEmailError(true);
        }
      }
    }
  };

  const onSelected = (e, val) => {
    if (val) {
      setOwner(val);
      const obj = options.find(
        (item) => item?.userEmail?.toLowerCase() === val?.toLowerCase()
      );
      if (obj && obj.userName) {
        setCertOwnerNTId(obj.userName);
      }
      const existsOwner = notificationEmailList.find(
        (item) => item.toLowerCase() === val?.toLowerCase()
      );
      if (!existsOwner) {
        setNotificationEmailList((prev) => [...prev, val]);
      }
      setEmailError(false);
    }
  };

  const onNotifyEmailSelected = (e, val) => {
    setNotifyEmail(val);
    setNotifyEmailError(false);
  };

  const onChangeApplicationName = (appName) => {
    setApplicationName(appName);
    setNotificationEmailList([]);
    const selectedApp = allApplication.find((item) => appName === item.appName);
    setSearchNotificationsEmail(true);
    apiService
      .getNotificationEmails(selectedApp?.appID)
      .then((res) => {
        if (res?.data?.spec) {
          const array = [];
          if (
            res.data.spec.projectLeadEmail &&
            array.indexOf(res.data.spec.projectLeadEmail?.toLowerCase()) === -1
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
        setSearchNotificationsEmail(false);
      })
      .catch(() => {
        setSearchNotificationsEmail(false);
        setResponseType(-1);
        setToastMessage('Something went wrong while fetching emails list!');
      });
  };

  const onAddEmailClicked = () => {
    const obj = notificationEmailList.find(
      (item) => item.toLowerCase() === notifyEmail.toLowerCase()
    );
    if (!notifyEmailError && isValidNotifyEmail) {
      if (!obj) {
        setNotificationEmailList((prev) => [...prev, notifyEmail]);
        setNotifyEmail('');
      } else {
        setNotifyEmailError(true);
        setEmailErrorMsg('Duplicate Email!');
      }
    }
  };

  const onEmailKeyDownClicked = (e) => {
    if (e?.keyCode === 13) {
      onAddEmailClicked();
    }
  };

  const onNotifyEmailChange = (e) => {
    if (e) {
      setNotifyEmail(e?.target?.value);
      if (e.target.value && e.target.value?.length > 2) {
        callNotifySearchApi(e.target.value);
        if (validateEmail(notifyEmail)) {
          setNotifyEmailError(false);
        } else {
          setNotifyEmailError(true);
          setEmailErrorMsg(
            'Please enter a valid email address or not available!'
          );
        }
      }
    }
  };
  const onRemoveEmailsClicked = (email) => {
    const array = notificationEmailList.filter((item) => item !== email);
    setNotificationEmailList([...array]);
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  const onCreateClicked = () => {
    const obj = allApplication.find((item) => item.appName === applicationName);
    const payload = {
      appName: obj.appID,
      certOwnerEmailId: owner,
      certOwnerNTId,
      certType: certificateData.certType.toLowerCase(),
      certificateName: certificateData.certificateName,
      dnsList: [],
      notificationEmail: notificationEmailList.toString(),
    };
    onOboardCertClicked(payload);
  };

  return (
    <ComponentError>
      <>
        <Modal
          aria-labelledby="transition-modal-title"
          aria-describedby="transition-modal-description"
          className={classes.modal}
          open={open}
          onClose={() => onCloseModal()}
          closeAfterTransition
          BackdropComponent={Backdrop}
          BackdropProps={{
            timeout: 500,
          }}
        >
          <Fade in={open}>
            <GlobalModalWrapper>
              <HeaderWrapper>
                <LeftIcon
                  src={leftArrowIcon}
                  alt="go-back"
                  onClick={() => onCloseModal()}
                />
                {!showPreviewData ? (
                  <Typography variant="h5">Onboard Certificate</Typography>
                ) : (
                  <Typography variant="h5">Certificate Preview</Typography>
                )}
              </HeaderWrapper>
              <PreviewWrap showPreviewData={showPreviewData}>
                <PreviewCertificate
                  dns={[]}
                  certificateType={certificateData.certType}
                  applicationName={applicationName}
                  certName={certificateData.certificateName}
                  owner={owner}
                  container={certificateData.containerName}
                  notificationEmails={notificationEmailList}
                  handleClose={() => onCloseModal()}
                  onEditClicked={() => setShowPreviewData(false)}
                  onCreateClicked={() => onCreateClicked()}
                />
              </PreviewWrap>
              <OnboardFormWrap showPreviewData={showPreviewData}>
                <EachValueWrap>
                  <Label>Certificate Name:</Label>
                  <Value>{certificateData.certificateName}</Value>
                </EachValueWrap>
                <InputFieldLabelWrapper postion>
                  <InputLabel>
                    Owner Email ID
                    <RequiredCircle margin="0.5rem" />
                  </InputLabel>
                  <AutoCompleteComponent
                    options={[...options.map((item) => item.userEmail)]}
                    classes={classes}
                    searchValue={owner}
                    icon="search"
                    name="owner"
                    onSelected={(e, val) => onSelected(e, val)}
                    onChange={(e) => onOwnerChange(e)}
                    placeholder="Email address- Enter min 3 characters"
                    error={owner?.length > 2 && (emailError || !isValidEmail)}
                    helperText={
                      owner?.length > 2 && (emailError || !isValidEmail)
                        ? 'Please enter a valid email address or not available!'
                        : ''
                    }
                  />
                  {autoLoader && (
                    <LoaderSpinner customStyle={autoLoaderStyle} />
                  )}
                </InputFieldLabelWrapper>
                <InputFieldLabelWrapper>
                  <InputLabel>
                    Aplication Name
                    <RequiredCircle margin="1.3rem" />
                  </InputLabel>
                  <TextFieldSelect
                    menu={[...allApplication.map((item) => item.appName)]}
                    value={applicationName}
                    classes={classes}
                    handleChange={(e) =>
                      onChangeApplicationName(e?.target?.value)
                    }
                    filledText="Select application name"
                  />
                  <FieldInstruction marginTop="1.2rem">
                    Please provide the AD group for which read or reset
                    permission to be granted later.
                  </FieldInstruction>
                </InputFieldLabelWrapper>
                <NotificationEmailsWrap>
                  {applicationName === '' && (
                    <FieldInstruction>
                      Select application name and add/update notification
                      emails.
                    </FieldInstruction>
                  )}
                  {applicationName && !searchNotificationsEmail && (
                    <InputLabel>
                      Add Emails to Notify
                      <RequiredCircle margin="1.3rem" />
                    </InputLabel>
                  )}
                  {searchNotificationsEmail && (
                    <FetchingWrap>
                      <span>Fetching notification list...</span>
                      <LoaderSpinner />
                    </FetchingWrap>
                  )}
                </NotificationEmailsWrap>
                {applicationName && !searchNotificationsEmail && (
                  <NotificationAutoWrap>
                    <AutoInputFieldLabelWrapper>
                      <AutoCompleteComponent
                        options={notifyOptions}
                        classes={classes}
                        searchValue={notifyEmail}
                        icon="search"
                        name="notifyEmail"
                        onSelected={(e, val) => onNotifyEmailSelected(e, val)}
                        onKeyDown={(e) => onEmailKeyDownClicked(e)}
                        onChange={(e) => onNotifyEmailChange(e)}
                        placeholder="Email address- Enter min 3 characters"
                        error={
                          notifyEmail?.length > 2 &&
                          (notifyEmailError || !isValidNotifyEmail)
                        }
                        helperText={
                          notifyEmail?.length > 2 &&
                          (notifyEmailError || !isValidNotifyEmail)
                            ? emailErrorMsg
                            : ''
                        }
                      />
                      {notifyAutoLoader && (
                        <LoaderSpinner customStyle={notifyAutoLoaderStyle} />
                      )}
                      <EndingBox width="4rem">
                        <ReturnIcon onClick={() => onAddEmailClicked()}>
                          <KeyboardReturnIcon />
                        </ReturnIcon>
                      </EndingBox>
                    </AutoInputFieldLabelWrapper>
                  </NotificationAutoWrap>
                )}
                {!searchNotificationsEmail &&
                  notificationEmailList.length > 0 &&
                  applicationName !== '' && (
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
              </OnboardFormWrap>
              <CancelSaveWrapper showPreviewData={showPreviewData}>
                <CancelButton>
                  <ButtonComponent
                    label="Cancel"
                    color="primary"
                    onClick={() => onCloseModal()}
                  />
                </CancelButton>
                <ButtonComponent
                  label="Preview"
                  color="secondary"
                  disabled={disabledTransfer}
                  onClick={() => onPreviewClicked()}
                />
              </CancelSaveWrapper>
            </GlobalModalWrapper>
          </Fade>
        </Modal>
        {responseType === -1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message={toastMessage || 'Something went wrong!'}
          />
        )}
      </>
    </ComponentError>
  );
};

OnboardCertificates.propTypes = {
  certificateData: PropTypes.objectOf(PropTypes.any).isRequired,
  onCloseModal: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
  onOboardCertClicked: PropTypes.func.isRequired,
};

export default OnboardCertificates;
