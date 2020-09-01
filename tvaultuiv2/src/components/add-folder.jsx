import React from 'react';
import styled from 'styled-components';
import TextField from '@material-ui/core/TextField';

const AddFolderNameWrapper = styled.section`
  padding: 3.2rem;
  border: 0.1rem solid #ddd;
  border-radius: 0.3rem;
  background-color: #fff;
  width: 50%;
  .MuiFormHelperText-contained {
    margin-left: 0;
    margin-top: 1rem;
    color: #000;
    font-size: 1.4rem;
  }
`;

const FolderHeader = styled.h1`
  margin-bottom: 1.4rem;
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
