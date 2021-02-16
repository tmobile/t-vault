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

const OffboardDecomissionedConfirmationModal = (props) => {
  const {
    offBoardSvcAccountConfirmation,
    handleSuccessfullConfirmation,
    offBoardSuccessfull,
    handleConfirmationModalClose,
    onServiceAccountOffBoard,
    itemDetail,
  } = props;
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  return (
    <ComponentError>
      <ConfirmationModal
        size={isMobileScreen ? 'large' : ''}
        open={offBoardSvcAccountConfirmation}
        handleClose={
          offBoardSuccessfull
            ? handleSuccessfullConfirmation
            : handleConfirmationModalClose
        }
        title={offBoardSuccessfull ? 'Offboarding successful!' : 'Confirmation'}
        description={
          offBoardSuccessfull
            ? Strings.Resources.offBoardSuccessfull
            : Strings.Resources.offBoardDecommissionedConfirmation
        }
        confirmButton={
          <ButtonComponent
            label={offBoardSuccessfull ? 'Close' : 'Offboard'}
            color="secondary"
            onClick={() =>
              offBoardSuccessfull
                ? handleSuccessfullConfirmation()
                : onServiceAccountOffBoard(itemDetail.name)
            }
          />
        }
      />
    </ComponentError>
  );
};

OffboardDecomissionedConfirmationModal.propTypes = {
  offBoardSvcAccountConfirmation: PropTypes.bool.isRequired,
  handleSuccessfullConfirmation: PropTypes.func.isRequired,
  offBoardSuccessfull: PropTypes.bool.isRequired,
  onServiceAccountOffBoard: PropTypes.func.isRequired,
  handleConfirmationModalClose: PropTypes.func.isRequired,
  itemDetail: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default OffboardDecomissionedConfirmationModal;
