/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import PropTypes from 'prop-types';
import VisibilityIcon from '@material-ui/icons/Visibility';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import VisibilityOffIcon from '@material-ui/icons/VisibilityOff';
import FileCopyIcon from '@material-ui/icons/FileCopy';
import ReportProblemOutlinedIcon from '@material-ui/icons/ReportProblemOutlined';
import Loader from '../../../../../components/Loaders/LoaderSpinner';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../../apiService';
import lock from '../../../../../assets/icon_lock.svg';
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
import Strings from '../../../../../resources';

const UserList = styled.div`
  display: flex;
  align-items: center;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0;
  position: relative;
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

const InfoWrapper = styled.div`
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: space-between;
  ${mediaBreakpoints.semiMedium} {
    flex-direction: column;
  }
`;

const Secret = styled.div`
  -webkit-text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  font-size: 1.2rem;
  color: #5a637a;
  word-break: break-all;
  margin: 0px 2rem;
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
  width: 33%;
  text-align: center;
  ${mediaBreakpoints.semiMedium} {
    width: 100%;
    margin: 1rem;
  }
`;

const Span = styled('span')`
  ${mediaBreakpoints.semiMedium} {
    text-align: center;
  }
`;

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

const NoSecretsContaner = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
`;

const NoSecretsdispIcon = styled.img`
  width: 16rem;
  height: 16rem;
`;

const NoData = styled.div`
  color: #5a637a;
  text-align: center;
  margin-top: 2rem;
  span {
    display: contents;
    margin: 0 0.3rem;
    color: #fff;
  }
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

const LabelWrap = styled.div`
  display: flex;
  align-items: center;
  padding-left: 2rem;
  span {
    margin-left: 1rem;
  }
`;

const SecretDetailsWrap = styled.div`
  display: flex;
  align-items: center;
  width: 90%;
