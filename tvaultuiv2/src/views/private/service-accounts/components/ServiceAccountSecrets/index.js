/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import Tooltip from '@material-ui/core/Tooltip';
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
import NoSecretsIcon from '../../../../../assets/no-data-secrets.svg';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import mediaBreakpoints from '../../../../../breakpoints';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import accessDeniedLogo from '../../../../../assets/accessdenied-logo.svg';
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
  align-items: center;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0;
  border-bottom: 1px solid #323649;
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
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

const SecretLastUpdated = styled.div`
  width: 88%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-left: 0.6rem;
`;

const LastUpdated = styled.div`
  font-size: 1.2rem;
  color: #5e627c;
  cursor: pointer;
`;

const SecretInputfield = styled.input`
  padding: 0;
  outline: none;
  border: none;
  background: transparent;
  font-size: 1.2rem;
  color: #5a637a;
  word-break: break-all;
  margin-bottom: 0.7rem;
  width: 70%;
  text-align: left;
  ${mediaBreakpoints.semiMedium} {
    width: 100%;
    margin: 1rem 0;
  }
`;

const useTooltipStyles = makeStyles((theme) => ({
  arrow: {
    color: theme.palette.common.white,
  },
  tooltip: {
    backgroundColor: theme.palette.common.white,
    color: theme.palette.common.black,
    fontSize: theme.typography.subtitle2.fontSize,
    textAlign: 'center',
  },
}));

const ServiceAccountSecrets = (props) => {
  const {
    accountDetail,
    accountSecretError,
    value,
    accountSecretData,
    secretStatus,
    hasSvcAccountAcitve,
  } = props;
  const [response, setResponse] = useState({ status: 'loading' });
  const [secretsData, setSecretsData] = useState({});
  const [showSecret, setShowSecret] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const tooltipClasses = useTooltipStyles();

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
    setShowSecret(false);
  }, [accountSecretData]);

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
      .resetServiceAccountPassword(accountDetail?.name, payload)
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

  const getDaysDifference = (end) => {
    if (end) {
      const date1 = new Date();
      const date2 = new Date(end);
      const diffInTime = Math.abs(date2.getTime() - date1.getTime());
      const diffInTimeDays = diffInTime / 1000;
      const time = Math.ceil(diffInTimeDays);

      return time < 60
        ? 'a few seconds ago'
        : time / 60 < 60
        ? `${Math.floor(time / 60)} minutes ago`
        : time / 3600 < 24
        ? `${Math.floor(time / 3600)} hours ago`
        : `${Math.floor(time / (3600 * 24))} days ago`;
    }
    return ' -- ';
  };

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleClose}
          title="Confirmation"
          description="Are you sure you want to reset the password for this Service Account?"
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
              label="Reset"
              color="secondary"
              onClick={() => onResetConfirmedClicked()}
              width={isMobileScreen ? '100%' : '45%'}
            />
          }
        />
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status === 'success' && secretsData && (
          <UserList>
            <Icon src={lock} alt="lock" />
            <SecretLastUpdated>
              <SecretInputfield
                type={showSecret ? 'text' : 'password'}
                value={
                  secretsData?.adServiceAccountCreds?.current_password
                    ? secretsData.adServiceAccountCreds?.current_password
                    : 'Secret not available!'
                }
                readOnly
              />
              <Tooltip
                classes={tooltipClasses}
                arrow
                title={secretsData.modifiedBy}
                placement="top"
              >
                <LastUpdated>
                  Last Updated: {getDaysDifference(secretsData.modifiedAt)}
                </LastUpdated>
              </Tooltip>
            </SecretLastUpdated>
            {secretsData?.adServiceAccountCreds?.current_password && (
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
                  {accountDetail?.access === 'write' && (
                    <PopperItem onClick={() => onResetClicked()}>
                      <img alt="refersh-ic" src={IconRefreshCC} />
                      <span>Reset Secret</span>
                    </PopperItem>
                  )}
                  <CopyToClipboard
                    text={secretsData?.adServiceAccountCreds?.current_password}
                    onCopy={() => onCopyClicked()}
                  >
                    <PopperItem>
                      <FileCopyIcon />
                      <span>Copy Secret</span>
                    </PopperItem>
                  </CopyToClipboard>
                </PopperElement>
              </FolderIconWrap>
            )}
          </UserList>
        )}
        {response.status === 'error' && (
          <Error description={accountSecretError || 'Something went wrong!'} />
        )}
        {(response.status === 'no-permission' ||
          response.status === 'no-data') && (
          <AccessDeniedWrap>
            <AccessDeniedIcon
              src={
                response.status === 'no-data' ? NoSecretsIcon : accessDeniedLogo
              }
              alt="accessDeniedLogo"
            />
            {response.status === 'no-data' ? (
              <NoPermission>
                Once you onboard a <span>Service Account</span> you’ll be able
                to view <span>Secret</span> all here!
              </NoPermission>
            ) : (
              <NoPermission>
                {hasSvcAccountAcitve
                  ? 'You do not have permission to view/reset the secrets.'
                  : 'Please activate the service account!'}
              </NoPermission>
            )}
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

ServiceAccountSecrets.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  accountSecretError: PropTypes.string,
  accountSecretData: PropTypes.objectOf(PropTypes.any),
  secretStatus: PropTypes.string,
  value: PropTypes.number,
  hasSvcAccountAcitve: PropTypes.bool.isRequired,
};

ServiceAccountSecrets.defaultProps = {
  accountSecretError: 'Something went wrong!',
  accountSecretData: {},
  secretStatus: 'loading',
  value: 0,
};

export default ServiceAccountSecrets;
