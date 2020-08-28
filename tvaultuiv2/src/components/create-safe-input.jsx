import React, { useState } from 'react';
import styled from 'styled-components';
import { InputLabel } from '@material-ui/core';
import TextField from '@material-ui/core/TextField';
import Select from '@material-ui/core/Select';
import MenuItem from '@material-ui/core/MenuItem';

const ModalWrapper = styled.section`
  background-color: #fff;
  padding: 2rem 3rem;
  border-radius: 10px;
  border: none;
  outline: none;
  width: 50%;
`;

const InputFieldLabelWrapper = styled.div`
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
    width: 100%;
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
`;

const PopoverDescriptionWrapper = styled.div`
  display: flex;
  margin-bottom: 1.5rem;
  position: relative;
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

const SafeDescription = styled.p`
  margin-left: 2rem;
  color: #ccc;
`;

const CreateSafeInput = () => {
  const [type, setType] = useState('Personal');
  const [owner, setOwner] = useState('');
  const [safeName, setSafeName] = useState('');
  const [description, setDescription] = useState('');
  const [popOverOpen, setPopOverOpen] = useState(false);

  const onIconClicked = () => {
    setPopOverOpen(!popOverOpen);
  };
  return (
    <ModalWrapper>
      <h2 id="transition-modal-title">Create Safe</h2>
      <PopoverDescriptionWrapper>
        <button type="button" onClick={() => onIconClicked()}>
          Icon
        </button>
        <PopoverWrapper popOverOpen={popOverOpen}>Icon popover</PopoverWrapper>
        <SafeDescription>
          A safe is a logical unit to store the secrets. All the safes are
          created within vault. You can control access only at the safe level.
          As a vault administrator you can manage safe but cannot view the
          content of the safe.
        </SafeDescription>
      </PopoverDescriptionWrapper>
      <form className="input-form">
        <InputFieldLabelWrapper>
          <InputLabel>Safe Name</InputLabel>
          <TextField
            id="filled-basic"
            variant="filled"
            value={safeName}
            onChange={(e) => setSafeName(e.target.value)}
          />
        </InputFieldLabelWrapper>
        <InputFieldLabelWrapper>
          <InputLabel>Owner</InputLabel>
          <TextField
            id="filled-basic"
            variant="filled"
            value={owner}
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
        </InputFieldLabelWrapper>
      </form>
    </ModalWrapper>
  );
};

export default CreateSafeInput;