`;

const formatDate = (expiryDate = '') => {
  const expirationArr = new Date(expiryDate).toDateString().split(' ');
  if (expirationArr.length > 3) {
    expirationArr.splice(3, 0, ',');
    const expiryFormattedDate = expirationArr.splice(1).join(' ');

    return expiryFormattedDate;
  }
  return null;
};

const AzureSecrets = (props) => {
  const {
    azureDetail,
    azureSecretData,
    value,
    azureMetaData,
    secretResponse,
    refresh,
  } = props;
  const [response, setResponse] = useState({ status: '' });
  const [secretsData, setSecretsData] = useState({});
  const [showSecret, setShowSecret] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [modalDetail, setModalDetail] = useState({ title: '', desc: '' });
  const [activateAction, setActivateAction] = useState({
    action: false,
    response: false,
  });
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  /**
   * @function onViewSecretDetails
   * @param {string} folderName
   * @description function to call the secret details api , which fetch the secrets details
   */
  const onViewSecretDetails = useCallback(() => {
    setSecretsData({});
    setResponse({ status: 'loading' });
    setShowSecret(false);
    return apiService
      .getSecretFolderData(
        `${azureSecretData?.servicePrincipalName}/${azureSecretData?.folders[0]}`
      )
      .then((res) => {
        setSecretsData(res?.data);
        setResponse({ status: 'success' });
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setResponse({ status: 'error' });
        setResponseType(-1);
      });
  }, [azureSecretData]);

  useEffect(() => {
    setShowSecret(false);
  }, [value]);
  useEffect(() => {
    if (
      azureSecretData &&
      Object.keys(azureSecretData).length > 0 &&
      secretResponse.status !== 'error'
    ) {
      onViewSecretDetails();
    } else {
      setSecretsData({});
    }
    // eslint-disable-next-line
  }, [azureSecretData, onViewSecretDetails]);

  useEffect(() => {
    setResponse({
      status: secretResponse.status,
      message: secretResponse.message,
    });
    if (Object.keys(azureDetail).length === 0) {
      setSecretsData({});
    }
    setShowSecret(false);
  }, [secretResponse, azureDetail]);

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

  const onRotateSecret = () => {
    setOpenConfirmationModal(true);
    setModalDetail({
      title: 'Confirmation',
      desc:
        'Are you sure you want to rotate the secret for this Azure SecretKeyId?',
    });
  };

  const onRotateSecretConfirmedClicked = () => {
    setResponse({ status: 'loading' });
    setOpenConfirmationModal(false);
    const payload = {
      azureSvcAccName: azureDetail.name,
      secretKeyId: secretsData.secretKeyId,
      servicePrincipalId: secretsData.secretKeyId,
      tenantId: secretsData.tenantId,
      expiryDurationMs: secretsData.expiryDateEpoch,
    };
    apiService
      .rotateSecret(payload)
      .then(async (res) => {
        setResponseType(1);
        if (res.data.messages && res.data.messages[0]) {
          setToastMessage(res.data.messages[0]);
        }
        await onViewSecretDetails();
      })
      .catch((err) => {
        setResponse({ status: 'success' });
        setResponseType(-1);
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
      });
  };

  const handleClose = async () => {
    setOpenConfirmationModal(false);
    setModalDetail({ title: '', desc: '' });
    if (activateAction.response) {
      setResponse({ status: 'loading' });
      await refresh();
    }
    setActivateAction({ action: false, response: false });
  };

  const activateServiceAccount = () => {
    setOpenConfirmationModal(true);
    setModalDetail({
      title: 'Confirm Activation',
      desc: Strings.Resources.azureActivateConfirmation,
    });
    setActivateAction({ action: true, response: false });
  };

  const onActivateConfirmedClicked = () => {
    setResponse({ status: 'loading' });
    setOpenConfirmationModal(false);
    apiService
      .activateAzureAccount(azureDetail.name)
      .then(() => {
        setResponse({ status: 'success' });
        setActivateAction({ action: true, response: true });
        setOpenConfirmationModal(true);
        setModalDetail({
          title: 'Activation Successful',
          desc:
            'Azure Service Principal has been activated. You may also want to assign permissions for other users or groups to view or modify this service account.',
        });
      })
      .catch((err) => {
        setResponse({ status: 'success' });
        setActivateAction({ action: false, response: false });
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setResponseType(-1);
      });
  };

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleClose}
          title={modalDetail.title}
          description={modalDetail.desc}
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleClose()}
              width={isMobileScreen ? '100%' : '45%'}
            />
          }
          confirmButton={
            !activateAction.response && (
              <ButtonComponent
                label={activateAction.action ? 'Activate' : 'Rotate'}
                color="secondary"
                onClick={() =>
                  activateAction.action
                    ? onActivateConfirmedClicked()
                    : onRotateSecretConfirmedClicked()
                }
                width={isMobileScreen ? '100%' : '45%'}
              />
            )
          }
        />
        {response.status === 'loading' && <Loader customStyle={customStyle} />}
        {response.status === 'success' && azureMetaData.isActivated && (
          <>
            {Object.keys(secretsData).length > 0 ? (
              <UserList>
                <SecretDetailsWrap>
                  <Icon src={lock} alt="lock" />
                  <InfoWrapper>
                    <Span>{secretsData.secretKeyId}</Span>
                    <SecretInputfield
                      type={showSecret ? 'text' : 'password'}
                      value={secretsData.secretText}
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
                    <PopperItem onClick={() => setShowSecret(!showSecret)}>
                      {showSecret ? <VisibilityOffIcon /> : <VisibilityIcon />}
                      <span>{showSecret ? 'Hide Secret' : 'View Secret'}</span>
                    </PopperItem>

                    {azureDetail.access === 'write' && (
                      <PopperItem onClick={() => onRotateSecret()}>
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
            ) : (
              <NoSecretsContaner>
                <NoSecretsdispIcon src={NoSecretsIcon} alt="NoSecretsIcon" />
                <NoData>No secrets available!</NoData>
              </NoSecretsContaner>
            )}
          </>
        )}
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
        {!azureMetaData.isActivated &&
          response.status === 'success' &&
          azureDetail.name && (
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
  azureMetaData: PropTypes.objectOf(PropTypes.any),
  secretResponse: PropTypes.objectOf(PropTypes.any).isRequired,
  refresh: PropTypes.func.isRequired,
  value: PropTypes.number,
};

AzureSecrets.defaultProps = {
  azureSecretData: {},
  azureMetaData: {},
  value: 0,
};

export default AzureSecrets;
