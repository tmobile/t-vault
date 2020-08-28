/* eslint-disable react/jsx-props-no-spreading */
import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import Backdrop from '@material-ui/core/Backdrop';
import TextField from '@material-ui/core/TextField';
import Fade from '@material-ui/core/Fade';
import styled from 'styled-components';
import { InputLabel } from '@material-ui/core';

const useStyles = makeStyles(() => ({
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
}));

const ModalWrapper = styled.div`
  background-color: #fff;
  padding: 2rem 3rem;
  border-radius: 10px;
  border: none;
  outline: none;
  width: 50%;
  .each-input {
    margin-bottom: 1rem;
    .MuiFormControl-root {
      width: 100%;
    }
    .MuiFormLabel-root {
      margin-bottom: 12px;
      font-size: 14px;
      font-weight: bold;
    }
    .MuiFilledInput-root {
      border-radius: 5px;
      background-color: #eee;
      :before,
      :after,
      :hover:before {
        border: 0;
      }
    }
    .MuiSelect-icon {
      top: auto;
      color: #000;
    }
    .MuiFilledInput-input,
    .MuiFilledInput-multiline {
      padding: 1rem 0.5rem;
    }
  }
`;

const PopoverDescriptionWrapper = styled.div`
  display: flex;
  margin-bottom: 1.5rem;
  position: relative;
  .safe-description {
    margin-left: 2rem;
    color: #ccc;
  }
`;

const PopoverWrapper = styled.div`
  position: absolute;
  bottom: -48px;
  background-color: #fff;
  padding: 2rem;
  z-index: 2;
  border-radius: 3px;
  box-shadow: 0 0.125em 0.75em 0px rgba(0, 0, 0, 0.15);
  display: ${(props) => (props.popOverOpen ? 'block' : 'none')};
`;

const CreateModal = () => {
  const classes = useStyles();
  const [open, setOpen] = useState(false);
  const [type, setType] = useState('Personal');
  const [owner, setOwner] = useState('');
  const [safeName, setSafeName] = useState('');
  const [description, setDescription] = useState('');
  const [popOverOpen, setPopOverOpen] = useState(false);
  const handleOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };
  const safeTypes = [
    {
      value: 'Personal',
    },
    {
      value: 'Public',
    },
  ];
  const onIconClicked = () => {
    setPopOverOpen(!popOverOpen);
  };

  return (
    <div>
      <button type="button" onClick={handleOpen}>
        Create
      </button>
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
            <h2 id="transition-modal-title">Create Safe</h2>
            <PopoverDescriptionWrapper>
              <button type="button" onClick={() => onIconClicked()}>
                Icon
              </button>
              <PopoverWrapper popOverOpen={popOverOpen}>
                Icon popover
              </PopoverWrapper>
              <p className="safe-description">
                A safe is a logical unit to store the secrets. All the safes are
                created within vault. You can control access only at the safe
                level. As a vault administrator you can manage safe but cannot
                view the content of the safe.
              </p>
            </PopoverDescriptionWrapper>
            <form className="input-form">
              <div className="each-input">
                <InputLabel>Safe Name</InputLabel>
                <TextField
                  id="filled-basic"
                  variant="filled"
                  value={safeName}
                  onChange={(e) => setSafeName(e.target.value)}
                />
              </div>
              <div className="each-input">
                <InputLabel>Owner</InputLabel>
                <TextField
                  id="filled-basic"
                  variant="filled"
                  value={owner}
                  onChange={(e) => setOwner(e.target.value)}
                />
              </div>
              <div className="each-input">
                <InputLabel>Type of Safe</InputLabel>
                <TextField
                  id="standard-select-currency-native"
                  select
                  value={type}
                  variant="filled"
                  onChange={(e) => setType(e.target.value)}
                  SelectProps={{
                    native: true,
                  }}
                >
                  {safeTypes.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.value}
                    </option>
                  ))}
                </TextField>
              </div>
              <div className="each-input">
                <InputLabel>Description</InputLabel>
                <TextField
                  id="standard-multiline-flexible"
                  multiline
                  rowsMax={4}
                  variant="filled"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Add some details about this safe"
                  helperText="Please add a minimum of 10 characters"
                />
              </div>
            </form>
          </ModalWrapper>
        </Fade>
      </Modal>
    </div>
  );
};

export default CreateModal;
