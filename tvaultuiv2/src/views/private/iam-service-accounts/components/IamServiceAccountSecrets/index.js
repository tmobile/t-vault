/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import ReportProblemOutlinedIcon from '@material-ui/icons/ReportProblemOutlined';
import PropTypes from 'prop-types';
import VisibilityIcon from '@material-ui/icons/Visibility';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import VisibilityOffIcon from '@material-ui/icons/VisibilityOff';
import FileCopyIcon from '@material-ui/icons/FileCopy';
import BackdropLoader from '../../../../../components/Loaders/BackdropLoader';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../../apiService';
import lock from '../../../../../assets/icon_lock.svg';
import refreshIcon from '../../../../../assets/refresh-ccw.svg';
import NoSecretsIcon from '../../../../../assets/no-data-secrets.svg';
import AccessDeniedLogo from '../../../../../assets/accessdenied-logo.svg';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import mediaBreakpoints from '../../../../../breakpoints';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import { useStateValue } from '../../../../../contexts/globalState';
import {
  PopperItem,
  BackgroundColor,
} from '../../../../../styles/GlobalStyles';
import PopperElement from '../../../../../components/Popper';
import SnackbarComponent from '../../../../../components/Snackbar';
import Error from '../../../../../components/Error';
import Folder from '../Folder';

const UserList = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0;
  border-bottom: 1px solid #323649;
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }

  .expirationDate {
    font-family: Roboto;
    font-size: 1.34rem;
    color: #ffffff;

    div {
      color: #c1c1c1;
    }
  }
`;

const Secret = styled.div`
  -webkit-text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  font-size: 1.2rem;
  color: #5a637a;
  word-break: break-all;
`;

const Span = styled('span')``;

const Icon = styled.img`
  width: 1.5rem;
  height: 1.5rem;
  margin-right: 1.6rem;
  margin-left: 2rem;
`;

const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  cursor: pointer;
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

const IamServiceAccountSecrets = (props) => {
  const {
    accountDetail,
    accountMetaData,
    accountSecretError,
    accountSecretData,
    getSecrets,
    isIamSvcAccountActive,
    status,
  } = props;
  const [response, setResponse] = useState({ status: '' });
  const [secretsData, setSecretsData] = useState({});
  const [showSecret, setShowSecret] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [openConfirmationModal, setOpenConfirmationModal] = useState({});
  const [writePermission, setWritePermission] = useState(false);
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const [state] = useStateValue();
  /**
   * @function handleClose
   * @description function to handle opening and closing of confirmation modal.
   */
  const handleClose = () => {
    setOpenConfirmationModal({});
  };

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
  const onViewSecretDetails = (folderName) => {
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
  };

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
        setResponse({});
        setResponseType(-1);
        setToastMessage(err?.response?.data?.errors[0]);
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
          await getSecrets();
        }
      })
      .catch((err) => {
        if (err?.response?.data?.errors[0]) {
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

  /**
   * @description to check the whether the user have write permission
   * compare the service account data with loged in user.
   */
  useEffect(() => {
    if (accountMetaData?.response?.users) {
      Object.entries(accountMetaData.response.users).map(([key, value]) => {
        if (
          key.toLowerCase() === state.username.toLowerCase() &&
          value === 'write'
        ) {
          return setWritePermission(true);
        }
        return null;
      });
    }
  }, [accountMetaData, state]);

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal?.status === 'open'}
          handleClose={handleClose}
          title={openConfirmationModal?.title}
          description={openConfirmationModal?.description}
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
        {(response.status === 'loading' ||
          status.status === 'secrets-loading') && <BackdropLoader />}
        {accountSecretData?.folders?.length && !accountSecretError
          ? accountSecretData?.folders.map((secret) => (
              // eslint-disable-next-line react/jsx-indent
              <Folder
                key={secret}
                labelValue={secret}
                onClick={onViewSecretDetails}
              >
                {response.status === 'success' && secretsData && (
                  <UserList>
                    <Icon src={lock} alt="lock" />
                    <Span>{secretsData.accessKeyId}</Span>
                    <Secret type="password" viewSecret={showSecret}>
                      {secretsData.accessKeySecret}
                    </Secret>
                    <span className="expirationDate">
                      <div>Expires: </div>
                      {formatDate(secretsData.expiryDate)}
                    </span>

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
                          {showSecret ? (
                            <VisibilityOffIcon />
                          ) : (
                            <VisibilityIcon />
                          )}
                          <span>
                            {showSecret ? 'Hide Secret key' : 'View Secret key'}
                          </span>
                        </PopperItem>

                        {writePermission && (
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
              </Folder>
            ))
          : null}

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

        {(response.status === 'error' ||
          status.status === 'error' ||
          (accountSecretError && isIamSvcAccountActive)) && (
          <Error
            description={
              accountSecretError || response.message || 'Something went wrong!'
            }
          />
        )}
        {!accountDetail?.name && (
          <AccessDeniedWrap>
            <AccessDeniedIcon src={NoSecretsIcon} alt="accessDeniedLogo" />
            <NoPermission>
              Once you onboard a <span>Service Account</span> you’ll be able to
              view <span>Secret</span> all here!
            </NoPermission>
          </AccessDeniedWrap>
        )}
        {accountDetail?.permission === 'deny' && (
          <AccessDeniedWrap>
            <AccessDeniedIcon src={AccessDeniedLogo} alt="accessDeniedLogo" />
            <NoPermission>
              Access Denied: No permission to read or rotate secret for the
              given IAM service account
            </NoPermission>
          </AccessDeniedWrap>
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
  accountMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  status: PropTypes.objectOf(PropTypes.any).isRequired,
  accountSecretError: PropTypes.string,
  accountSecretData: PropTypes.objectOf(PropTypes.any),
  getSecrets: PropTypes.func,
  isIamSvcAccountActive: PropTypes.bool.isRequired,
};

IamServiceAccountSecrets.defaultProps = {
  accountSecretError: 'Something went wrong!',
  accountSecretData: {},
  getSecrets: () => {},
};

export default IamServiceAccountSecrets;
