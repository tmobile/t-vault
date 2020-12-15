/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import PropTypes from 'prop-types';
import VisibilityIcon from '@material-ui/icons/Visibility';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import VisibilityOffIcon from '@material-ui/icons/VisibilityOff';
import FileCopyIcon from '@material-ui/icons/FileCopy';
import BackdropLoader from '../../../../../components/Loaders/BackdropLoader';
import Loader from '../../../../../components/Loaders/LoaderSpinner';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../../apiService';
import lock from '../../../../../assets/icon_lock.svg';
import AccessDeniedLogo from '../../../../../assets/accessdenied-logo.svg';
import refreshIcon from '../../../../../assets/refresh-ccw.svg';
import NoSecretsIcon from '../../../../../assets/no-data-secrets.svg';
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
import Folder from '../../../iam-service-accounts/components/Folder';

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

const customStyle = css`
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 2;
  transform: translate(-50%, -50%);
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

const AzureSecrets = (props) => {
  const { azureDetail, azureSecretData, secretResponse } = props;
  const [response, setResponse] = useState({ status: '' });
  const [secretsData, setSecretsData] = useState({});
  const [showSecret, setShowSecret] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [secretsDataLoader, setSecretsDataLoader] = useState(false);
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  /**
   * @function onViewSecretDetails
   * @param {string} folderName
   * @description function to call the secret details api , which fetch the secrets details
   */
  const onViewSecretDetails = (folderName) => {
    setSecretsDataLoader(true);
    setSecretsData({});
    apiService
      .getSecretFolderData(`${azureDetail.name}/${folderName}`)
      .then((res) => {
        setSecretsDataLoader(false);
        setSecretsData(res?.data);
      })
      .catch((err) => {
        if (err.response.data.errors && err.response.data.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setSecretsDataLoader(false);
        setResponseType(-1);
      });
  };

  useEffect(() => {
    setResponse(secretResponse);
  }, [secretResponse]);

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

  const onCopyClicked = (msg) => {
    setToastMessage(msg);
    setResponseType(1);
  };

  const onRotateSecretConfirmedClicked = () => {
    setResponse({ status: 'loading' });
    setOpenConfirmationModal(false);
    const payload = {
      azureSvcAccName: azureDetail.name,
      secretKeyId: secretsData.secretKeyId,
      servicePrincipalId: secretsData.secretKeyId,
      tenantId: secretsData.tenantId,
    };
    apiService
      .rotateSecret(payload)
      .then((res) => {
        setResponse({ status: 'success' });
        setResponseType(1);
        if (res.data.messages && res.data.messages[0]) {
          setToastMessage(res.data.messages[0]);
        }
      })
      .catch((err) => {
        setResponse({ status: 'success' });
        setResponseType(-1);
        if (err.response.data.errors && err.response.data.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
      });
  };

  const handleClose = () => {
    setOpenConfirmationModal(false);
  };

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleClose}
          title="Confirmation"
          description="Are you sure you want to rotate the secret for this Azure SecretKeyId?"
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
              label="Rotate"
              color="secondary"
              onClick={() => onRotateSecretConfirmedClicked()}
              width={isMobileScreen ? '100%' : '45%'}
            />
          }
        />
        {response.status === 'loading' && <Loader customStyle={customStyle} />}

        {secretsDataLoader && <BackdropLoader />}

        {response.status === 'success' &&
          azureSecretData?.folders?.length > 0 &&
          azureSecretData?.folders.map((secret) => (
            <Folder
              key={secret}
              labelValue={secret}
              onClick={() => onViewSecretDetails(secret)}
            >
              {Object.keys(secretsData).length > 0 && !secretsDataLoader && (
                <UserList>
                  <Icon src={lock} alt="lock" />
                  <Span>{secretsData.secretKeyId}</Span>
                  <Secret type="password" viewSecret={showSecret}>
                    {secretsData.secretText}
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
                      <PopperItem onClick={() => setShowSecret(!showSecret)}>
                        {showSecret ? (
                          <VisibilityOffIcon />
                        ) : (
                          <VisibilityIcon />
                        )}
                        <span>
                          {showSecret ? 'Hide Secret' : 'View Secret'}
                        </span>
                      </PopperItem>

                      {azureDetail.access === 'write' && (
                        <PopperItem
                          onClick={() => setOpenConfirmationModal(true)}
                        >
                          <img alt="refersh-ic" src={refreshIcon} />
                          <span>Rotate Secret</span>
                        </PopperItem>
                      )}
                      <CopyToClipboard
                        text={secretsData.secretKeyId}
                        onCopy={() =>
                          onCopyClicked('Secret key is copied to clipboard!')
                        }
                      >
                        <PopperItem>
                          <FileCopyIcon />
                          <span>Copy Secret Key</span>
                        </PopperItem>
                      </CopyToClipboard>
                      <CopyToClipboard
                        text={secretsData.secretText}
                        onCopy={() =>
                          onCopyClicked('Password is copied to clipboard!')
                        }
                      >
                        <PopperItem>
                          <FileCopyIcon />
                          <span>Copy Password</span>
                        </PopperItem>
                      </CopyToClipboard>
                    </PopperElement>
                  </FolderIconWrap>
                </UserList>
              )}
            </Folder>
          ))}
        {response.status === 'error' && (
          <Error description={response.message || 'Something went wrong!'} />
        )}
        {!azureDetail?.name && response.status !== 'loading' && (
          <AccessDeniedWrap>
            <AccessDeniedIcon src={NoSecretsIcon} alt="NoSecretsIcon" />
            <NoPermission>
              Once you onboard a <span>Azure Service Account</span> you’ll be
              able to view <span>Secret</span> all here!
            </NoPermission>
          </AccessDeniedWrap>
        )}
        {response.status === 'inactive' && (
          <AccessDeniedWrap>
            <AccessDeniedIcon src={AccessDeniedLogo} alt="accessDeniedLogo" />
            <NoPermission>
              Please activate the azure service account!
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
            message={toastMessage || 'Something went wrong'}
          />
        )}
      </>
    </ComponentError>
  );
};

AzureSecrets.propTypes = {
  azureDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  azureSecretData: PropTypes.objectOf(PropTypes.any),
  secretResponse: PropTypes.objectOf(PropTypes.any).isRequired,
};

AzureSecrets.defaultProps = {
  azureSecretData: {},
};

export default AzureSecrets;
