/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import ReportProblemOutlinedIcon from '@material-ui/icons/ReportProblemOutlined';
import PropTypes from 'prop-types';
import VisibilityIcon from '@material-ui/icons/Visibility';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import VisibilityOffIcon from '@material-ui/icons/VisibilityOff';
import FileCopyIcon from '@material-ui/icons/FileCopy';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../../apiService';
import lock from '../../../../../assets/icon_lock.svg';
import refreshIcon from '../../../../../assets/refresh-ccw.svg';
import NoSecretsIcon from '../../../../../assets/no-data-secrets.svg';
import AccessDeniedLogo from '../../../../../assets/accessdenied-logo.svg';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import mediaBreakpoints from '../../../../../breakpoints';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import {
  PopperItem,
  BackgroundColor,
} from '../../../../../styles/GlobalStyles';
import PopperElement from '../../../../../components/Popper';
import SnackbarComponent from '../../../../../components/Snackbar';
import Error from '../../../../../components/Error';

const UserList = styled.div`
  display: flex;
  align-items: center;
  position: relative;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0;
  border-bottom: 1px solid #323649;
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }

  .expirationDate {
    font-size: 1.4rem;
    color: #ffffff;
    display: flex;
    flex-direction: column;
    ${mediaBreakpoints.semiMedium} {
      flex-direction: row;
    }
    .expiry {
      color: #c1c1c1;
      margin-right: 0.2rem;
    }
  }
`;

const Secret = styled.div`
  -webkit-text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  font-size: 1.2rem;
  color: #5a637a;
  word-break: break-all;
  margin: 0 2rem;
  ${mediaBreakpoints.semiMedium} {
    margin: 1rem;
  }
`;

const SecretInputfield = styled.input`
  padding: 0;
  outline: none;
  border: none;
  background: transparent;
  font-size: 1.2rem;
  color: #5a637a;
  word-break: break-all;
  margin: 0px 1rem;
  width: 55%;
  text-align: center;
  ${mediaBreakpoints.semiMedium} {
    width: 100%;
    margin: 1rem;
  }
`;

const Span = styled('span')``;

const Icon = styled.img`
  width: 1.5rem;
  height: 1.5rem;
  margin-right: 3rem;
  margin-left: 2rem;
`;

const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  cursor: pointer;
  position: absolute;
  right: 0;
  .MuiSvgIcon-root {
    width: 2rem;
    height: 2rem;
    :hover {
      background: ${(props) =>
        props.theme.customColor.hoverColor.list || '#151820'};
      border-radius: 50%;
    }
  }
`;

const LabelWrap = styled.div`
  display: flex;
  align-items: center;
  padding-left: 2rem;
  span {
    margin-left: 1rem;
  }
`;

const AccessDeniedWrap = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
`;

const AccessDeniedIcon = styled.img`
  width: 16rem;
  height: 16rem;
`;

const NoPermission = styled.div`
  color: #5a637a;
  text-align: center;
  margin-top: 2rem;
  span {
    display: contents;
    margin: 0 0.3rem;
    color: #fff;
  }
`;

const customStyle = css`
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 2;
  transform: translate(-50%, -50%);
`;

const InfoWrapper = styled.div`
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: space-between;
  ${mediaBreakpoints.semiMedium} {
    flex-direction: column;
  }
`;

const SecretDetailsWrap = styled.div`
  display: flex;
  align-items: center;
  width: 90%;
`;

