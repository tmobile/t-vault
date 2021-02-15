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
  } = props;

  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  return (
    <ComponentError>
      <ConfirmationModal
        size={isMobileScreen ? 'large' : ''}
        open={openDeleteConfirmation}
        handleClose={handleDeleteConfirmationModalClose}
        title="Confirmation"
        description="Are you sure you want to delete this certificate?"
        cancelButton={
          <ButtonComponent
            label="Cancel"
            color="primary"
            onClick={() => handleDeleteConfirmationModalClose()}
          />
        }
        confirmButton={
          <ButtonComponent
            label="Confirm"
            color="secondary"
            onClick={() => onCertificateDeleteConfirm()}
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
};

export default DeletionConfirmationModal;
