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
    flex-wrap: wrap;
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
    margin-right: 0rem;
    width: 100%;
    margin-bottom: 0.5rem;
  }
`;
const Label = styled.p`
  font-size: 1.3rem;
  color: ${(props) => props.theme.customColor.label.color};
  margin-bottom: 0.9rem;
`;

const Value = styled.p`
  font-size: 1.8rem;
  text-transform: ${(props) => props.capitalize || ''};
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
    onDeleteClicked,
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
  const [ownerDetail, setownerDetail] = useState(applicationOwner);
  const [ownerSelected, setOwnerselected] = useState(false);

  const [projectLeadDetail, setProjectLeadDetail] = useState(projectLeadEmail);
  const [projectLeadSelected, setProjectLeadselected] = useState(false);

  const [notifyUserDetail, setNotifyUserDetail] = useState(notifyEmail);
  const [notifyUserSelected, setNotifyUserselected] = useState(false);

  useEffect(() => {
    const admin = localStorage.getItem('isAdmin');
    if (admin) {
      setIsAdmin(JSON.parse(admin));
    }
  }, []);

  useEffect(() => {
    if (Object.keys(certificateData).length > 0) {
      setApplicationOwner(certificateData?.applicationOwnerEmailId);
      setownerDetail(certificateData?.applicationOwnerEmailId);
      setProjectLeadDetail(certificateData?.projectLeadEmailId);
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
    if (ownerDetail?.length > 2) {
      if (
        !autoLoader &&
        ownerDetail !== certificateData.applicationOwnerEmailId
      ) {
        if (options.length === 0 || !options.includes(ownerDetail)) {
          setIsValidEmail(false);
        } else {
          setIsValidEmail(true);
        }
      }
    }
  }, [ownerDetail, autoLoader, options, certificateData]);

  useEffect(() => {
    if (notifyUserDetail?.length > 2) {
      if (!notifyAutoLoader) {
        if (
          notifyOptions.length === 0 ||
          !notifyOptions.includes(notifyUserDetail)
        ) {
          setIsValidNotifyEmail(false);
        } else {
          setIsValidNotifyEmail(true);
        }
      }
    }
  }, [notifyUserDetail, notifyOptions, notifyAutoLoader]);

  useEffect(() => {
    if (projectLeadEmail?.length > 2) {
      if (
        !projectLeadAutoLoader &&
        projectLeadEmail !== certificateData.projectLeadEmailId
      ) {
        if (
          projectLeadOptions.length === 0 ||
          !projectLeadOptions.includes(projectLeadDetail)
        ) {
          setIsValidProjectLeadEmail(false);
        } else {
          setIsValidProjectLeadEmail(true);
        }
      }
    }
  }, [
    projectLeadEmail,
    projectLeadDetail,
    projectLeadOptions,
    projectLeadAutoLoader,
    certificateData,
  ]);

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
                  return array.add(
                    `${item.displayName} [${item.userEmail}] (${item.userName})`
                  );
                }
                return null;
              });
            }
            if (responses[1]?.data?.data?.values?.length > 0) {
              responses[1].data.data.values.map((item) => {
                if (item.userName) {
                  return array.add(
                    `${item.displayName} [${item.userEmail}] (${item.userName})`
                  );
                }
                return null;
              });
            }
            if (type === 'applicationOwner') {
              setOptions([...array]);
            } else if (type === 'projectLead') {
              setProjectLeadOptions([...array]);
            } else {
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
    if (e) {
      setownerDetail(e?.target?.value);
      if (e?.target?.value && e?.target?.value?.length > 2) {
        setOptions([]);
        setOwnerselected(false);
        setAutoLoader(true);
        callSearchApi(e.target.value, 'applicationOwner');
        setAutoLoader(false);
        if (validateEmail(applicationOwner)) {
          setEmailError(false);
        } else {
          setEmailError(true);
        }
      }
    } else {
      setownerDetail('');
      setApplicationOwner('');
    }
  };

  const onSelected = (e, val) => {
    if (val) {
      setownerDetail(val);
      const applicationOwnerEmail = val?.match(/\[(.*)\]/)[1].toLowerCase();
      setApplicationOwner(applicationOwnerEmail);
      setOwnerselected(true);
      setEmailError(false);
    }
  };

  const onNotifyEmailSelected = (e, val) => {
    if (val) {
      setNotifyUserDetail(val);
      const notifyUserEmail = val?.match(/\[(.*)\]/)[1].toLowerCase();
      setNotifyEmail(notifyUserEmail);
      setNotifyEmailError(false);
      setNotifyUserselected(true);
    }
  };

  const onAddEmailClicked = () => {
    const obj = notificationEmailList.find(
      (item) => item.toLowerCase() === notifyEmail.toLowerCase()
    );
    if (
      !notifyEmailError &&
      isValidNotifyEmail &&
      notifyEmail !== '' &&
      validateEmail(notifyEmail)
    ) {
      if (!obj) {
        setNotificationEmailList((prev) => [...prev, notifyEmail]);
        setNotifyEmail('');
        setNotifyUserDetail('');
      } else {
        setNotifyEmailError(true);
        setEmailErrorMsg('Duplicate Email!');
      }
    }
  };

  const onEmailKeyDownClicked = (e) => {
    if (e?.keyCode === 13) {
      e.preventDefault();
      onAddEmailClicked();
    }
  };

  const onNotifyEmailChange = (e) => {
    if (e && e.target.value) {
      setNotifyUserDetail(e?.target?.value);
      if (e.target.value && e.target.value?.length > 2) {
        setNotifyOptions([]);
        setNotifyUserselected(false);
        setNotifyAutoLoader(true);
        callSearchApi(e.target.value, 'notifyUser');
        setNotifyAutoLoader(false);
        if (validateEmail(notifyEmail)) {
          setNotifyEmailError(false);
        } else {
          setNotifyEmailError(true);
          setEmailErrorMsg(
            'Please enter a valid email address or not available!'
          );
        }
      }
    } else {
      setNotifyEmail('');
      setNotifyUserDetail('');
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
    setProjectLeadDetail(val);
    const projectLeadUserEmail = val?.match(/\[(.*)\]/)[1].toLowerCase();
    setProjectLeadEmail(projectLeadUserEmail);
    setProjectLeadEmailError(false);
    setProjectLeadselected(true);
  };

  const onProjectLeadChange = (e) => {
    if (e) {
      setProjectLeadDetail(e?.target?.value);
      if (e?.target?.value && e?.target.value?.length > 2) {
        setProjectLeadOptions([]);
        setProjectLeadselected(false);
        setProjectLeadAutoLoader(true);
        callSearchApi(e.target.value, 'projectLead');
        setProjectLeadAutoLoader(false);
        if (validateEmail(e?.target?.value)) {
          setProjectLeadEmailError(false);
        } else {
          setProjectLeadEmailError(true);
        }
      }
    } else {
      setProjectLeadDetail('');
      setProjectLeadEmail('');
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
                    options={options}
                    classes={classes}
                    searchValue={ownerDetail}
                    icon="search"
                    name="applicationOwner"
                    open={
                      ownerDetail?.length > 2 &&
                      options.length > 0 &&
                      !ownerSelected
                    }
                    onSelected={(e, val) => onSelected(e, val)}
                    onChange={(e) => onOwnerChange(e)}
                    placeholder="Search by NTID, Email or Name "
                    error={
                      ownerDetail?.length > 2 && (emailError || !isValidEmail)
                    }
                    helperText={
                      ownerDetail?.length > 2 && (emailError || !isValidEmail)
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
                    options={projectLeadOptions}
                    classes={classes}
                    searchValue={projectLeadDetail}
                    icon="search"
                    name="applicationOwner"
                    open={
                      projectLeadDetail?.length > 2 &&
                      projectLeadOptions.length > 0 &&
                      !projectLeadSelected
                    }
                    onSelected={(e, val) => onProjectLeadSelected(e, val)}
                    onChange={(e) => onProjectLeadChange(e)}
                    placeholder="Search by NTID, Email or Name "
                    error={
                      projectLeadDetail?.length > 2 &&
                      (projectLeadEmailError || !isValidProjectLeadEmail)
                    }
                    helperText={
                      projectLeadDetail?.length > 2 &&
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
                  searchValue={notifyUserDetail}
                  icon="search"
                  name="notifyEmail"
                  open={
                    notifyUserDetail?.length > 2 &&
                    notifyOptions.length > 0 &&
                    !notifyUserSelected
                  }
                  onSelected={(e, val) => onNotifyEmailSelected(e, val)}
                  onKeyDown={(e) => onEmailKeyDownClicked(e)}
                  onChange={(e) => onNotifyEmailChange(e)}
                  placeholder="Search by NTID, Email or Name "
                  error={
                    notifyUserDetail?.length > 2 &&
                    (notifyEmailError || !isValidNotifyEmail)
                  }
                  helperText={
                    notifyUserDetail?.length > 2 &&
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
          <CancelButton>
            <ButtonComponent
              label="Delete"
              color="secondary"
              onClick={() => onDeleteClicked()}
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
  onDeleteClicked: PropTypes.func,
};

ViewCertificate.defaultProps = {
  onCertRenewClicked: () => {},
  onCloseModal: () => {},
  onCertRevokeClicked: () => {},
  certificateData: {},
  onUpdateCertClicked: () => {},
  onDeleteClicked: () => {},
  showRevokeRenewBtn: false,
};

export default ViewCertificate;
