import React from 'react';
import PropTypes from 'prop-types';
import { Backdrop } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import styled from 'styled-components';
import CreateSecret from '../CreateSecrets';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';

// styled components here
const ModalWrapper = styled('div')`
  outline: none;
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
const CreateSecretModal = (props) => {
  const {
    openModal,
    setOpenModal,
    handleSecretSave,
    handleSecretCancel,
    parentId,
    secretprefilledData,
  } = props;

  const classes = useStyles();
  const handleClose = () => {
    setOpenModal(false);
  };
  return (
    <ComponentError>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={openModal}
        onClose={() => handleClose()}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <ModalWrapper>
          <CreateSecret
            parentId={parentId}
            handleSecretCancel={handleSecretCancel}
            handleSecretSave={handleSecretSave}
            secretprefilledData={secretprefilledData}
          />
        </ModalWrapper>
      </Modal>
    </ComponentError>
  );
};

CreateSecretModal.propTypes = {
  openModal: PropTypes.bool,
  setOpenModal: PropTypes.func,
  handleSecretSave: PropTypes.func,
  handleSecretCancel: PropTypes.func,
  parentId: PropTypes.string,
  secretprefilledData: PropTypes.objectOf(PropTypes.any),
};
CreateSecretModal.defaultProps = {
  openModal: false,
  setOpenModal: () => {},
  handleSecretSave: () => {},
  handleSecretCancel: () => {},
  parentId: '',
  secretprefilledData: {},
};

export default CreateSecretModal;
