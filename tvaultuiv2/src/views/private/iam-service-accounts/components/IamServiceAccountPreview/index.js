/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ViewIamSvcAccountDetails from './components/ViewIamSvcAccount';
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
  const { refresh, iamServiceAccountDetails, setViewDetails } = props;
  const classes = useStyles();
  const [open] = useState(true);
  const [openModal, setOpenModal] = useState({
    status: '',
    message: '',
    description: '',
  });
  const [status, setStatus] = useState({});

  const isMobileScreen = useMediaQuery(small);

  //   useEffect(() => {}, []);

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
   * @function onRotateSecret
   * @description function when user clicked on rotate secret to roate the secret.
   */

  const onRotateSecret = () => {};

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openModal.status === 'open'}
          handleClose={() => handleCloseConfirmationModal()}
          title={openModal.message}
          description={openModal?.description}
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleCloseConfirmationModal()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
          confirmButton={
            <ButtonComponent
              label="Rotate"
              color="secondary"
              onClick={() => onRotateSecret()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
        <div>
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
                  setViewDetails={setViewDetails}
                />
              )}
            </Fade>
          </Modal>
        </div>
      </>
    </ComponentError>
  );
};

ViewIamServiceAccount.propTypes = {
  refresh: PropTypes.func.isRequired,
  setViewDetails: PropTypes.func.isRequired,
  iamServiceAccountDetails: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default ViewIamServiceAccount;
