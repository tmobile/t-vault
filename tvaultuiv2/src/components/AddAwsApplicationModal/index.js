/* eslint-disable no-nested-ternary */

import React from 'react';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import Fade from '@material-ui/core/Fade';
import { Backdrop } from '@material-ui/core';
import Modal from '@material-ui/core/Modal';
import styled from 'styled-components';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import AddAwsApplication from '../AddAwsApplication';
import mediaBreakpoints from '../../breakpoints';

const ModalWrapper = styled('div')`
  outline: none;
  background-color: ${(props) => props.theme.palette.background.modal};
  width: 69.6rem;
  padding: 6rem;
  margin: auto 0;
  ${mediaBreakpoints.small} {
    padding: 2.5rem 2rem;
  }
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
  const {
    roles,
    handleSaveClick,
    handleCancelClick,
    handleModalClose,
    open,
    isSvcAccount,
    isCertificate,
    isIamAzureSvcAccount,
  } = props;

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
              roles={roles}
              handleSaveClick={handleSaveClick}
              handleCancelClick={handleCancelClick}
              isSvcAccount={isSvcAccount}
              isCertificate={isCertificate}
              isIamAzureSvcAccount={isIamAzureSvcAccount}
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
  isSvcAccount: PropTypes.bool,
  isCertificate: PropTypes.bool,
  isIamAzureSvcAccount: PropTypes.bool,
  roles: PropTypes.objectOf(PropTypes.any),
};
AddAwsApplicationModal.defaultProps = {
  isSvcAccount: false,
  isCertificate: false,
  isIamAzureSvcAccount: false,
  roles: {},
};

export default AddAwsApplicationModal;
