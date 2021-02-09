/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState } from 'react';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import ConfirmationModal from '../../../../../../../components/ConfirmationModal';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import mediaBreakpoints from '../../../../../../../breakpoints';
import LoaderSpinner from '../../../../../../../components/Loaders/LoaderSpinner';
import apiService from '../../../../apiService';

const DeleteCertificate = (props) => {
  const {
    deleteModalOpen,
    handleDeleteConfirmationModalClose,
    loaderStyle,
    onDeletionSuccess,
    certificateData,
    onCloseDelete,
  } = props;
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const [deleteResponse, setDeleteResponse] = useState(false);
  const [loading, setLoading] = useState(false);
  const [deleteModalDetail, setDeleteModalDetail] = useState({
    title: 'Delete Certificate',
    desc: 'Are you sure you want to delete this certificate?',
  });

  /**
   * @function onCertificateDeleteConfirm
   * @description function to perform the delete of certificate.
   */
  const onCertificateDeleteConfirm = () => {
    setDeleteModalDetail({ title: '', desc: '' });
    setLoading(true);
    apiService
      .deleteCertificate(
        certificateData.certificateName,
        `${certificateData.certType}`
      )
      .then((res) => {
        if (res?.data?.messages && res.data.messages[0]) {
          setDeleteModalDetail({
            title: 'Deletion Successful',
            desc: res.data.messages[0],
          });
        }
        setLoading(false);
        setDeleteResponse(true);
        onDeletionSuccess();
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setDeleteModalDetail({
            title: 'Error',
            desc: err.response.data.errors[0],
          });
        } else {
          setDeleteModalDetail({
            title: 'Error',
            desc: 'Something went wrong!',
          });
        }
        setDeleteResponse(true);
        setLoading(false);
      });
  };

  return (
    <ComponentError>
      <>
        {loading && (
          <ConfirmationModal
            open
            handleClose={() => {}}
            title=""
            description=""
            confirmButton={<LoaderSpinner customStyle={loaderStyle} />}
          />
        )}
        {!loading && (
          <ConfirmationModal
            size={isMobileScreen ? 'large' : ''}
            open={deleteModalOpen}
            handleClose={
              deleteResponse
                ? onCloseDelete
                : handleDeleteConfirmationModalClose
            }
            title={deleteModalDetail.title}
            description={deleteModalDetail.desc}
            cancelButton={
              !deleteResponse && (
                <ButtonComponent
                  label="Cancel"
                  color="primary"
                  onClick={() => handleDeleteConfirmationModalClose()}
                />
              )
            }
            confirmButton={
              <ButtonComponent
                label={deleteResponse ? 'Close' : 'Confirm'}
                color="secondary"
                onClick={() =>
                  deleteResponse
                    ? onCloseDelete()
                    : onCertificateDeleteConfirm()
                }
              />
            }
          />
        )}
      </>
    </ComponentError>
  );
};

DeleteCertificate.propTypes = {
  deleteModalOpen: PropTypes.bool.isRequired,
  handleDeleteConfirmationModalClose: PropTypes.func.isRequired,
  loaderStyle: PropTypes.arrayOf(PropTypes.any).isRequired,
  onDeletionSuccess: PropTypes.func.isRequired,
  certificateData: PropTypes.objectOf(PropTypes.any).isRequired,
  onCloseDelete: PropTypes.func.isRequired,
};

export default DeleteCertificate;
