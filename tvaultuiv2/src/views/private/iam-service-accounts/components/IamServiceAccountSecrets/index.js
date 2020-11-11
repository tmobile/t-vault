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
    secretStatus,
  } = props;
  const [response, setResponse] = useState({ status: 'loading' });
  const [secretsData, setSecretsData] = useState({});
  const [showSecret, setShowSecret] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [writePermission, setWritePermission] = useState(false);
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const [state] = useStateValue();

  /**
   * @function handleClose
   * @description function to handle opening and closing of confirmation modal.
   */
  const handleClose = () => {
    setOpenConfirmationModal(false);
  };

  /**
   * @description function to get the secret once the component loads.
   */

  useEffect(() => {
    setResponse({ status: secretStatus });
  }, [secretStatus]);

  useEffect(() => {
    if (accountSecretData) {
      setSecretsData({ ...accountSecretData });
    }
  }, [accountSecretData]);

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
   * @function onResetConfirmedClicked
   * @description function to reset secret when the confirm is clicked.
   */
  const onResetConfirmedClicked = () => {
    const payload = {};
    setOpenConfirmationModal(false);
    setResponse({ status: 'loading' });
    apiService
      .getIamServiceAccountPassword(accountDetail?.name, payload)
      .then((res) => {
        setResponse({ status: 'success' });
        if (res?.data) {
          setSecretsData(res.data);
          setResponseType(1);
          setToastMessage('Password reset successfully!');
        }
      })
      .catch(() => {
        setResponse({ status: 'success' });
        setResponseType(-1);
        setToastMessage('Unable to reset password!');
      });
  };

  /**
   * @function onResetClicked
   * @description function to open the confirmation modal.
   */
  const onResetClicked = () => {
    setOpenConfirmationModal(true);
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
          open={openConfirmationModal}
          handleClose={handleClose}
          title="Confirmation"
          description="Are you sure you want to rotate the password for this IAM Service Account?"
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
              onClick={() => onResetConfirmedClicked()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {!accountMetaData.isActivated ||
          (!accountDetail.active && (
            <UserList>
              <Icon src={lock} alt="lock" />
              <Secret type="password" viewSecret={showSecret}>
                ****
              </Secret>

              <FolderIconWrap>activate</FolderIconWrap>
            </UserList>
          ))}
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
                  {showSecret ? <VisibilityOffIcon /> : <VisibilityIcon />}
                  <span>{showSecret ? 'Hide Secret' : 'View Secret'}</span>
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
        {response.status === 'error' && (
          <Error description={accountSecretError || 'Something went wrong!'} />
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