const IamServiceAccountSecrets = (props) => {
  const {
    accountDetail,
    accountSecretError,
    accountSecretData,
    value,
    getSecrets,
    isIamSvcAccountActive,
    secretResponse,
    refresh,
  } = props;
  const [response, setResponse] = useState({ status: 'loading' });
  const [secretsData, setSecretsData] = useState({});
  const [showSecret, setShowSecret] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [openConfirmationModal, setOpenConfirmationModal] = useState({});
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  useEffect(() => {
    setResponse({ status: secretResponse });
  }, [secretResponse]);
  /**
   * @function handleClose
   * @description function to handle opening and closing of confirmation modal.
   */
  const handleClose = () => {
    setOpenConfirmationModal({});
  };

  useEffect(() => {
    setShowSecret(false);
  }, [value]);

  /**
   * @function onViewSecretsCliked
   * @description function to hide and show secret.
   */
  const onViewSecretsCliked = () => {
    setShowSecret(!showSecret);
  };

  const formatDate = (expiryDate = '') => {
    const expirationArr = new Date(expiryDate).toDateString().split(' ');
    if (expirationArr.length > 3) {
      expirationArr.splice(3, 0, ',');
      const expiryFormattedDate = expirationArr.splice(1).join(' ');

      return expiryFormattedDate;
    }
    return null;
  };

  /**
   * @function onCopyClicked
   * @description function to copy the secret.
   */
  const onCopyClicked = (message = 'Secret copied to clipboard') => {
    setResponseType(1);
    setToastMessage(message);
  };

  /**
   * @function onViewSecretDetails
   * @param {string} folderName
   * @description function to call the secret details api , which fetch the
   */
  const onViewSecretDetails = useCallback(
    (folderName) => {
      if (accountDetail.active) {
        setResponse({ status: 'loading' });
        apiService
          .getIamServiceAccountPassword(
            `${accountDetail?.iamAccountId}_${accountDetail?.name}`,
            folderName
          )
          .then((res) => {
            setResponse({ status: 'success' });
            setSecretsData(res?.data);
          })
          .catch(() => {
            setResponse({
              status: 'error',
              message: 'There was a  error while fetching secret details!',
            });
          });
      }
    },
    [accountDetail]
  );

  /**
   * @function onRotateConfirmedClicked
   * @description function to reset secret when the confirm is clicked.
   */
  const onRotateConfirmedClicked = () => {
    const payload = {
      accessKeyId: secretsData?.accessKeyId,
      accountId: secretsData?.awsAccountId,
      userName: secretsData.userName,
    };
    setOpenConfirmationModal({
      status: 'close',
      type: 'rotate',
      title: '',
      description: '',
    });
    setResponse({ status: 'loading' });
    apiService
      .rotateIamServiceAccountPassword(payload)
      .then(async (res) => {
        setResponse({ status: 'success' });
        if (res?.data) {
          setResponseType(1);
          setToastMessage(
            res.data.messages[0] || 'Password rotated successfully!'
          );
          await getSecrets();
        }
      })
      .catch((err) => {
        setResponse({ status: 'success' });
        setResponseType(-1);
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setToastMessage(err?.response?.data?.errors[0]);
        }
      });
  };

  /**
   * @function activateServiceAccount
   * @description function to activate the service account for the first tie when it is onboarded.
   */
  const activateServiceAccount = () => {
    setOpenConfirmationModal({
      status: 'open',
      type: 'activate',
      title: 'IAM Service Account Activation!',
      description:
        "During the activation. the password of the IAM service account will be rotated to ensure AWS and T-Vault are in sync If you want to continue with activation now please click the 'ACTIVATE IAM SERVICE ACCOUNT’ button below and make sure to update any services depending on the service account with its new password.",
    });
  };

  /**
   * @function onActivateConfirm
   * To activate the servcie account once it is onBoarded
   */

  const onActivateConfirm = () => {
    setOpenConfirmationModal({
      status: 'close',
      type: 'activate',
      title: '',
      description: '',
    });
    setResponse({ status: 'loading' });
    apiService
      .activateIamServiceAccount(
        accountDetail?.name,
        accountDetail?.iamAccountId
      )
      .then(async (res) => {
        if (res?.data) {
          setResponse({ status: 'success', message: res.data.messages[0] });
          setResponseType(1);
          setToastMessage(res.data.messages[0]);
          await refresh();
        }
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setResponse({});
          setToastMessage(err?.response?.data?.errors[0]);
        }
        setResponseType(-1);
      });
  };

  /**
   * @function onRotateClicked
   * @description function to open the confirmation modal.
   */
  const onRotateClicked = () => {
    setOpenConfirmationModal({
      status: 'open',
      type: 'rotate',
      title: 'Confirmation',
      description:
        'Are you sure you want to rotate the password for this IAM Service Account?',
    });
  };

  /**
   * @function onToastClose
   * @description function to handle the snackbar component.
   */
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  useEffect(() => {
    if (accountSecretData && Object.keys(accountSecretData).length > 0) {
      onViewSecretDetails(accountSecretData.folders[0]);
    } else {
      setSecretsData({});
    }
    setShowSecret(false);
  }, [accountSecretData, onViewSecretDetails]);

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal?.status === 'open'}
          handleClose={handleClose}
          title={openConfirmationModal?.title || ''}
          description={openConfirmationModal?.description || ''}
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleClose()}
              width={isMobileScreen ? '100%' : '45%'}
            />
          }
          confirmButton={
            <ButtonComponent
              label={
                openConfirmationModal?.type === 'activate'
                  ? 'Activate'
                  : 'Rotate'
              }
              color="secondary"
              onClick={
                openConfirmationModal?.type === 'activate'
                  ? () => onActivateConfirm()
                  : () => onRotateConfirmedClicked()
              }
              width={isMobileScreen ? '100%' : '45%'}
            />
          }
        />
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status !== 'loading' && (
          <>
            {!isIamSvcAccountActive && accountDetail?.name && (
              <UserList>
                <LabelWrap>
                  <ReportProblemOutlinedIcon />
                  <Span>Rotate Secret to Activate</Span>
                </LabelWrap>
                <Secret type="password" viewSecret={showSecret}>
                  ****
                </Secret>
                <FolderIconWrap onClick={() => activateServiceAccount()}>
                  <Icon src={refreshIcon} alt="refresh" />
                </FolderIconWrap>
              </UserList>
            )}
            {!accountDetail?.name && (
              <AccessDeniedWrap>
                <AccessDeniedIcon src={NoSecretsIcon} alt="accessDeniedLogo" />
                <NoPermission>
                  Once you onboard a <span>Service Account</span> you’ll be able
                  to view <span>Secret</span> all here!
                </NoPermission>
              </AccessDeniedWrap>
            )}
            {accountDetail?.permission === 'deny' && (
              <AccessDeniedWrap>
                <AccessDeniedIcon
                  src={AccessDeniedLogo}
                  alt="accessDeniedLogo"
                />
                <NoPermission>
                  Access Denied: No permission to read or rotate secret for the
                  given IAM service account
                </NoPermission>
              </AccessDeniedWrap>
            )}
          </>
        )}
        {response.status === 'success' &&
          Object.keys(secretsData).length > 0 &&
          accountDetail.name && (
            <UserList>
              <SecretDetailsWrap>
                <Icon src={lock} alt="lock" />
                <InfoWrapper>
                  <Span>{secretsData.accessKeyId}</Span>
                  <SecretInputfield
                    type={showSecret ? 'text' : 'password'}
                    value={secretsData.accessKeySecret}
                    readOnly
                  />
                  <div className="expirationDate">
                    <div className="expiry">Expires: </div>
                    <div>{formatDate(secretsData.expiryDate)}</div>
                  </div>
                </InfoWrapper>
              </SecretDetailsWrap>
              <FolderIconWrap>
                <PopperElement
                  anchorOrigin={{
                    vertical: 'bottom',
                    horizontal: 'right',
                  }}
                  transformOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                  }}
                >
                  <PopperItem onClick={() => onViewSecretsCliked()}>
                    {showSecret ? <VisibilityOffIcon /> : <VisibilityIcon />}
                    <span>
                      {showSecret ? 'Hide Secret key' : 'View Secret key'}
                    </span>
                  </PopperItem>
                  {accountDetail.permission === 'write' && (
                    <PopperItem onClick={() => onRotateClicked()}>
                      <img alt="refersh-ic" src={refreshIcon} />
                      <span>Rotate Secret</span>
                    </PopperItem>
                  )}
                  <CopyToClipboard
                    text={secretsData.accessKeySecret}
                    onCopy={() => onCopyClicked()}
                  >
                    <PopperItem>
                      <FileCopyIcon />
                      <span>Copy Secret Key</span>
                    </PopperItem>
                  </CopyToClipboard>
                  <CopyToClipboard
                    text={secretsData.accessKeyId}
                    onCopy={() =>
                      onCopyClicked('Copied Access Id To Clipboard!')
                    }
                  >
                    <PopperItem>
                      <FileCopyIcon />
                      <span>Copy Access Key</span>
                    </PopperItem>
                  </CopyToClipboard>
                </PopperElement>
              </FolderIconWrap>
            </UserList>
          )}
        {response.status === 'error' && (
          <Error
            description={
              accountSecretError || response.message || 'Something went wrong!'
            }
          />
        )}
        {responseType === 1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message={toastMessage}
          />
        )}
        {responseType === -1 && (
          <SnackbarComponent
            open
            severity="error"
            icon="error"
            onClose={() => onToastClose()}
            message={toastMessage}
          />
        )}
      </>
    </ComponentError>
  );
};

IamServiceAccountSecrets.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  secretResponse: PropTypes.string,
  accountSecretError: PropTypes.string,
  accountSecretData: PropTypes.objectOf(PropTypes.any),
  getSecrets: PropTypes.func,
  isIamSvcAccountActive: PropTypes.bool.isRequired,
  refresh: PropTypes.func.isRequired,
  value: PropTypes.number,
};

IamServiceAccountSecrets.defaultProps = {
  accountSecretError: 'Something went wrong!',
  accountSecretData: {},
  getSecrets: () => {},
  secretResponse: '',
  value: 0,
};

export default IamServiceAccountSecrets;
