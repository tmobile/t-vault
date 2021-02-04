/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import { useHistory } from 'react-router-dom';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import apiService from '../../apiService';
import ViewCertificate from './components/ViewCertificate';
import { getDaysDifference } from '../../../../../services/helper-function';
import RevokeCertificate from './components/RevokeCertificate';
import DeleteCertificate from './components/DeleteCertificate';
import UpdateCertificate from './components/UpdateCertificate';

const StyledModal = styled(Modal)`
  @-moz-document url-prefix() {
    .MuiBackdrop-root {
      position: absolute;
      height: 215rem;
    }
  }
`;

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

const EditCertificate = (props) => {
  const { refresh } = props;
  const classes = useStyles();
  const [modalDetail, setModalDetail] = useState({
    title: '',
    description: '',
  });
  const [open, setOpen] = useState(true);
  const [certificateData, setCertificateData] = useState({});
  const [openModal, setOpenModal] = useState({ status: 'edit' });
  const [loading, setLoading] = useState(true);
  const [showRevokeRenewBtn, setShowRevokeRenewBtn] = useState(true);
  const [revokeMenu, setRevokeMenu] = useState([]);
  const [allRevokeReason, setAllRevokeReason] = useState([]);
  const [editActionPerform, setEditActionPerform] = useState(false);
  const [updatePayload, setUpdatePayload] = useState({});
  const [renewPossible, setRenewPossible] = useState(true);
  const isMobileScreen = useMediaQuery(small);
  const history = useHistory();

  useEffect(() => {
    if (history?.location?.state?.certificate) {
      setCertificateData({ ...history.location.state.certificate });
    } else {
      history.goBack();
    }
  }, [history]);

  /**
   * @function clearModalDetail
   * @description function to clear modal detail.
   */
  const clearModalDetail = () => {
    setModalDetail({ title: '', description: '' });
  };

  /**
   * @function checkCertStatus
   * @description function to check the status of revoked certificate.
   */
  const checkCertStatus = () => {
    let url = '';
    if (certificateData.certificateStatus === 'Revoked') {
      url = `/sslcert/checkstatus/${certificateData.certificateName}/${certificateData.certType}`;
    } else {
      url = `/sslcert/validate/${certificateData.certificateName}/${certificateData.certType}`;
    }
    apiService
      .checkCertificateStatus(url)
      .then((res) => {
        if (
          res?.data?.messages &&
          res.data.messages[0] === 'Certifictae is in Revoked status '
        ) {
          setShowRevokeRenewBtn(false);
        } else {
          setShowRevokeRenewBtn(true);
        }
        setLoading(false);
        setOpenModal({ status: 'edit' });
        clearModalDetail();
      })
      .catch((err) => {
        if (
          err?.response?.data?.errors &&
          err.response.data.errors[0] !==
            'Certificate is in Revoke Requested status'
        ) {
          setLoading(false);
          setModalDetail({
            title: 'Certificate Status',
            description: err.response.data.errors[0],
          });
        } else if (
          err?.response?.data?.errors[0] ===
          'Certificate is in Revoke Requested status'
        ) {
          setShowRevokeRenewBtn(true);
          setLoading(false);
          setOpenModal({ status: 'edit' });
          clearModalDetail();
        }
      });
  };

  const constructConfirmationMessage = () => {
    const desc = `Certificate expiring in ${getDaysDifference(
      certificateData.expiryDate
    )} Days . Do you want 
    to renew this certificate?`;
    setModalDetail({
      title: 'Renew Confirmation',
      description: desc,
    });
    setRenewPossible(true);
  };

  /**
   * @function onCertRenewClicked
   * @description function when user clicked the renew certificate calculate the difference.
   */
  const onCertRenewClicked = () => {
    clearModalDetail();
    setOpenModal({ status: 'renew' });

    if (certificateData.certType === 'external') {
      const diff = getDaysDifference(certificateData.createDate);
      if (diff < 30) {
        setModalDetail({
          title: 'Confirmation',
          description:
            'External certificate can be renewed only after a month of certificate creation',
        });
        setRenewPossible(false);
      } else {
        constructConfirmationMessage();
      }
    } else {
      constructConfirmationMessage();
    }
  };

  /**
   * @function onRenewConfirmClicked
   * @description function when user clicked the renew certificate Confirmation.
   */
  const onRenewConfirmClicked = () => {
    setLoading(true);
    setOpenModal({ status: 'confirm' });
    clearModalDetail();
    apiService
      .certificateRenew(
        certificateData.certType,
        certificateData.certificateName
      )
      .then((res) => {
        if (res?.data?.messages && res.data.messages[0]) {
          setModalDetail({
            title: 'Successful',
            description: res.data.messages[0],
          });
        }
        setLoading(false);
        setEditActionPerform(true);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setModalDetail({
            title: 'Error',
            description: err.response.data.errors[0],
          });
        }
        setLoading(false);
      });
  };

  /**
   * @function onCloseRenewConfirmation
   * @description function when user clicked the renew certificate cancel open the
   * edit certificate.
   */
  const onCloseRenewConfirmation = () => {
    setOpenModal({ status: 'edit' });
    clearModalDetail();
  };

  useEffect(() => {
    if (Object.keys(certificateData).length > 0) {
      if (
        certificateData.certificateStatus === 'Revoked' ||
        !certificateData.certificateStatus
      ) {
        setOpenModal({ status: 'confirm' });
        setLoading(true);
        checkCertStatus();
      }
      setEditActionPerform(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [certificateData]);

  const closeEditModal = async () => {
    setOpen(false);
    history.goBack();
    await refresh(editActionPerform);
  };

  /**
   * @function handleCloseConfirmationModal
   * @description function when user clicked cancel of confirmation modal.
   */
  const handleCloseConfirmationModal = () => {
    if (!loading) {
      setOpenModal({ status: '' });
      closeEditModal();
      setOpen(false);
    }
  };

  /**
   * @function backToEdit
   * @description function to take the user back to edit mode.
   */
  const backToEdit = () => {
    setOpenModal({ status: 'edit' });
  };

  /**
   * @function getRevokeReasons
   * @description function to get the revoke reason.
   */
  const getRevokeReasons = () => {
    apiService
      .getRevokeReason(certificateData.certificateId)
      .then((res) => {
        if (res?.data?.reasons) {
          setAllRevokeReason([...res.data.reasons]);
          res.data.reasons.map((item) => {
            return setRevokeMenu((prev) => [...prev, item.displayName]);
          });
          setOpenModal({ status: 'revoke' });
          setLoading(false);
        }
      })
      .catch((err) => {
        if (err.response.data.errors && err.response.data.errors[0]) {
          setModalDetail({
            title: 'Error',
            description: err.response.data.errors[0],
          });
        } else {
          setModalDetail({
            title: 'Error',
            description: 'Something went wrong!',
          });
        }
        setOpenModal({ status: 'confirm' });
        setLoading(false);
      });
  };

  /**
   * @function onCertRevokeClicked
   * @description function to when user clicked the renew and open the renew reason modal.
   */
  const onCertRevokeClicked = async () => {
    clearModalDetail();
    setAllRevokeReason([]);
    setRevokeMenu([]);
    setLoading(true);
    setOpenModal({ status: 'confirm' });
    await getRevokeReasons();
  };

  /**
   * @function onRevokeConfirm
   * @description function to when user clicked the confirmation renew.
   */
  const onRevokeConfirm = (data) => {
    setLoading(true);
    setOpenModal({ status: 'confirm' });
    apiService
      .revokeRequest(
        certificateData.certType,
        certificateData.certificateName,
        data
      )
      .then((res) => {
        if (res?.data?.messages && res.data.messages[0]) {
          setModalDetail({
            title: 'Successful',
            description: res.data.messages[0],
          });
        }
        setLoading(false);
        setEditActionPerform(true);
      })
      .catch((err) => {
        if (err.response.data.errors && err.response.data.errors[0]) {
          setModalDetail({
            title: 'Error',
            description: err.response.data.errors[0],
          });
        }
        setLoading(false);
      });
  };

  /**
   * @function handleRevokeModalClose
   * @description function to close the revoke modal.
   */
  const handleRevokeModalClose = () => {
    setOpenModal({ status: 'edit' });
  };

  const onUpdateCertClicked = (payload) => {
    setOpenModal({ status: 'update' });
    setUpdatePayload(payload);
  };

  const onDeleteClicked = () => {
    setOpenModal({ status: 'delete' });
  };
  return (
    <ComponentError>
      <>
        {open && (
          <ConfirmationModal
            open={openModal.status === 'confirm'}
            handleClose={handleCloseConfirmationModal}
            title={modalDetail.title}
            description={modalDetail.description}
            confirmButton={
              !loading ? (
                <ButtonComponent
                  label="Close"
                  color="secondary"
                  onClick={() => handleCloseConfirmationModal()}
                  width={isMobileScreen ? '100%' : '45%'}
                />
              ) : (
                <LoaderSpinner customStyle={loaderStyle} />
              )
            }
          />
        )}
        <ConfirmationModal
          open={openModal.status === 'renew'}
          handleClose={onCloseRenewConfirmation}
          title={modalDetail.title}
          description={modalDetail.description}
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => onCloseRenewConfirmation()}
              width={isMobileScreen ? '100%' : '45%'}
            />
          }
          confirmButton={
            renewPossible && (
              <ButtonComponent
                label="Renew"
                color="secondary"
                onClick={() => onRenewConfirmClicked()}
                width={isMobileScreen ? '100%' : '45%'}
              />
            )
          }
        />
        {openModal.status === 'update' && (
          <UpdateCertificate
            handleUpdateConfirmationModalClose={() => backToEdit()}
            onCloseUpdate={() => closeEditModal()}
            updateModalOpen={openModal.status === 'update'}
            onUpdationSuccess={() => setEditActionPerform(true)}
            loaderStyle={loaderStyle}
            updatePayload={updatePayload}
          />
        )}
        {openModal.status === 'delete' && (
          <DeleteCertificate
            handleDeleteConfirmationModalClose={() => backToEdit()}
            onCloseDelete={() => closeEditModal()}
            deleteModalOpen={openModal.status === 'delete'}
            onDeletionSuccess={() => setEditActionPerform(true)}
            loaderStyle={loaderStyle}
            certificateData={certificateData}
          />
        )}
        {openModal.status === 'revoke' && (
          <RevokeCertificate
            revokeModalOpen={openModal.status === 'revoke'}
            revokeMenu={revokeMenu}
            handleRevokeModalClose={handleRevokeModalClose}
            isMobileScreen={isMobileScreen}
            onRevokeConfirm={(data) => onRevokeConfirm(data)}
            allRevokeReason={allRevokeReason}
          />
        )}
        {openModal.status === 'edit' && (
          <StyledModal
            aria-labelledby="transition-modal-title"
            aria-describedby="transition-modal-description"
            className={classes.modal}
            open={open}
            onClose={() => closeEditModal()}
            closeAfterTransition
            BackdropComponent={Backdrop}
            BackdropProps={{
              timeout: 500,
            }}
          >
            <Fade in={open}>
              <ViewCertificate
                certificateData={certificateData}
                onCertRenewClicked={onCertRenewClicked}
                showRevokeRenewBtn={showRevokeRenewBtn}
                onCloseModal={() => closeEditModal()}
                onCertRevokeClicked={onCertRevokeClicked}
                onUpdateCertClicked={onUpdateCertClicked}
                onDeleteClicked={onDeleteClicked}
              />
            </Fade>
          </StyledModal>
        )}
      </>
    </ComponentError>
  );
};

EditCertificate.propTypes = {
  refresh: PropTypes.func.isRequired,
};

export default EditCertificate;
