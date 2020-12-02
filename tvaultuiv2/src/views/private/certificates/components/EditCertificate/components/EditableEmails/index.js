/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useCallback, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { debounce } from 'lodash';
import { InputLabel } from '@material-ui/core';
import styled, { css } from 'styled-components';
import KeyboardReturnIcon from '@material-ui/icons/KeyboardReturn';
import PropTypes from 'prop-types';
import removeIcon from '../../../../../../../assets/close.svg';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import AutoCompleteComponent from '../../../../../../../components/FormFields/AutoComplete';
import LoaderSpinner from '../../../../../../../components/Loaders/LoaderSpinner';
import apiService from '../../../../apiService';
import { validateEmail } from '../../../../../../../services/helper-function';
import SnackbarComponent from '../../../../../../../components/Snackbar';
import { RequiredCircle } from '../../../../../../../styles/GlobalStyles';

const OnboardFormWrap = styled.form`
  display: ${(props) => (props.showPreviewData ? 'none' : 'flex')};
  flex-direction: column;
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

const EditableEmails = (props) => {
  const { certificateData } = props;

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
  const [isValidNotifyEmail, setIsValidNotifyEmail] = useState(false);
  const [emailError, setEmailError] = useState(false);
  const [projectLeadEmailError, setProjectLeadEmailError] = useState(false);
  const [isValidProjectLeadEmail, setIsValidProjectLeadEmail] = useState(false);
  const [projectLeadOptions, setProjectLeadOptions] = useState([]);
  const [projectLeadAutoLoader, setProjectLeadAutoLoader] = useState(false);
  const [projectLeadEmail, setProjectLeadEmail] = useState('');

  useEffect(() => {
    setApplicationOwner(certificateData?.applicationOwnerEmailId);
    setProjectLeadEmail(certificateData?.projectLeadEmailId);
    const array = certificateData?.notificationEmails?.split(',');
    setNotificationEmailList([...array]);
  }, [certificateData]);

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
      if (!projectLeadAutoLoader) {
        if (
          projectLeadOptions.length === 0 ||
          !projectLeadOptions.includes(projectLeadEmail)
        ) {
          setIsValidProjectLeadEmail(false);
        } else {
          setIsValidProjectLeadEmail(true);
        }
      }
    }
  }, [projectLeadEmail, projectLeadAutoLoader, projectLeadOptions]);

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
      if (e.target?.value && e.target.value?.length > 2) {
        callSearchApi(e.target.value);
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
      setApplicationOwner(val);
      setEmailError(false);
    }
  };

  const onNotifyEmailSelected = (e, val) => {
    setNotifyEmail(val);
    setNotifyEmailError(false);
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
    setNotifyEmail(val);
    setNotifyEmailError(false);
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
      if (e.target?.value && e.target.value?.length > 2) {
        callProjectLeadSearchApi(e.target.value);
        if (validateEmail(applicationOwner)) {
          setProjectLeadEmailError(false);
        } else {
          setProjectLeadEmailError(true);
        }
      }
    }
  };

  return (
    <ComponentError>
      <>
        <OnboardFormWrap>
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
                applicationOwner?.length > 2 && (emailError || !isValidEmail)
              }
              helperText={
                applicationOwner?.length > 2 && (emailError || !isValidEmail)
                  ? 'Please enter a valid email address or not available!'
                  : ''
              }
            />
            {autoLoader && <LoaderSpinner customStyle={autoLoaderStyle} />}
          </InputFieldLabelWrapper>
          <InputFieldLabelWrapper postion>
            <InputLabel>
              Project Lead
              <RequiredCircle margin="0.5rem" />
            </InputLabel>
            <AutoCompleteComponent
              options={[...projectLeadOptions.map((item) => item.userEmail)]}
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
        </OnboardFormWrap>
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

EditableEmails.propTypes = {
  certificateData: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default EditableEmails;
