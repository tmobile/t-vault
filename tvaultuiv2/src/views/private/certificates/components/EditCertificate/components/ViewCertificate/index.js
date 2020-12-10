import React, { useCallback, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { debounce } from 'lodash';
import { makeStyles } from '@material-ui/core/styles';
import styled, { css } from 'styled-components';
import { InputLabel, Typography } from '@material-ui/core';
import KeyboardReturnIcon from '@material-ui/icons/KeyboardReturn';
import { validateEmail } from '../../../../../../../services/helper-function';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import PreviewCertificate from '../../../../CreateCertificates/preview';
import mediaBreakpoints from '../../../../../../../breakpoints';
import leftArrowIcon from '../../../../../../../assets/left-arrow.svg';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import CertificateHeader from '../../../CertificateHeader';
import SnackbarComponent from '../../../../../../../components/Snackbar';
import LoaderSpinner from '../../../../../../../components/Loaders/LoaderSpinner';
import removeIcon from '../../../../../../../assets/close.svg';
import AutoCompleteComponent from '../../../../../../../components/FormFields/AutoComplete';
import {
  GlobalModalWrapper,
  RequiredCircle,
} from '../../../../../../../styles/GlobalStyles';
import apiService from '../../../../apiService';

const { small } = mediaBreakpoints;

const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  ${small} {
    margin-top: 1rem;
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

const PreviewWrap = styled.div``;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
  justify-content: flex-end;
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
const Label = styled.p`
  font-size: 1.3rem;
  color: ${(props) => props.theme.customColor.label.color};
  margin-bottom: 0.9rem;
`;

const Value = styled.p`
  font-size: 1.8rem;
  text-transform: capitalize;
`;

const EachDetail = styled.div`
  margin-bottom: 3rem;
  p {
    margin: 0;
  }
`;

const EditableFormWrap = styled.form`
  display: ${(props) => (props.showPreviewData ? 'none' : 'flex')};
  flex-direction: column;
  margin-bottom: 3rem;
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

const NotificationEmailsWrap = styled.div``;

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

const notifyAutoLoaderStyle = css`
  position: absolute;
  top: 1rem;
  right: 4rem;
`;

const useStyles = makeStyles(() => ({
  select: {
    '&.MuiFilledInput-root.Mui-focused': {
      backgroundColor: '#fff',
    },
  },
  dropdownStyle: {
    backgroundColor: '#fff',
  },
  icon: {
    color: '#5e627c',
    fontSize: '2rem',
  },
}));

const ViewCertificate = (props) => {
  const {
    onCertRenewClicked,
    certificateData,
    showRevokeRenewBtn,
    onCloseModal,
    onCertRevokeClicked,
    onUpdateCertClicked,
  } = props;
  const [applicationOwner, setApplicationOwner] = useState('');
  const [options, setOptions] = useState([]);
  const [autoLoader, setAutoLoader] = useState(false);
  const classes = useStyles();
  const [responseType, setResponseType] = useState(null);
  const [notificationEmailList, setNotificationEmailList] = useState([]);
  const [notifyEmail, setNotifyEmail] = useState('');
  const [notifyEmailError, setNotifyEmailError] = useState(false);
  const [emailErrorMsg, setEmailErrorMsg] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [notifyOptions, setNotifyOptions] = useState([]);
  const [notifyAutoLoader, setNotifyAutoLoader] = useState(false);
  const [isValidEmail, setIsValidEmail] = useState(true);
  const [isValidNotifyEmail, setIsValidNotifyEmail] = useState(true);
  const [emailError, setEmailError] = useState(false);
  const [projectLeadEmailError, setProjectLeadEmailError] = useState(false);
  const [isValidProjectLeadEmail, setIsValidProjectLeadEmail] = useState(true);
  const [projectLeadOptions, setProjectLeadOptions] = useState([]);
  const [projectLeadAutoLoader, setProjectLeadAutoLoader] = useState(false);
  const [projectLeadEmail, setProjectLeadEmail] = useState('');
  const [disabledUpdate, setDisabledUpdate] = useState(true);
  const [isAdmin, setIsAdmin] = useState(true);

  useEffect(() => {
    const admin = localStorage.getItem('isAdmin');
    if (admin) {
      setIsAdmin(JSON.parse(admin));
    }
  }, []);

  useEffect(() => {
    if (Object.keys(certificateData).length > 0) {
      setApplicationOwner(certificateData?.applicationOwnerEmailId);
      setProjectLeadEmail(certificateData?.projectLeadEmailId);
      const array = certificateData?.notificationEmails?.split(',');
      if (array?.length > 0) {
        setNotificationEmailList([...array]);
      }
    }
  }, [certificateData]);

  useEffect(() => {
    if (
      emailError ||
      projectLeadEmailError ||
      !isValidProjectLeadEmail ||
      !isValidEmail ||
      projectLeadEmail === '' ||
      applicationOwner === '' ||
      notificationEmailList.length === 0 ||
      (projectLeadEmail === certificateData.projectLeadEmailId &&
        applicationOwner === certificateData.applicationOwnerEmailId &&
        notificationEmailList.toString() === certificateData.notificationEmails)
    ) {
      setDisabledUpdate(true);
    } else {
      setDisabledUpdate(false);
    }
  }, [
    isValidEmail,
    isValidProjectLeadEmail,
    emailError,
    projectLeadEmailError,
    notificationEmailList,
    applicationOwner,
    projectLeadEmail,
    certificateData,
  ]);

  useEffect(() => {
    if (applicationOwner?.length > 2) {
      if (
        !autoLoader &&
        applicationOwner !== certificateData.applicationOwnerEmailId
      ) {
        if (
          options.length === 0 ||
          !options.find((item) => item.userEmail === applicationOwner)
        ) {
          setIsValidEmail(false);
        } else {
          setIsValidEmail(true);
        }
      }
    }
  }, [applicationOwner, autoLoader, options, certificateData]);

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
    if (projectLeadEmail?.length > 2) {
      if (
        !projectLeadAutoLoader &&
        projectLeadEmail !== certificateData.projectLeadEmailId
      ) {
        if (
          projectLeadOptions.length === 0 ||
          !projectLeadOptions.find(
            (item) => item.userEmail === projectLeadEmail
          )
        ) {
          setIsValidProjectLeadEmail(false);
        } else {
          setIsValidProjectLeadEmail(true);
        }
      }
    }
  }, [
    projectLeadEmail,
    projectLeadAutoLoader,
    projectLeadOptions,
    certificateData,
  ]);

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
      setApplicationOwner(e?.target?.value);
      if (e?.target?.value && e?.target?.value?.length > 2) {
        callSearchApi(e.target.value);
        if (validateEmail(e.target.value)) {
          setEmailError(false);
        } else {
          setEmailError(true);
        }
      }
    }
  };

  const onSelected = (e, val) => {
    if (val) {
      setApplicationOwner(val);
      setEmailError(false);
    }
  };

  const onNotifyEmailSelected = (e, val) => {
    if (val) {
      setNotifyEmail(val);
      setNotifyEmailError(false);
    }
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

  const onProjectLeadSelected = (e, val) => {
    setProjectLeadEmail(val);
    setProjectLeadEmailError(false);
  };

  const callProjectLeadSearchApi = useCallback(
    debounce(
      (value) => {
        setProjectLeadAutoLoader(true);
        apiService
          .getOwnerTransferEmail(value)
          .then((res) => {
            setProjectLeadOptions([]);
            const array = [];
            setProjectLeadAutoLoader(false);
            if (res?.data?.data?.values?.length > 0) {
              res.data.data.values.map((item) => {
                if (item.userEmail) {
                  return array.push(item);
                }
                return null;
              });
              setProjectLeadOptions([...array]);
            }
          })
          .catch(() => {
            setProjectLeadAutoLoader(false);
            setResponseType(-1);
            setToastMessage('Something went wrong while fetching emails!');
          });
      },
      1000,
      true
    ),
    []
  );

  const onProjectLeadChange = (e) => {
    if (e) {
      setProjectLeadEmail(e?.target?.value);
      if (e?.target?.value && e?.target.value?.length > 2) {
        callProjectLeadSearchApi(e.target.value);
        if (validateEmail(e?.target?.value)) {
          setProjectLeadEmailError(false);
        } else {
          setProjectLeadEmailError(true);
        }
      }
    }
  };

  const onEditClicked = () => {
    const payload = {
      applicationOwnerEmail: applicationOwner,
      certType: certificateData.certType,
      certificateName: certificateData.certificateName,
      notificationEmail: notificationEmailList.toString(),
      projectLeadEmail,
    };
    onUpdateCertClicked(payload);
  };

  return (
    <ComponentError>
      <GlobalModalWrapper>
        <HeaderWrapper>
          <LeftIcon src={leftArrowIcon} alt="go-back" onClick={onCloseModal} />
          <Typography variant="h5">Edit Certificate</Typography>
        </HeaderWrapper>
        <CertificateHeader />
        <PreviewWrap>
          <PreviewCertificate
            dns={certificateData.dnsNames}
            certificateType={certificateData.certType}
            applicationName={certificateData.applicationName}
            certName={certificateData.certificateName}
            container={certificateData.containerName}
            owner={certificateData.certOwnerEmailId}
            isEditCertificate
          />
          {!isAdmin && (
            <>
              <EachDetail>
                <Label>Application Owner:</Label>
                <Value>{applicationOwner}</Value>
              </EachDetail>
              <EachDetail>
                <Label>Project Lead:</Label>
                <Value>{projectLeadEmail}</Value>
              </EachDetail>
            </>
          )}
          <EditableFormWrap>
            {isAdmin && (
              <>
                <InputFieldLabelWrapper postion>
                  <InputLabel>
                    Application Owner
                    <RequiredCircle margin="0.5rem" />
                  </InputLabel>
                  <AutoCompleteComponent
                    options={[...options.map((item) => item.userEmail)]}
                    classes={classes}
                    searchValue={applicationOwner}
                    icon="search"
                    name="applicationOwner"
                    onSelected={(e, val) => onSelected(e, val)}
                    onChange={(e) => onOwnerChange(e)}
                    placeholder="Email address- Enter min 3 characters"
                    error={
                      applicationOwner?.length > 2 &&
                      (emailError || !isValidEmail)
                    }
                    helperText={
                      applicationOwner?.length > 2 &&
                      (emailError || !isValidEmail)
                        ? 'Please enter a valid email address or not available!'
                        : ''
                    }
                  />
                  {autoLoader && (
                    <LoaderSpinner customStyle={autoLoaderStyle} />
                  )}
                </InputFieldLabelWrapper>
                <InputFieldLabelWrapper postion>
                  <InputLabel>
                    Project Lead
                    <RequiredCircle margin="0.5rem" />
                  </InputLabel>
                  <AutoCompleteComponent
                    options={[
                      ...projectLeadOptions.map((item) => item.userEmail),
                    ]}
                    classes={classes}
                    searchValue={projectLeadEmail}
                    icon="search"
                    name="applicationOwner"
                    onSelected={(e, val) => onProjectLeadSelected(e, val)}
                    onChange={(e) => onProjectLeadChange(e)}
                    placeholder="Email address- Enter min 3 characters"
                    error={
                      projectLeadEmail?.length > 2 &&
                      (projectLeadEmailError || !isValidProjectLeadEmail)
                    }
                    helperText={
                      projectLeadEmail?.length > 2 &&
                      (projectLeadEmailError || !isValidProjectLeadEmail)
                        ? 'Please enter a valid email address or not available!'
                        : ''
                    }
                  />
                  {projectLeadAutoLoader && (
                    <LoaderSpinner customStyle={autoLoaderStyle} />
                  )}
                </InputFieldLabelWrapper>
              </>
            )}

            <NotificationEmailsWrap>
              <InputLabel>
                Add Emails to Notify
                <RequiredCircle margin="0.5rem" />
              </InputLabel>
            </NotificationEmailsWrap>
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
          </EditableFormWrap>
          <EachDetail>
            <Label>Status:</Label>
            <Value>{certificateData.certificateStatus}</Value>
          </EachDetail>
          <EachDetail>
            <Label>Create Data:</Label>
            <Value>{certificateData.createDate}</Value>
          </EachDetail>
          <EachDetail>
            <Label>Expiry Date:</Label>
            <Value>{certificateData.expiryDate}</Value>
          </EachDetail>
        </PreviewWrap>
        <CancelSaveWrapper>
          <CancelButton>
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={onCloseModal}
            />
          </CancelButton>
          <CancelButton>
            <ButtonComponent
              label="Update"
              color="secondary"
              disabled={disabledUpdate}
              onClick={() => onEditClicked()}
            />
          </CancelButton>
          {showRevokeRenewBtn && (
            <CancelButton>
              <ButtonComponent
                label="Revoke"
                color="secondary"
                onClick={() => onCertRevokeClicked()}
              />
            </CancelButton>
          )}
          {showRevokeRenewBtn && (
            <ButtonComponent
              label="Renew"
              color="secondary"
              onClick={() => onCertRenewClicked()}
            />
          )}
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
        {responseType === 1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message="Certificate details updated successfully!"
          />
        )}
      </GlobalModalWrapper>
    </ComponentError>
  );
};

ViewCertificate.propTypes = {
  onCertRenewClicked: PropTypes.func,
  onCloseModal: PropTypes.func,
  certificateData: PropTypes.objectOf(PropTypes.any),
  showRevokeRenewBtn: PropTypes.bool,
  onCertRevokeClicked: PropTypes.func,
  onUpdateCertClicked: PropTypes.func,
};

ViewCertificate.defaultProps = {
  onCertRenewClicked: () => {},
  onCloseModal: () => {},
  onCertRevokeClicked: () => {},
  certificateData: {},
  onUpdateCertClicked: () => {},
  showRevokeRenewBtn: false,
};

export default ViewCertificate;