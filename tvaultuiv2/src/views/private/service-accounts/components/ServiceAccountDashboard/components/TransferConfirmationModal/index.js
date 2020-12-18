/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React from 'react';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import Strings from '../../../../../../../resources';
import ConfirmationModal from '../../../../../../../components/ConfirmationModal';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import mediaBreakpoints from '../../../../../../../breakpoints';

const TransferConfirmationModal = (props) => {
  const {
    transferSvcAccountConfirmation,
    onTranferConfirmationClicked,
    onTransferOwnerCancelClicked,
    transferResponse,
    transferResponseDesc,
  } = props;
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  return (
    <ComponentError>
      <ConfirmationModal
        size={isMobileScreen ? 'large' : ''}
        open={transferSvcAccountConfirmation}
        handleClose={onTransferOwnerCancelClicked}
        title="Confirmation"
        description={
          transferResponse
            ? transferResponseDesc
            : Strings.Resources.transferConfirmation
        }
        cancelButton={
          !transferResponse && (
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => onTransferOwnerCancelClicked()}
            />
          )
        }
        confirmButton={
          <ButtonComponent
            label={transferResponse ? 'Close' : 'Confirm'}
            color="secondary"
            onClick={() =>
              transferResponse
                ? onTransferOwnerCancelClicked()
                : onTranferConfirmationClicked()
            }
          />
        }
      />
    </ComponentError>
  );
};

TransferConfirmationModal.propTypes = {
  transferSvcAccountConfirmation: PropTypes.bool.isRequired,
  onTransferOwnerCancelClicked: PropTypes.func.isRequired,
  onTranferConfirmationClicked: PropTypes.func.isRequired,
  transferResponseDesc: PropTypes.string,
  transferResponse: PropTypes.bool,
};

TransferConfirmationModal.defaultProps = {
  transferResponseDesc: 'Transfer of ownership is successful!',
  transferResponse: false,
};

export default TransferConfirmationModal;
