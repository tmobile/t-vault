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
import TypeAheadComponent from '../../../../../components/TypeAheadComponent';
import RadioButtonComponent from '../../../../../components/FormFields/RadioButton';

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

const TypeAheadWrap = styled.div`
  width: 100%;
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
  display: flex;
  .MuiTextField-root {
    width: 100%;
  }
`;

const EndingBox = styled.div`
  background-color: ${(props) =>
    props.applicationName === false
      ? 'rgba(0, 0, 0, 0.12)'
      : props.theme.customColor.primary.backgroundColor};
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

const Value = styled.p``;

const notifyAutoLoaderStyle = css`
  position: absolute;
  top: 1rem;
  right: 4rem;
`;

const SearchInputFieldLabelWrapper = styled.div`
  margin-bottom: 2rem;
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
  const [notifyEmailStatus, setNotifyEmailStatus] = useState({
    status: 'available',
  });
  const [notifyOptions, setNotifyOptions] = useState([]);
  const [showPreviewData, setShowPreviewData] = useState(false);
  const [notifyAutoLoader, setNotifyAutoLoader] = useState(false);
  const [isValidEmail, setIsValidEmail] = useState(false);
  const [isValidNotifyEmail, setIsValidNotifyEmail] = useState(false);
  const [emailError, setEmailError] = useState(false);
  const [disabledTransfer, setDisabledTransfer] = useState(true);
  const [certOwnerNTId, setCertOwnerNTId] = useState('');
  const [searchBy, setSearchBy] = useState('User');
  const [ownerSelected, setOwnerselected] = useState({});
  const [applicationNameError, setApplicationNameError] = useState(false);
  const [notifyUserSelected, setNotifyUserselected] = useState({});
  const [selfserviceAppName, setSelfserviceAppName] = useState([]);

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

  useEffect(() => {
    if (owner?.length > 2 && ownerSelected?.userEmail) {
      if (!autoLoader) {
        if (ownerSelected?.userEmail.toLowerCase() !== owner) {
          setIsValidEmail(false);
        } else {
          setIsValidEmail(true);
        }
      }
    }
  }, [owner, ownerSelected, autoLoader, options]);

  useEffect(() => {
    if (!notifyAutoLoader && notifyEmail?.length > 2) {
      if (notifyUserSelected?.userEmail && searchBy !== 'GroupEmail') {
        if (notifyUserSelected?.userEmail.toLowerCase() !== notifyEmail) {
          setIsValidNotifyEmail(false);
          setEmailErrorMsg('Please enter a user or not available!');
        } else {
          setIsValidNotifyEmail(true);
        }
      } else if (
        searchBy === 'GroupEmail' &&
        !notifyOptions?.find(
          (item) => item?.toLowerCase() === notifyEmail?.toLowerCase()
        )
      ) {
        setIsValidNotifyEmail(false);
        setEmailErrorMsg('Please enter a valid group email or not available!');
      } else {
        setIsValidNotifyEmail(true);
      }
    }
  }, [
    notifyEmail,
    notifyUserSelected,
    notifyOptions,
    notifyAutoLoader,
    searchBy,
  ]);

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
      setApplicationNameError(true);
      setNotifyEmailStatus({ status: 'available' });
      setNotificationEmailList([]);
      setSearchBy('User');
    } else {
      setApplicationNameError(false);
    }
  }, [allApplication, applicationName, selfserviceAppName]);

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

  const onPreviewClicked = () => {
    setShowPreviewData(true);
  };

  const callSearchByGroupemailApi = useCallback(
    debounce(
      (value) => {
        setNotifyAutoLoader(true);
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
              setNotifyOptions([...array]);
            }
            setNotifyAutoLoader(false);
          })
          .catch(() => {
            setNotifyAutoLoader(false);
          });
      },
      1000,
      true
    ),
    []
  );

  const callSearchApi = useCallback(
    debounce(
      (value, type) => {
        const userNameSearch = apiService.getUserName(value);
        const emailSearch = apiService.getOwnerTransferEmail(value);
        Promise.all([userNameSearch, emailSearch])
          .then((responses) => {
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
            if (type === 'applicationOwner') {
              setAutoLoader(false);
              setOptions([...array]);
            } else {
              setNotifyAutoLoader(false);
              setNotifyOptions([...array]);
            }
          })
          .catch(() => {
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
    if (e && e?.target?.value !== undefined) {
      setOwner(e?.target?.value);
      if (e?.target?.value && e?.target?.value?.length > 2) {
        setOptions([]);
        setAutoLoader(true);
        callSearchApi(e.target.value, 'applicationOwner');
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
      const applicationOwnerEmail = val?.split(', ')[0];
      setOwnerselected(
        options.filter(
          (i) => i?.userEmail?.toLowerCase() === applicationOwnerEmail
        )[0]
      );
      setEmailError(false);
      setOwner(applicationOwnerEmail);
      const obj = options.find(
        (item) =>
          item?.userEmail?.toLowerCase() ===
          applicationOwnerEmail?.toLowerCase()
      );
      if (obj && obj.userName) {
        setCertOwnerNTId(obj.userName);
      }
      const existsOwner = notificationEmailList.find(
        (item) => item.toLowerCase() === applicationOwnerEmail?.toLowerCase()
      );
      if (!existsOwner) {
        setNotificationEmailList((prev) => [...prev, applicationOwnerEmail]);
      }
      setEmailError(false);
    }
  };

  const onNotifyEmailSelected = (e, val) => {
    if (val) {
      const notifyUserEmail = val?.split(', ')[0];
      setNotifyUserselected(
        notifyOptions.filter(
          (i) => i?.userEmail?.toLowerCase() === notifyUserEmail
        )[0]
      );
      setNotifyEmail(notifyUserEmail);
      setNotifyEmailError(false);
    }
  };

  const onChangeAppilcationName = (value) => {
    setApplicationName(value);
  };

  const onSelectedApplicationName = (e, appName) => {
    setApplicationName(appName);
    setNotificationEmailList([]);
    const selectedApp = allApplication.find((item) => appName === item.appName);
    if (selectedApp !== undefined) {
      setNotifyEmailStatus({ status: 'searching' });
      apiService
        .getNotificationEmails(selectedApp?.appID)
        .then((res) => {
          if (res?.data?.spec) {
            const array = [];
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
            const obj = array.find(
              (item) => item.toLowerCase() === owner.toLowerCase()
            );
            if (owner !== '' && !obj) {
              array.push(owner);
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

  const onAddEmailClicked = () => {
    const obj = notificationEmailList.find(
      (item) => item.toLowerCase() === notifyEmail.toLowerCase()
    );
    if (!notifyEmailError && isValidNotifyEmail && validateEmail(notifyEmail)) {
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
      e.preventDefault();
      if (validateEmail(notifyEmail)) {
        onAddEmailClicked();
      } else {
        setIsValidNotifyEmail(false);
      }
    }
  };

  const onNotifyEmailChange = (e) => {
    if (e && e?.target?.value !== undefined) {
      setNotifyEmail(e?.target?.value);
      if (e.target.value && e.target.value?.length > 2) {
        setNotifyOptions([]);
        setNotifyAutoLoader(true);
        if (searchBy === 'GroupEmail') {
          callSearchByGroupemailApi(e.target.value);
        } else {
          callSearchApi(e.target.value, 'notifyUser');
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
                  onboard
                />
              </PreviewWrap>
              <OnboardFormWrap showPreviewData={showPreviewData}>
                <EachValueWrap>
                  <Label>Certificate Name:</Label>
                  <Value>{certificateData.certificateName}</Value>
                </EachValueWrap>
                <InputFieldLabelWrapper postion>
                  <InputLabel>
                    Owner
                    <RequiredCircle margin="0.5rem" />
                  </InputLabel>
                  <TypeAheadComponent
                    options={options.map(
                      (item) =>
                        `${item?.userEmail?.toLowerCase()}, ${getName(
                          item?.displayName?.toLowerCase()
                        )}, ${item?.userName?.toLowerCase()}`
                    )}
                    loader={autoLoader}
                    userInput={owner}
                    icon="search"
                    name="owner"
                    onSelected={(e, val) => onSelected(e, val)}
                    onChange={(e) => onOwnerChange(e)}
                    placeholder="Search by NTID, Email or Name "
                    error={owner?.length > 2 && (emailError || !isValidEmail)}
                    helperText={
                      owner?.length > 2 && (emailError || !isValidEmail)
                        ? 'Please enter a valid value or not available!'
                        : ''
                    }
                  />
                  {autoLoader && (
                    <LoaderSpinner customStyle={autoLoaderStyle} />
                  )}
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
                    onChange={(e) => onChangeAppilcationName(e?.target?.value)}
                    onSelected={(event, value) =>
                      onSelectedApplicationName(event, value)
                    }
                    placeholder="Search for Application Name"
                    error={applicationNameError}
                    helperText={
                      applicationNameError
                        ? `Application ${applicationName} does not exist!`
                        : ''
                    }
                  />
                </InputFieldLabelWrapper>
                <NotificationEmailsWrap>
                  {notifyEmailStatus.status === 'available' && (
                    <>
                      <SearchInputFieldLabelWrapper>
                        <InputLabel>Search By:</InputLabel>
                        <RadioButtonComponent
                          menu={['User', 'GroupEmail']}
                          handleChange={(e) => {
                            setSearchBy(e.target.value);
                            setNotifyOptions([]);
                          }}
                          value={searchBy}
                        />
                      </SearchInputFieldLabelWrapper>
                      <InputLabel>
                        Add Users to Notify
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
                              ? notifyOptions
                              : notifyOptions.map(
                                  (item) =>
                                    `${item?.userEmail?.toLowerCase()}, ${getName(
                                      item?.displayName?.toLowerCase()
                                    )}, ${item?.userName?.toLowerCase()}`
                                )
                          }
                          loader={notifyAutoLoader}
                          userInput={notifyEmail}
                          disabled={
                            applicationName === '' &&
                            notificationEmailList.length === 0
                          }
                          icon="search"
                          name="notifyEmail"
                          onSelected={(e, val) => onNotifyEmailSelected(e, val)}
                          onKeyDownClick={(e) => onEmailKeyDownClicked(e)}
                          onChange={(e) => onNotifyEmailChange(e)}
                          placeholder={
                            searchBy === 'GroupEmail'
                              ? 'Search by GroupEmail'
                              : 'Search by NTID, Email or Name '
                          }
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
                      </TypeAheadWrap>
                      <EndingBox
                        width="4rem"
                        applicationName={
                          notificationEmailList.length > 0 ||
                          applicationName !== ''
                        }
                      >
                        <ReturnIcon onClick={() => onAddEmailClicked()}>
                          <KeyboardReturnIcon />
                        </ReturnIcon>
                      </EndingBox>
                    </AutoInputFieldLabelWrapper>
                  </NotificationAutoWrap>
                )}
                {notificationEmailList.length > 0 && (
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
