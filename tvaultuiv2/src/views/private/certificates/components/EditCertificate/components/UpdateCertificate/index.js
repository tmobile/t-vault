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

const UpdateCertificate = (props) => {
  const {
    updateModalOpen,
    handleUpdateConfirmationModalClose,
    loaderStyle,
    onUpdationSuccess,
    updatePayload,
    onCloseUpdate,
  } = props;
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const [deleteResponse, setDeleteResponse] = useState(false);
  const [loading, setLoading] = useState(false);
  const [deleteDetail, setModalDetail] = useState({
    title: 'Update Confirmation',
    desc: 'Are you sure you want to update this certificate details?',
  });

  /**
   * @function onCertificateUpdateaConfirm
   * @description function to perform to update the certificate.
   */
  const onCertificateUpdateaConfirm = () => {
    setModalDetail({ title: '', desc: '' });
    setLoading(true);
    apiService
      .updateCert(updatePayload)
      .then((res) => {
        if (res?.data?.messages && res.data.messages[0]) {
          setModalDetail({
            title: 'Successful',
            desc: res.data.messages[0],
          });
        }
        setLoading(false);
        setDeleteResponse(true);
        onUpdationSuccess();
      })
      .catch((err) => {
        if (err.response.data.errors && err.response.data.errors[0]) {
          setModalDetail({
            title: 'Error',
            desc: err.response.data.errors[0],
          });
        } else {
          setModalDetail({
            title: 'Error',
            desc: 'Something went wrong!',
          });
        }
        setLoading(false);
        setDeleteResponse(true);
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
            open={updateModalOpen}
            handleClose={
              deleteResponse
                ? onCloseUpdate
                : handleUpdateConfirmationModalClose
            }
            title={deleteDetail.title}
            description={deleteDetail.desc}
            cancelButton={
              !deleteResponse && (
                <ButtonComponent
                  label="Cancel"
                  color="primary"
                  onClick={() => handleUpdateConfirmationModalClose()}
                />
              )
            }
            confirmButton={
              <ButtonComponent
                label={deleteResponse ? 'Close' : 'Confirm'}
                color="secondary"
                onClick={() =>
                  deleteResponse
                    ? onCloseUpdate()
                    : onCertificateUpdateaConfirm()
                }
              />
            }
          />
        )}
      </>
    </ComponentError>
  );
};

UpdateCertificate.propTypes = {
  updateModalOpen: PropTypes.bool.isRequired,
  handleUpdateConfirmationModalClose: PropTypes.func.isRequired,
  loaderStyle: PropTypes.arrayOf(PropTypes.any).isRequired,
  onUpdationSuccess: PropTypes.func.isRequired,
  updatePayload: PropTypes.objectOf(PropTypes.any).isRequired,
  onCloseUpdate: PropTypes.func.isRequired,
};

export default UpdateCertificate;
