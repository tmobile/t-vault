import React from 'react';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import Fade from '@material-ui/core/Fade';
import { Backdrop } from '@material-ui/core';
import Modal from '@material-ui/core/Modal';
import styled from 'styled-components';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import AddAwsApplication from '../AddAwsApplication';

const ModalWrapper = styled('div')`
  outline: none;
  background-color: #2a2e3e;
  width: 69.6rem;
  padding: 6rem;
`;
const useStyles = makeStyles((theme) => ({
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

const AddAwsApplicationModal = (props) => {
  const { handleSaveClick, handleCancelClick, handleModalClose, open } = props;

  const classes = useStyles();

  return (
    <ComponentError>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={open}
        onClose={handleModalClose}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <Fade in={open}>
          <ModalWrapper>
            <AddAwsApplication
              handleSaveClick={handleSaveClick}
              handleCancelClick={handleCancelClick}
            />
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

AddAwsApplicationModal.propTypes = {
  handleSaveClick: PropTypes.func.isRequired,
  handleCancelClick: PropTypes.func.isRequired,
  handleModalClose: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
};
export default AddAwsApplicationModal;
