/* eslint-disable react/jsx-curly-newline */
/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import { InputLabel, Typography } from '@material-ui/core';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ButtonComponent from 'components/FormFields/ActionButton';
import TextFieldComponent from 'components/FormFields/TextField';

const SecretWrapper = styled.section`
  padding: 3rem;
  background: #1f232e;
  display: flex;
  flex-direction: column;
`;

const FormWrapper = styled.form`
  margin-top: 4rem;
`;

const KeyIdInputRequirements = styled.p`
  color: #fff;
  opacity: 0.4;
  font-size: 1.3rem;
  margin-bottom: 2rem;
`;
const CancelSaveWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
  margin-top: 2rem;
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
`;

const CreateSecret = (props) => {
  const { handleSecretSave, handleSecretCancel } = props;
  const [secret, setSecret] = useState('');
  const [keyId, setKeyId] = useState('');
  const [keyErrorMessage, setKeyErrorMessage] = useState('');
  const [valueErrorMessage, setValueErrorMessage] = useState('');

  const handleValidation = (value, type) => {
    if (type === 'key') {
      if (value.length > 3) setKeyErrorMessage('max of 3 characters');
    } else {
      setValueErrorMessage('');
    }
  };

  const handleKeyChange = (key) => {
    setKeyId(key);
    handleValidation(key, 'key');
  };

  const handleValueChange = (value) => {
    setSecret(value);
    handleValidation(value, 'value');
  };

  return (
    <SecretWrapper>
      <Typography variant="h5">Add Secrets</Typography>
      <FormWrapper>
        <InputLabel>Key Id</InputLabel>
        <TextFieldComponent
          placeholder="Key Id"
          value={keyId || ''}
          onChange={(e) => setKeyId(e.target.value)}
          fullWidth
        />
        <KeyIdInputRequirements>
          Please enter a minimum of 3 characters lowercase alphabets, number and
          underscores only
        </KeyIdInputRequirements>
        <InputLabel>Secret</InputLabel>
        <TextFieldComponent
          placeholder="Secret"
          value={secret || ''}
          onChange={(e) => setSecret(e.target.value)}
          fullWidth
        />
        <CancelSaveWrapper>
          <CancelButton>
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleSecretCancel(false)}
            />
          </CancelButton>
          <ButtonComponent
            label="Create"
            icon="add"
            color="secondary"
            onClick={() =>
              handleSecretSave({ labelKey: keyId, labelValue: secret })
            }
          />
        </CancelSaveWrapper>
      </FormWrapper>
    </SecretWrapper>
  );
};
CreateSecret.propTypes = {
  handleSecretSave: PropTypes.func,
  handleSecretCancel: PropTypes.func,
};
CreateSecret.defaultProps = {
  handleSecretSave: () => {},
  handleSecretCancel: () => {},
};

export default CreateSecret;
