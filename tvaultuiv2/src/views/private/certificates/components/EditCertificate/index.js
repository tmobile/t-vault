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
import apiService from '../../apiService';
import ViewCertificate from './components/ViewCertificate';
import { getDaysDifference } from '../../../../../services/helper-function';
import RevokeCertificate from './components/RevokeCertificate';

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
  const { open, certificateData, onCloseModal } = props;
  const classes = useStyles();
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  // const [responseTitle, setResponseTitle] = useState('');
  // const [responseDesc, setResponseDesc] = useState('');
  const [modalDetail, setModalDetail] = useState({
    title: '',
    description: '',
  });
  const [loading, setLoading] = useState(true);
  const [showRevokeRenewBtn, setShowRevokeRenewBtn] = useState(true);
  const isMobileScreen = useMediaQuery(small);
  const [openRenewConfirmation, setOpenRenewConfirmation] = useState(false);
  const [actionResponse, setActionResponse] = useState(false);
  const [revokeModalOpen, setRevokeModalOpen] = useState(false);
  const [revokeMenu, setRevokeMenu] = useState([]);
  const [allRevokeReason, setAllRevokeReason] = useState([]);

  const clear = () => {
    setModalDetail({ title: '', description: '' });
  };

  const checkCertStatus = () => {
    apiService
      .checkExtCertificateStatus(
        certificateData.certificateName,
        certificateData.certType
      )
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
        setOpenConfirmationModal(false);
        clear();
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
          setOpenConfirmationModal(false);
          clear();
        }
      });
  };

  const onCertRenewClicked = () => {
    clear();
    setOpenRenewConfirmation(true);
    const diff = getDaysDifference(
      certificateData.createDate,
      certificateData.expiryDate
    );
    const desc = `Certificate expiring in ${diff} Days . Do you want to renew this certificate?`;
    setModalDetail({
      title: 'Renew Confirmation',
      description: desc,
    });
  };

  const onRenewConfirmClicked = () => {
    setLoading(true);
    setOpenConfirmationModal(true);
    setOpenRenewConfirmation(false);
    clear();
    apiService
      .certificateRenew(
        certificateData.certType,
        certificateData.certificateName
      )
      .then((res) => {
        if (res?.data?.messages && res.data.messages[0]) {
          setModalDetail({
            title: 'Successfull',
            description: res.data.messages[0],
          });
        }
        setLoading(false);
        setActionResponse(true);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setModalDetail({
            title: 'Error',
            description: err.response.data.errors[0],
          });
        }
        setLoading(false);
        setActionResponse(true);
      });
  };

  const onCloseRenewConfirmation = () => {
    setOpenRenewConfirmation(false);
    clear();
  };

  useEffect(() => {
    if (certificateData) {
      if (
        certificateData.certificateStatus === 'Revoked' ||
        !certificateData.certificateStatus
      ) {
        setOpenConfirmationModal(true);
        setLoading(true);
        checkCertStatus();
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [certificateData]);

  const handleCloseConfirmationModal = () => {
    if (!loading) {
      setOpenConfirmationModal(false);
      onCloseModal();
    }
  };

  const backToEdit = () => {
    setOpenConfirmationModal(false);
    setActionResponse(false);
  };

  const getRevokeReasons = () => {
    apiService
      .getRevokeReason(certificateData.certificateId)
      .then((res) => {
        if (res?.data?.reasons) {
          setAllRevokeReason([...res.data.reasons]);
          res.data.reasons.map((item) => {
            return setRevokeMenu((prev) => [...prev, item.displayName]);
          });
          setOpenConfirmationModal(false);
          setRevokeModalOpen(true);
          setLoading(false);
        }
      })
      .catch(() => {
        setRevokeModalOpen(false);
        setLoading(false);
        setModalDetail({
          title: 'Error',
          description: 'Something went wrong!',
        });
        setActionResponse(true);
      });
  };

  const onCertRevokeClicked = async () => {
    clear();
    setAllRevokeReason([]);
    setRevokeMenu([]);
    setLoading(true);
    setOpenConfirmationModal(true);
    await getRevokeReasons();
  };

  const onRevokeConfirm = (data) => {
    setLoading(true);
    setRevokeModalOpen(false);
    setOpenConfirmationModal(true);
    apiService
      .revokeRequest(
        certificateData.certType,
        certificateData.certificateName,
        data
      )
      .then((res) => {
        if (res?.data?.messages && res.data.messages[0]) {
          setModalDetail({
            title: 'Successfull',
            description: res.data.messages[0],
          });
        }
        setLoading(false);
        setActionResponse(true);
      })
      .catch((err) => {
        if (err.response.data.errors && err.response.data.errors[0]) {
          setModalDetail({
            title: 'Successfull',
            description: err.response.data.errors[0],
          });
        }
        setLoading(false);
        setActionResponse(true);
      });
  };

  const handleRevokeModalClose = () => {
    setRevokeModalOpen(false);
  };

  return (
    <ComponentError>
      <>
        {open && (
          <ConfirmationModal
            open={openConfirmationModal}
            handleClose={
              actionResponse ? backToEdit : handleCloseConfirmationModal
            }
            title={modalDetail.title}
            description={modalDetail.description}
            confirmButton={
              !loading ? (
                <ButtonComponent
                  label="Close"
                  color="secondary"
                  onClick={() =>
                    actionResponse
                      ? backToEdit()
                      : handleCloseConfirmationModal()
                  }
                  width={isMobileScreen ? '100%' : '38%'}
                />
              ) : (
                <LoaderSpinner customStyle={loaderStyle} />
              )
            }
          />
        )}
        <ConfirmationModal
          open={openRenewConfirmation}
          handleClose={onCloseRenewConfirmation}
          title={modalDetail.title}
          description={modalDetail.description}
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => onCloseRenewConfirmation()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
          confirmButton={
            <ButtonComponent
              label="Renew"
              color="secondary"
              onClick={() => onRenewConfirmClicked()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
        {revokeModalOpen && (
          <RevokeCertificate
            revokeModalOpen={revokeModalOpen}
            revokeMenu={revokeMenu}
            handleRevokeModalClose={handleRevokeModalClose}
            isMobileScreen={isMobileScreen}
            onRevokeConfirm={(data) => onRevokeConfirm(data)}
            allRevokeReason={allRevokeReason}
          />
        )}
        {!openConfirmationModal && !openRenewConfirmation && !revokeModalOpen && (
          <Modal
            aria-labelledby="transition-modal-title"
            aria-describedby="transition-modal-description"
            className={classes.modal}
            open={open}
            onClose={onCloseModal}
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
                isMobileScreen={isMobileScreen}
                showRevokeRenewBtn={showRevokeRenewBtn}
                onCloseModal={onCloseModal}
                onCertRevokeClicked={onCertRevokeClicked}
              />
            </Fade>
          </Modal>
        )}
      </>
    </ComponentError>
  );
};

EditCertificate.propTypes = {
  certificateData: PropTypes.objectOf(PropTypes.any).isRequired,
  open: PropTypes.bool.isRequired,
  onCloseModal: PropTypes.func.isRequired,
};

export default EditCertificate;
