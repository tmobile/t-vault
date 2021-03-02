/* eslint-disable no-nested-ternary */
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
import SuccessAndErrorModal from '../../../../../../../components/SuccessAndErrorModal';

const OffboardDecomissionedConfirmationModal = (props) => {
  const {
    offBoardSvcAccountConfirmation,
    handleSuccessfullConfirmation,
    offBoardSuccessfull,
    handleConfirmationModalClose,
    onServiceAccountOffBoard,
    itemDetail,
    serviceAccountMetaData,
  } = props;
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const Admin = sessionStorage.getItem('isAdmin');
  const Owner = sessionStorage.getItem('username').toLowerCase();
  return (
    <ComponentError>
      <>
        {offBoardSuccessfull && (
          <SuccessAndErrorModal
            title="Offboarding successful!"
            description={Strings.Resources.offBoardSuccessfull}
            handleSuccessAndDeleteModalClose={() =>
              handleSuccessfullConfirmation()
            }
          />
        )}
        {!offBoardSuccessfull &&
          Object.keys(serviceAccountMetaData).length > 0 && (
            <ConfirmationModal
              size={isMobileScreen ? 'large' : ''}
              open={offBoardSvcAccountConfirmation}
              handleClose={handleConfirmationModalClose}
              title="Service Account Decommissioned!"
              description={
                Admin === 'true' ||
                serviceAccountMetaData?.managedBy?.toLowerCase() === Owner
                  ? Strings.Resources.offBoardDecommissionedConfirmation
                  : 'Unable to read the password for the service account since it does not exist in Active Directory.'
              }
              confirmButton={
                <ButtonComponent
                  label={
                    Admin === 'true' ||
                    serviceAccountMetaData?.managedBy?.toLowerCase() === Owner
                      ? 'Offboard'
                      : 'Close'
                  }
                  color="secondary"
                  onClick={() =>
                    Admin === 'true' ||
                    serviceAccountMetaData?.managedBy?.toLowerCase() === Owner
                      ? onServiceAccountOffBoard(itemDetail.name)
                      : handleSuccessfullConfirmation()
                  }
                />
              }
            />
          )}
      </>
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
  serviceAccountMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default OffboardDecomissionedConfirmationModal;
