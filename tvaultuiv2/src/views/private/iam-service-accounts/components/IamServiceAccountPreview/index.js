/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import SnackbarComponent from '../../../../../components/Snackbar';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ViewIamSvcAccountDetails from './components/ViewIamSvcAccount';
import apiService from '../../../iam-service-accounts/apiService';
import BackdropLoader from '../../../../../components/Loaders/BackdropLoader';

const { small } = mediaBreakpoints;

const loaderStyle = css`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  color: red;
  z-index: 1;
`;

const useStyles = makeStyles((theme) => ({
  select: {
    '&.MuiFilledInput-root.Mui-focused': {
      backgroundColor: '#fff',
    },
  },
  dropdownStyle: {
    backgroundColor: '#fff',
  },
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflowY: 'auto',
    padding: '10rem 0',
    [theme.breakpoints.down('xs')]: {
      alignItems: 'unset',
      justifyContent: 'unset',
      padding: '0',
      height: '100%',
    },
  },
}));

const ViewIamServiceAccount = (props) => {
  const {
    refresh,
    iamServiceAccountDetails,
    setViewDetails,
    getSecrets,
  } = props;
  const classes = useStyles();
  const [open] = useState(true);
  const [status, setStatus] = useState(null);
  // const [passwordDetails, setPasswordDetails] = useState(null);
  const [openModal, setOpenModal] = useState({
    status: '',
    message: '',
    description: '',
  });

  const isMobileScreen = useMediaQuery(small);

  // toast close handler
  const onToastClose = () => {
    setStatus({});
  };

  /**
   * @function handleCloseConfirmationModal
   * @description function when user clicked cancel of confirmation modal.
   */
  const handleCloseConfirmationModal = () => {
    setOpenModal({ status: '', message: '' });
  };

  /**
   * @function rotateSecret
   * @description function when user clicked on rotate secret to roate the secret.
   */

  const rotateSecret = () => {
    setOpenModal({
      status: 'open',
      message: 'Confirmation!',
      description: 'Are you sure, You want to rotate the passoword?',
    });
  };

  /**
   * @function isActivateIamSvcAcc
   * @description function when user clicked on activate iam service account for the very first time.
   */

  const isActivateIamSvcAcc = () => {
    setOpenModal({
      status: 'open',
      message: 'IAM Service Account Activation!',
      description:
        "During the activation. the password of the LAM service account will be rotated to ensure AWS and T-Vault are in sync If you want to continue with activation now please click the 'ACTIVATE IAM SERVICE ACCOUNT’ button below and make sure to update any services depending on the service account with its new password",
    });
  };

  /**
   * @function activateServiceAccount
   * @description function when user clicked on activate iam service account for the very first time.
   */
  const activateServiceAccount = () => {
    setStatus({ status: 'loading', message: '' });
    apiService
      .activateIamServiceAccount(
        iamServiceAccountDetails?.userName,
        iamServiceAccountDetails?.awsAccountId
      )
      .then(async (res) => {
        setStatus({ status: 'success', message: res?.data?.messages[0] });
        await getSecrets();
        await refresh();
      })
      .catch((err) => {
        setStatus({
          status: 'failed',
          message: err?.response?.data?.errors[0],
        });
      });
  };

  /**
   * @function onRotateSecret
   * @description function when user clicked on rotate secret to rotate the secret.
   */

  const onRotateSecret = () => {
    const payload = {
      accessKeyId: iamServiceAccountDetails?.secret[0]?.accessKeyId,
      accountId: iamServiceAccountDetails?.awsAccountId,
      userName: iamServiceAccountDetails?.userName,
    };
    setStatus({ status: 'loading' });
    setOpenModal({});
    apiService
      .rotateIamServiceAccountPassword(payload)
      .then(async (res) => {
        if (res?.data) {
          setStatus({ status: 'success', message: res.data.messages[0] });
        }
        await getSecrets();
        await refresh();
      })
      .catch((err) => {
        if (err?.response?.data?.errors[0]) {
          setStatus({
            status: 'failed',
            message: err?.response?.data?.errors[0],
          });
        }
      });
  };

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openModal?.status === 'open'}
          handleClose={() => handleCloseConfirmationModal()}
          title={openModal.message}
          description={openModal?.description}
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleCloseConfirmationModal()}
              width={isMobileScreen ? '100%' : '45%'}
            />
          }
          confirmButton={
            <ButtonComponent
              label={
                iamServiceAccountDetails?.isActivated ? 'Rotate' : 'Activate'
              }
              color="secondary"
              onClick={
                iamServiceAccountDetails?.isActivated
                  ? () => onRotateSecret()
                  : () => activateServiceAccount()
              }
              width={isMobileScreen ? '100%' : '45%'}
            />
          }
        />
        <div>
          {!(openModal?.status === 'open') ? (
            <Modal
              aria-labelledby="transition-modal-title"
              aria-describedby="transition-modal-description"
              className={classes.modal}
              onClose={() => setViewDetails(false)}
              open={open}
              closeAfterTransition
              BackdropComponent={Backdrop}
              BackdropProps={{
                timeout: 500,
              }}
            >
              <Fade in={open}>
                {!iamServiceAccountDetails ? (
                  <LoaderSpinner customStyle={loaderStyle} />
                ) : (
                  <ViewIamSvcAccountDetails
                    iamSvcAccountData={iamServiceAccountDetails}
                    isMobileScreen={isMobileScreen}
                    isRotateSecret={rotateSecret}
                    isActivateIamSvcAcc={isActivateIamSvcAcc}
                    setViewDetails={setViewDetails}
                  />
                )}
              </Fade>
            </Modal>
          ) : status?.status === 'loading' ? (
            <BackdropLoader />
          ) : (
            <></>
          )}
        </div>
        {status?.status === 'failed' && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message={status?.message || 'Something went wrong!'}
          />
        )}
        {status?.status === 'success' && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message={status?.message || 'Request Successfull!'}
          />
        )}
      </>
    </ComponentError>
  );
};

ViewIamServiceAccount.propTypes = {
  refresh: PropTypes.func.isRequired,
  setViewDetails: PropTypes.func.isRequired,
  iamServiceAccountDetails: PropTypes.objectOf(PropTypes.any).isRequired,
  getSecrets: PropTypes.func.isRequired,
};

export default ViewIamServiceAccount;
