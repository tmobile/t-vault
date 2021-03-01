/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React from 'react';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import ConfirmationModal from '../ConfirmationModal';
import ButtonComponent from '../FormFields/ActionButton';
import mediaBreakpoints from '../../breakpoints';

const SuccessAndErrorModal = (props) => {
  const { title, description, handleSuccessAndDeleteModalClose } = props;

  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  return (
    <ComponentError>
      <ConfirmationModal
        size={isMobileScreen ? 'large' : ''}
        open
        handleClose={handleSuccessAndDeleteModalClose}
        title={title}
        description={description}
        confirmButton={
          <ButtonComponent
            label="Close"
            color="secondary"
            onClick={() => handleSuccessAndDeleteModalClose()}
          />
        }
      />
    </ComponentError>
  );
};

SuccessAndErrorModal.propTypes = {
  title: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  handleSuccessAndDeleteModalClose: PropTypes.func.isRequired,
};

export default SuccessAndErrorModal;
