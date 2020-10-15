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
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../apiService';
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

const SecretsList = styled.div`
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
  width: 2.2rem;
  height: 2.2rem;
  margin-right: 1.6rem;
  margin-left: 2rem;
`;

const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  cursor: pointer;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
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

const AppRoleSecrets = (props) => {
  const { accountDetail } = props;
  const [response, setResponse] = useState({ status: 'loading' });
  const [secretsData, setSecretsData] = useState({});
  const [showSecret, setShowSecret] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [status, setStatus] = useState(null);
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
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

  //   useEffect(() => {
  //     setResponse({ status: secretStatus });
  //   }, [secretStatus]);

  //   useEffect(() => {
  //     if (accountSecretData) {
  //       setSecretsData({ ...accountSecretData });
  //     }
  //   }, [accountSecretData]);

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
    setStatus({ status: 'success', message: 'Secret copied to clipboard' });
  };

  /**
   * @function onDeleteClicked
   * @description function to reset secret when the confirm is clicked.
   */
  const onDeleteClicked = () => {
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
          setStatus({
            status: 'success',
            message: 'SecretId deleted successfully',
          });
        }
      })
      .catch((err) => {
        setResponseType(-1);
        setStatus({
          status: 'failed',
          message: err?.response?.data?.errors[0],
        });
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
//    */
  //   useEffect(() => {
  //     if (accountMetaData?.response?.users) {
  //       Object.entries(accountMetaData.response.users).map(([key, value]) => {
  //         if (key === state.username && value === 'write') {
  //           return setWritePermission(true);
  //         }
  //         return setWritePermission(false);
  //       });
  //     }
  //   }, [accountMetaData, state]);

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleClose}
          title="Confirmation"
          description="Are you sure you want to Delete the secretId?"
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
              onClick={() => onDeleteClicked()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
        {status.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {status.status === 'success' && secretsData && (
          <SecretsList>
            <Icon src={lock} alt="lock" />

            <Secret type="password" viewSecret={showSecret}>
              {secretsData.current_password}
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
                <CopyToClipboard
                  text={secretsData.current_password}
                  onCopy={() => onCopyClicked()}
                >
                  <PopperItem>
                    <FileCopyIcon />
                    <span>Copy Secret</span>
                  </PopperItem>
                </CopyToClipboard>
              </PopperElement>
            </FolderIconWrap>
          </SecretsList>
        )}
        {response.status === 'error' && (
          <Error description={"error while fetching secretId's"} />
        )}
        {response.status === 'no-permission' && (
          <NoPermission>There are no secretIds to view here.</NoPermission>
        )}
        {responseType === 1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message={status.message}
          />
        )}
        {responseType === -1 && (
          <SnackbarComponent
            open
            severity="error"
            icon="error"
            onClose={() => onToastClose()}
            message={status.message}
          />
        )}
      </>
    </ComponentError>
  );
};

AppRoleSecrets.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any).isRequired,
};

AppRoleSecrets.defaultProps = {};

export default AppRoleSecrets;
