import React from 'react';
import styled from 'styled-components';
import TextField from '@material-ui/core/TextField';

const AddFolderNameWrapper = styled.section`
  padding: 2rem;
  border: 1.5px solid #ddd;
  border-radius: 3px;
  background-color: #fff;
  width: 50%;
  .MuiFormHelperText-contained {
    margin-left: 0;
    margin-top: 10px;
    color: #000;
    font-size: 14px;
  }
`;

const FolderHeader = styled.h1`
  margin-bottom: 0.875rem;
  margin-top: 0;
`;
const AddFolder = () => {
  return (
    <AddFolderNameWrapper>
      <FolderHeader>Add Folder Name*</FolderHeader>
      <form noValidate autoComplete="off">
        <TextField
          id="outlined-basic"
          variant="outlined"
          helperText="Please enter a minimum of 3 characters lowercase alphabets, number and underscore only."
        />
      </form>
    </AddFolderNameWrapper>
  );
};

export default AddFolder;
