/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { withRouter } from 'react-router-dom';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled from 'styled-components';
import Select from '@material-ui/core/Select';
import MenuItem from '@material-ui/core/MenuItem';
import PropTypes from 'prop-types';
import TextFieldComponent from 'components/FormFields/TextField';
import ButtonComponent from 'components/FormFields/ActionButton';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import safeIcon from '../../../../assets/icon_safe.svg';

const ModalWrapper = styled.section`
  background-color: #2a2e3e;
  padding: 5.5rem 5.9rem;
  border: none;
  outline: none;
  width: 50%;
  margin: auto 0;
`;

const IconDescriptionWrapper = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
  position: relative;
  margin-top: 3.2rem;
`;

const SafeIcon = styled.img`
  height: 5.7rem;
`;

const SafeDescription = styled.p`
  margin-left: 2rem;
  color: #ccc;
  font-size: 1.4rem;
`;

const CreateSafeForm = styled.form`
  display: flex;
  flex-direction: column;
`;

const InputFieldLabelWrapper = styled.div`
  margin-bottom: 2rem;
  .MuiSelect-icon {
    top: auto;
    color: #000;
  }
`;

const FieldInstruction = styled.p`
  color: #8b8ea6;
  font-size: 1.3rem;
`;

const CancelSaveWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
`;

const useStyles = makeStyles(() => ({
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflowY: 'auto',
    padding: '10rem 0',
  },
}));

const CreateModal = (props) => {
  const { history } = props;
  const classes = useStyles();
  const [open, setOpen] = useState(true);
  const [type, setType] = useState('Personal');
  const [owner, setOwner] = useState('');
  const [safeName, setSafeName] = useState('');
  const [description, setDescription] = useState('');

  const handleClose = () => {
    setOpen(false);
    history.goBack();
  };
  return (
    <ComponentError>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={open}
        onClose={() => handleClose()}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <Fade in={open}>
          <ModalWrapper>
            <Typography variant="h5">Create Safe</Typography>
            <IconDescriptionWrapper>
              <SafeIcon src={safeIcon} alt="safe-icon" />
              <SafeDescription>
                A Safe is a logical unit to store the secrets. All the safes are
                created within Vault. You can control access only at the safe
                level. As a vault administrator you can manage safes but cannot
                view the content of the safe.
              </SafeDescription>
            </IconDescriptionWrapper>
            <CreateSafeForm>
              <InputFieldLabelWrapper>
                <InputLabel>Safe Name</InputLabel>
                <TextFieldComponent
                  value={safeName}
                  placeholder="Save Name"
                  fullWidth
                  onChange={(e) => setSafeName(e.target.value)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabel>Owner</InputLabel>
                <TextFieldComponent
                  placeholder="Owner"
                  value={owner}
                  fullWidth
                  onChange={(e) => setOwner(e.target.value)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabel>Type of Safe</InputLabel>
                <Select
                  labelId="demo-customized-select-label"
                  id="demo-customized-select"
                  value={type}
                  variant="filled"
                  onChange={(e) => setType(e.target.value)}
                >
                  <MenuItem value="Personal">Personal</MenuItem>
                  <MenuItem value="Public">Public</MenuItem>
                </Select>
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabel>Description</InputLabel>
                <TextFieldComponent
                  multiline
                  value={description}
                  fullWidth
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Add some details about this safe"
                />
                <FieldInstruction>
                  Please add a minimum of 10 characters
                </FieldInstruction>
              </InputFieldLabelWrapper>
              <CancelSaveWrapper>
                <CancelButton>
                  <ButtonComponent
                    label="Cancel"
                    color="primary"
                    onClick={() => handleClose()}
                  />
                </CancelButton>
                <ButtonComponent label="Create" color="secondary" icon="add" />
              </CancelSaveWrapper>
            </CreateSafeForm>
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

CreateModal.propTypes = {
  history: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default withRouter(CreateModal);
