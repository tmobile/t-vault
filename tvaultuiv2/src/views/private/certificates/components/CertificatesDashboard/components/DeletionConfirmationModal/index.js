/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React from 'react';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import ConfirmationModal from '../../../../../../../components/ConfirmationModal';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import mediaBreakpoints from '../../../../../../../breakpoints';

const DeletionConfirmationModal = (props) => {
  const {
    openDeleteConfirmation,
    handleDeleteConfirmationModalClose,
    onCertificateDeleteConfirm,
    deleteResponse,
    deleteModalDetail,
  } = props;

  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  return (
    <ComponentError>
      <ConfirmationModal
        size={isMobileScreen ? 'large' : ''}
        open={openDeleteConfirmation}
        handleClose={handleDeleteConfirmationModalClose}
        title={deleteModalDetail.title}
        description={deleteModalDetail.description}
        cancelButton={
          !deleteResponse && (
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleDeleteConfirmationModalClose()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          )
        }
        confirmButton={
          <ButtonComponent
            label={deleteResponse ? 'Close' : 'Confirm'}
            color="secondary"
            onClick={() =>
              deleteResponse
                ? handleDeleteConfirmationModalClose()
                : onCertificateDeleteConfirm()
            }
            width={isMobileScreen ? '100%' : '38%'}
          />
        }
      />
    </ComponentError>
  );
};

DeletionConfirmationModal.propTypes = {
  openDeleteConfirmation: PropTypes.bool.isRequired,
  handleDeleteConfirmationModalClose: PropTypes.func.isRequired,
  onCertificateDeleteConfirm: PropTypes.func.isRequired,
  deleteResponse: PropTypes.bool.isRequired,
  deleteModalDetail: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default DeletionConfirmationModal;
