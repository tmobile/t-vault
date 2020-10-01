import React from 'react';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import { Backdrop } from '@material-ui/core';
import Modal from '@material-ui/core/Modal';
import styled from 'styled-components';
import AddFolder from '../AddFolder';
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
const AddFolderModal = (props) => {
  const {
    openModal,
    setOpenModal,
    childrens,
    handleSaveClick,
    handleCancelClick,
    parentId,
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
          <AddFolder
            childrens={childrens}
            handleSaveClick={handleSaveClick}
            handleCancelClick={handleCancelClick}
            parentId={parentId}
          />
        </ModalWrapper>
      </Modal>
    </ComponentError>
  );
};
AddFolderModal.propTypes = {
  openModal: PropTypes.bool,
  setOpenModal: PropTypes.func,
  childrens: PropTypes.arrayOf(PropTypes.object),
  handleSaveClick: PropTypes.func,
  handleCancelClick: PropTypes.func,
  parentId: PropTypes.string,
};
AddFolderModal.defaultProps = {
  openModal: false,
  setOpenModal: () => {},
  childrens: [],
  handleSaveClick: () => {},
  handleCancelClick: () => {},
  parentId: '',
};
export default AddFolderModal;
