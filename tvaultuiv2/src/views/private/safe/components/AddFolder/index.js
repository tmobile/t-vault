/* eslint-disable react/jsx-curly-newline */
/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Typography } from '@material-ui/core';
import ButtonComponent from 'components/FormFields/ActionButton';
import TextFieldComponent from 'components/FormFields/TextField';
import ComponentError from 'errorBoundaries/ComponentError/component-error';

const AddFolderNameWrapper = styled.div`
  padding: 3.2rem;
  border-radius: 0.3rem;
  background-color: ${(props) =>
    props.theme.palette.background.paper || '#20232e'};
  width: ${(props) => props.width || '100%'};
`;

const FormWrapper = styled.form`
  margin-top: 2rem;
`;

const ButtonWrapper = styled('div')`
  display: flex;
  justify-content: flex-end;
  margin-top: 2rem;
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
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
    <ComponentError>
      {' '}
      <AddFolderNameWrapper width={width}>
        <Typography variant="h5">Add Folder Name*</Typography>
        <FormWrapper>
          <TextFieldComponent
            placeholder="Add folder"
            onChange={(e) => handleChange(e)}
            value={inputValue || ''}
            fullWidth
            helperText="Please enter a minimum of 3 characters lowercase alphabets, number and underscore only."
          />
          <ButtonWrapper>
            <CancelButton>
              <ButtonComponent
                label="Cancel"
                color="primary"
                onClick={handleCancelClick}
              />
            </CancelButton>
            <ButtonComponent
              label="Save"
              color="secondary"
              buttonType="containedSecondary"
              onClick={() =>
                handleSaveClick({ labelText: inputValue, type: 'folder' })
              }
            />
          </ButtonWrapper>
        </FormWrapper>
      </AddFolderNameWrapper>
    </ComponentError>
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
