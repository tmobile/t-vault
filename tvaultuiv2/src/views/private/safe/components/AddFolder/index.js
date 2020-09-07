/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import TextField from '@material-ui/core/TextField';
import ButtonComponent from 'components/common/ButtonComponent';

const AddFolderNameWrapper = styled.div`
  padding: 3.2rem;
  border: 0.1rem solid #ddd;
  border-radius: 0.3rem;
  background-color: #fff;
  width: ${(props) => props.width || '100%'};
  .MuiFormHelperText-contained {
    margin-left: 0;
    margin-top: 1rem;
    color: #000;
    font-size: 0.8rem;
  }
`;

const FolderHeader = styled.h1`
  margin-bottom: 1.4rem;
  margin-top: 0;
`;
const ButtonWrapper = styled('div')`
  display: flex;
  justify-content: flex-end;
`;
const AddFolder = (props) => {
  const [inputValue, setInputValue] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const { width = '100%', handleCancelClick, handleSaveClick } = props;

  const handleValidation = (value) => {
    if (value.length > 10) setErrorMessage('max of 3 characters');
  };

  const handleChange = (e) => {
    setInputValue(e.target.value);
    handleValidation(e.target.value);
  };

  return (
    <AddFolderNameWrapper width={width}>
      <FolderHeader>Add Folder Name*</FolderHeader>
      <form noValidate autoComplete="off">
        <TextField
          id="outlined-basic"
          variant="outlined"
          label={<span>{errorMessage || ''}</span>}
          error={!!errorMessage}
          onChange={(e) => handleChange(e)}
          value={inputValue || ''}
          helperText="Please enter a minimum of 3 characters lowercase alphabets, number and underscore only."
        />
        <ButtonWrapper>
          <ButtonComponent
            label="CANCEL"
            color="default"
            type="contained"
            onClick={handleCancelClick}
          />
          <ButtonComponent
            label="SAVE"
            color="primary"
            type="contained"
            onClick={() => handleSaveClick({ name: inputValue, key: '' })}
          />
        </ButtonWrapper>
      </form>
    </AddFolderNameWrapper>
  );
};
AddFolder.propTypes = {
  width: PropTypes.string,
  handleCancelClick: PropTypes.func,
  handleSaveClick: PropTypes.func,
};
AddFolder.defaultProps = {
  width: '100%',
  handleSaveClick: () => {},
  handleCancelClick: () => {},
};
export default AddFolder;
