/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import PropTypes from 'prop-types';
import VisibilityIcon from '@material-ui/icons/Visibility';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import VisibilityOffIcon from '@material-ui/icons/VisibilityOff';
import FileCopyIcon from '@material-ui/icons/FileCopy';
import IconRefreshCC from '../../../../../assets/refresh-ccw.svg';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../../apiService';
import lock from '../../../../../assets/icon_lock.svg';
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
  margin-top: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0;
  border-bottom: 1px solid #323649;
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
`;

const Secret = styled.div`
  -webkit-text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  font-size: 1.2rem;
  color: #5a637a;
  word-break: break-all;
`;

const customStyle = css`
  height: 100%;
`;

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

const NoPermission = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #5a637a;
  text-align: center;
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

  /**
   * @function onCopyClicked
   * @description function to copy the secret.
   */
  const onCopyClicked = () => {
    setResponseType(1);
    setToastMessage('Secret copied to clipboard');
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
      .catch((err) => {
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
    const payload = {};
    setOpenConfirmationModal({
      status: 'open',
      type: 'rotate',
      title: '',
      description: '',
    });
    setResponse({ status: 'loading' });
    apiService
      .rotateIamServiceAccountPassword(payload)
      .then((res) => {
        setResponse({ status: 'success' });
        if (res?.data) {
          setResponseType(1);
          setToastMessage(
            res.data.messages[0] || 'Password rotated successfully!'
          );
        }
      })
      .catch(() => {
        setResponse({ status: 'error' });
        setResponseType(-1);
        setToastMessage('Unable to rotate password!');
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
        "During the activation. the password of the LAM service account will be rotated to ensure AWS and T-Vault are in sync If you want to continue with activation now please click the 'ACTIVATE IAM SERVICE ACCOUNT’ button below and make sure to update any services depending on the service account with its new password.",
    });
  };

  /**
   * @function onActivateConfirm
   * To activate the servcie account once it is onBoarded
   */

  const onActivateConfirm = () => {
    setResponse({ status: 'loading' });
    apiService
      .activateIamServiceAccount()
      .then((res) => {
        if (res?.data) {
          setResponse({ status: 'success', message: res.data.messages[0] });
        }
      })
      .catch((err) => {
        if (err?.response?.data?.errors[0]) {
          setResponse({ status: 'error' });
        }
      });
  };

  /**
   * @function onResetClicked
   * @description function to open the confirmation modal.
   */
  const onResetClicked = () => {
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
        if (key === state.username && value === 'write') {
          return setWritePermission(true);
        }
        return setWritePermission(false);
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
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
          confirmButton={
            <ButtonComponent
              label="Confirm"
              color="secondary"
              onClick={
                openConfirmationModal?.type === 'activate'
                  ? () => onActivateConfirm()
                  : () => onRotateConfirmedClicked()
              }
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {accountSecretData?.folders?.length
          ? accountSecretData?.folders.map((secret, index) => (
              <Folder
                key={secret}
                labelValue={secret}
                onClick={onViewSecretDetails}
              >
                {' '}
                {response.status === 'success' && secretsData && (
                  <UserList>
                    <Icon src={lock} alt="lock" />
                    <Secret type="password" viewSecret={showSecret}>
                      {secretsData.accessKeySecret}
                    </Secret>

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
                            {showSecret ? 'Hide Secret' : 'View Secret'}
                          </span>
                        </PopperItem>
                        {writePermission && (
                          <PopperItem onClick={() => onResetClicked()}>
                            <img alt="refersh-ic" src={IconRefreshCC} />
                            <span>Rotate Secret</span>
                          </PopperItem>
                        )}
                        <CopyToClipboard
                          text={secretsData.accessKeySecret}
                          onCopy={() => onCopyClicked()}
                        >
                          <PopperItem>
                            <FileCopyIcon />
                            <span>Copy Secret</span>
                          </PopperItem>
                        </CopyToClipboard>
                      </PopperElement>
                    </FolderIconWrap>
                  </UserList>
                )}
              </Folder>
            ))
          : null}
        {!accountMetaData?.response?.isActivated && (
          <UserList>
            <Icon src={lock} alt="lock" />
            <Secret type="password" viewSecret={showSecret}>
              ****
            </Secret>

            <FolderIconWrap onClick={() => activateServiceAccount()}>
              activate
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
        {response.status === 'no-permission' && (
          <NoPermission>
            Access denied: no permission to read the password details for the{' '}
            <span>{accountDetail.name}</span> service account.
          </NoPermission>
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
  accountSecretError: PropTypes.string,
  accountSecretData: PropTypes.objectOf(PropTypes.any),
  secretStatus: PropTypes.string,
};

IamServiceAccountSecrets.defaultProps = {
  accountSecretError: 'Something went wrong!',
  accountSecretData: {},
  secretStatus: 'loading',
};

export default IamServiceAccountSecrets;
