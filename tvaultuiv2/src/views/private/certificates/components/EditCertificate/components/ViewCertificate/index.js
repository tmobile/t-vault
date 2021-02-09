import React, { useCallback, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { debounce } from 'lodash';
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
import {
  GlobalModalWrapper,
  RequiredCircle,
} from '../../../../../../../styles/GlobalStyles';
import apiService from '../../../../apiService';
import TypeAheadComponent from '../../../../../../../components/TypeAheadComponent';
import RadioButtonComponent from '../../../../../../../components/FormFields/RadioButton';

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
  display: flex;
  .MuiTextField-root {
    width: 100%;
  }
`;

const SearchInputFieldLabelWrapper = styled.div`
  margin-bottom: 2rem;
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

const TypeAheadWrap = styled.div`
  width: 100%;
`;

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
  const [responseType, setResponseType] = useState(null);
  const [notificationEmailList, setNotificationEmailList] = useState([]);
  const [notifyEmail, setNotifyEmail] = useState('');
  const [notifyEmailError, setNotifyEmailError] = useState(false);
  const [emailErrorMsg, setEmailErrorMsg] = useState('');
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
  const [ownerSelected, setOwnerselected] = useState({});
  const [searchBy, setSearchBy] = useState('User');

  const [projectLeadSelected, setProjectLeadselected] = useState({});

  const [notifyUserSelected, setNotifyUserselected] = useState({});

  useEffect(() => {
    const admin = sessionStorage.getItem('isAdmin');
    if (admin) {
      setIsAdmin(JSON.parse(admin));
    }
  }, []);

  useEffect(() => {
    if (Object.keys(certificateData).length > 0) {
      setApplicationOwner(certificateData?.applicationOwnerEmailId);
      setProjectLeadEmail(certificateData?.projectLeadEmailId);
      const array = certificateData?.notificationEmails
        ? certificateData?.notificationEmails?.split(',')
        : [];
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
    if (applicationOwner?.length > 2 && ownerSelected?.userEmail) {
      if (
        !autoLoader &&
        applicationOwner !== certificateData.applicationOwnerEmailId
      ) {
        if (ownerSelected?.userEmail.toLowerCase() !== applicationOwner) {
          setIsValidEmail(false);
        } else {
          setIsValidEmail(true);
        }
      }
    }
  }, [applicationOwner, ownerSelected, autoLoader, certificateData]);

  useEffect(() => {
    if (!notifyAutoLoader && notifyEmail?.length > 2) {
      if (notifyUserSelected?.userEmail && searchBy !== 'GroupEmail') {
        if (notifyUserSelected?.userEmail.toLowerCase() !== notifyEmail) {
          setIsValidNotifyEmail(false);
          setEmailErrorMsg('Please enter a valid user or not available!');
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
    if (projectLeadEmail?.length > 2 && projectLeadSelected?.userEmail) {
      if (
        !projectLeadAutoLoader &&
        projectLeadEmail !== certificateData.projectLeadEmailId
      ) {
        if (projectLeadEmail !== projectLeadSelected?.userEmail.toLowerCase()) {
          setIsValidProjectLeadEmail(false);
        } else {
          setIsValidProjectLeadEmail(true);
        }
      }
    }
  }, [
    projectLeadEmail,
    projectLeadSelected,
    projectLeadOptions,
    projectLeadAutoLoader,
    certificateData,
  ]);

  const callSearchByGroupemailApi = useCallback(
    debounce(
      (value) => {
        setNotifyAutoLoader(true);
        apiService
          .searchByGroupEmail(value)
          .then((response) => {
            setNotifyOptions([]);
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
            } else if (type === 'projectLead') {
              setProjectLeadAutoLoader(false);
              setProjectLeadOptions([...array]);
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
      setApplicationOwner(e?.target?.value);
      if (e?.target?.value && e?.target?.value?.length > 2) {
        setOptions([]);
        setAutoLoader(true);
        callSearchApi(e.target.value, 'applicationOwner');
        if (validateEmail(applicationOwner)) {
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
      setApplicationOwner(applicationOwnerEmail);
      setOwnerselected(
        options.filter(
          (i) => i?.userEmail?.toLowerCase() === applicationOwnerEmail
        )[0]
      );
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

  const onAddEmailClicked = () => {
    const obj = notificationEmailList.find(
      (item) => item.toLowerCase() === notifyEmail.toLowerCase()
    );
    if (!notifyEmailError && isValidNotifyEmail && notifyEmail !== '') {
      if (!obj) {
        setNotificationEmailList((prev) => [...prev, notifyEmail]);
        setNotifyEmail('');
      } else {
        setNotifyEmailError(true);
        setEmailError('Duplicate Email!');
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

  const onProjectLeadSelected = (e, val) => {
    const projectLeadUserEmail = val?.split(', ')[0];
    setProjectLeadselected(
      projectLeadOptions.filter(
        (i) => i?.userEmail?.toLowerCase() === projectLeadUserEmail
      )[0]
    );
    setProjectLeadEmail(projectLeadUserEmail);
    setProjectLeadEmailError(false);
  };

  const onProjectLeadChange = (e) => {
    if (e?.target?.value !== undefined) {
      setProjectLeadEmail(e?.target?.value);
      if (e?.target?.value && e?.target.value?.length > 2) {
        setProjectLeadOptions([]);
        setProjectLeadAutoLoader(true);
        callSearchApi(e.target.value, 'projectLead');
        if (validateEmail(e?.target?.value)) {
          setProjectLeadEmailError(false);
        } else {
          setProjectLeadEmailError(true);
        }
      }
    } else {
      setProjectLeadAutoLoader(false);
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
                  <TypeAheadComponent
                    options={options.map(
                      (item) =>
                        `${item?.userEmail?.toLowerCase()}, ${getName(
                          item?.displayName?.toLowerCase()
                        )}, ${item?.userName?.toLowerCase()}`
                    )}
                    loader={autoLoader}
                    userInput={applicationOwner}
                    icon="search"
                    name="applicationOwner"
                    onSelected={(e, val) => onSelected(e, val)}
                    onChange={(e) => onOwnerChange(e)}
                    placeholder="Search by NTID, Email or Name "
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
                  <TypeAheadComponent
                    options={projectLeadOptions.map(
                      (item) =>
                        `${item?.userEmail?.toLowerCase()}, ${getName(
                          item?.displayName?.toLowerCase()
                        )}, ${item?.userName?.toLowerCase()}`
                    )}
                    loader={projectLeadAutoLoader}
                    userInput={projectLeadEmail}
                    icon="search"
                    name="applicationOwner"
                    onSelected={(e, val) => onProjectLeadSelected(e, val)}
                    onChange={(e) => onProjectLeadChange(e)}
                    placeholder="Search by NTID, Email or Name "
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
            <NotificationEmailsWrap>
              <InputLabel>
                Add Emails to Notify
                <RequiredCircle margin="0.5rem" />
              </InputLabel>
            </NotificationEmailsWrap>
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
                    icon="search"
                    name="notifyEmail"
                    onSelected={(e, val) => onNotifyEmailSelected(e, val)}
                    onKeyDownClick={(e) => onEmailKeyDownClicked(e)}
                    onChange={(e) => onNotifyEmailChange(e)}
                    placeholder={
                      searchBy === 'GroupEmail'
                        ? 'Search by GroupEmail'
                        : 'Search by NTID, Email or Name'
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
                <EndingBox width="4rem">
                  <ReturnIcon onClick={() => onAddEmailClicked()}>
                    <KeyboardReturnIcon />
                  </ReturnIcon>
                </EndingBox>
              </AutoInputFieldLabelWrapper>
            </NotificationAutoWrap>
            <ArrayList>
              {notificationEmailList.length > 0 &&
                notificationEmailList.map((item) => {
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
