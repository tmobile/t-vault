/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { useHistory } from 'react-router-dom';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import TextFieldComponent from 'components/FormFields/TextField';
import ButtonComponent from 'components/FormFields/ActionButton';
import SelectComponent from 'components/FormFields/SelectFields';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import safeIcon from '../../../../assets/icon_safe.svg';
import mediaBreakpoints from '../../../../breakpoints';

const { small, smallAndMedium } = mediaBreakpoints;

const ModalWrapper = styled.section`
  background-color: #2a2e3e;
  padding: 5.5rem 6rem 6rem 6rem;
  border: none;
  outline: none;
  width: 69.6rem;
  margin: auto 0;
  ${smallAndMedium} {
    padding: 4.7rem 5rem 5rem 5rem;
  }
  ${small} {
    width: 37.5rem;
    padding: 2rem;
    margin: 0;
  }
  .MuiTypography-h5 {
    ${small} {
      margin-top: 1rem;
    }
  }
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
  margin: 0 0 0 2rem;
  color: #ccc;
  font-size: 1.4rem;
  ${small} {
    font-size: 1.3rem;
  }
`;

const CreateSafeForm = styled.form`
  display: flex;
  flex-direction: column;
  margin-top: 2.8rem;
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
  margin-top: 1.2rem;
  margin-bottom: 0.5rem;
`;

const CancelSaveWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
  ${small} {
    margin-top: 11.3rem;
  }
  button {
    ${small} {
      width: 16.3rem;
      height: 4.5rem;
    }
  }
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${small} {
    margin-right: 1rem;
  }
`;

const useStyles = makeStyles(() => ({
  select: {
    '&.MuiFilledInput-root.Mui-focused': {
      backgroundColor: '#fff',
    },
  },
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflowY: 'auto',
    padding: '10rem 0',
    '@media (max-width: 767.95px)': {
      alignItems: 'unset',
      justifyContent: 'unset',
      padding: '0',
    },
  },
}));

const CreateModal = (props) => {
  const { createSafe } = props;
  const classes = useStyles();
  const [open, setOpen] = useState(true);
  const [type, setType] = useState('Personal');
  const [owner, setOwner] = useState('');
  const [safeName, setSafeName] = useState('');
  const [description, setDescription] = useState('');

  const history = useHistory();

  const [menu] = useState(['Personal', 'Public']);

  const handleClose = () => {
    setOpen(false);
    history.goBack();
  };
  const saveSafes = () => {
    const safeContent = {
      safeName,
      description,
      owner,
      type,
    };
    createSafe(safeContent);
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
                <SelectComponent
                  menu={menu}
                  value={type}
                  classes={classes.select}
                  onChange={(e) => setType(e.target.value)}
                />
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
                <ButtonComponent
                  label="Create"
                  color="secondary"
                  icon="add"
                  onClick={() => saveSafes()}
                />
              </CancelSaveWrapper>
            </CreateSafeForm>
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

CreateModal.propTypes = {
  createSafe: PropTypes.func,
};
CreateModal.defaultProps = {
  createSafe: () => {},
};
export default CreateModal;
