/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React from 'react';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import ConfirmationModal from '../../../../../../../components/ConfirmationModal';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import mediaBreakpoints from '../../../../../../../breakpoints';
import Strings from '../../../../../../../resources';

const DeletionConfirmationModal = (props) => {
  const {
    certificateData,
    openDeleteConfirmation,
    handleDeleteConfirmationModalClose,
    onCertificateDeleteConfirm,
  } = props;

  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  const title = certificateData.certificateStatus
    ? 'Confirmation'
    : 'Certificate Status';
  const description = certificateData.certificateStatus
    ? 'Are you sure you want to delete this certificate?'
    : Strings.Resources.noTransferOwnerAvailable;
  return (
    <ComponentError>
      <ConfirmationModal
        size={isMobileScreen ? 'large' : ''}
        open={openDeleteConfirmation}
        handleClose={handleDeleteConfirmationModalClose}
        title={title}
        description={description}
        cancelButton={
          <ButtonComponent
            label="Cancel"
            color={certificateData.certificateStatus ? 'primary' : 'secondary'}
            onClick={() => handleDeleteConfirmationModalClose()}
          />
        }
        confirmButton={
          certificateData.certificateStatus && (
            <ButtonComponent
              label="Confirm"
              color="secondary"
              onClick={() => onCertificateDeleteConfirm()}
            />
          )
        }
      />
    </ComponentError>
  );
};

DeletionConfirmationModal.propTypes = {
  openDeleteConfirmation: PropTypes.bool.isRequired,
  handleDeleteConfirmationModalClose: PropTypes.func.isRequired,
  onCertificateDeleteConfirm: PropTypes.func.isRequired,
  certificateData: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default DeletionConfirmationModal;
