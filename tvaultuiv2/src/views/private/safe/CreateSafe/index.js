/* eslint-disable import/no-unresolved */
import React, { useState, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { useHistory } from 'react-router-dom';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import TextFieldComponent from 'components/FormFields/TextField';
import ButtonComponent from 'components/FormFields/ActionButton';
import SelectComponent from 'components/FormFields/SelectFields';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import safeIcon from 'assets/icon_safe.svg';
import leftArrowIcon from 'assets/left-arrow.svg';
import mediaBreakpoints from 'breakpoints';
import SnackbarComponent from 'components/Snackbar';
import Loader from '../components/Loader';
import apiService from '../apiService';

const { small, smallAndMedium } = mediaBreakpoints;

const ModalWrapper = styled.section`
  background-color: #2a2e3e;
  padding: 5.5rem 6rem 6rem 6rem;
  border: none;
  outline: none;
  width: 69.6rem;
  margin: auto 0;
  display: flex;
  flex-direction: column;
  position: relative;
  ${smallAndMedium} {
    padding: 4.7rem 5rem 5rem 5rem;
  }
  ${small} {
    width: 100%;
    padding: 2rem;
    margin: 0;
    height: fit-content;
  }
`;

const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  ${small} {
    margin-top: 1rem;
  }
`;

const LeftIcon = styled.img`
  display: none;
  ${small} {
    display: block;
    margin-right: 1.4rem;
    margin-top: 0.3rem;
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
    margin-top: 5.3rem;
  }
  button {
    ${small} {
      height: 4.5rem;
    }
  }
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${small} {
    margin-right: 1rem;
    width: 100%;
  }
`;

const loaderStyle = css`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  color: red;
  z-index: 1;
`;

const useStyles = makeStyles((theme) => ({
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
    [theme.breakpoints.down('xs')]: {
      alignItems: 'unset',
      justifyContent: 'unset',
      padding: '0',
      height: '100%',
    },
  },
}));

const CreateModal = () => {
  const classes = useStyles();
  const [open, setOpen] = useState(true);
  const [safeType, setSafeType] = useState('Users Safe');
  const [owner, setOwner] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [disabledSave, setDisabledSave] = useState(true);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const isMobileScreen = useMediaQuery(small);
  const history = useHistory();

  useEffect(() => {
    if (name === '' || owner === '' || description.length < 10) {
      setDisabledSave(true);
    } else {
      setDisabledSave(false);
    }
  }, [name, description, owner]);

  const [menu] = useState(['Users Safe', 'Shared Safe', 'Application Safe']);

  const handleClose = () => {
    setOpen(false);
    history.goBack();
  };

  const saveSafes = () => {
    const value = safeType.split(' ')[0].toLowerCase();
    const safeContent = {
      data: {
        name,
        description,
        type: '',
        owner,
      },
      path: `${value}/${name}`,
    };
    setDisabledSave(true);
    setResponseType(0);
    apiService
      .postApiCall('/vault/v2/ss/sdb', safeContent)
      .then((res) => {
        if (res && res.status === 200) {
          setResponseType(1);
          setTimeout(() => {
            setOpen(false);
            history.goBack();
          }, 1000);
        }
      })
      .catch((err) => {
        if (err.response && err.response.data?.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setResponseType(-1);
      });
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
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
            {responseType === 0 && <Loader customStyle={loaderStyle} />}
            <HeaderWrapper>
              <LeftIcon
                src={leftArrowIcon}
                alt="go-back"
                onClick={() => handleClose()}
              />
              <Typography variant="h5">Create Safe</Typography>
            </HeaderWrapper>
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
                  value={name}
                  placeholder="Save Name"
                  fullWidth
                  onChange={(e) => setName(e.target.value)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabel>Owner</InputLabel>
                <TextFieldComponent
                  placeholder="Owner"
                  value={owner}
                  fullWidth
                  type="email"
                  onChange={(e) => setOwner(e.target.value)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabel>Type of Safe</InputLabel>
                <SelectComponent
                  menu={menu}
                  value={safeType}
                  classes={classes.select}
                  onChange={(e) => setSafeType(e.target.value)}
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
            </CreateSafeForm>
            <CancelSaveWrapper>
              <CancelButton>
                <ButtonComponent
                  label="Cancel"
                  color="primary"
                  onClick={() => handleClose()}
                  width={isMobileScreen ? '100%' : ''}
                />
              </CancelButton>
              <ButtonComponent
                label="Create"
                color="secondary"
                icon="add"
                disabled={disabledSave}
                onClick={() => saveSafes()}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelSaveWrapper>
            {responseType === -1 && (
              <SnackbarComponent
                open
                onClose={() => onToastClose()}
                severity="error"
                icon="error"
                message={toastMessage || 'Something went wrong!'}
              />
            )}
            {responseType === 1 && (
              <SnackbarComponent
                open
                onClose={() => onToastClose()}
                message="New Safe has been createtd successfully"
              />
            )}
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

export default CreateModal;
