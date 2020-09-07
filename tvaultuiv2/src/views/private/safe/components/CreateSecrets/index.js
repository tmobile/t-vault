/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import { InputLabel } from '@material-ui/core';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ButtonComponent from 'components/FormFields/ActionButton';
import TextFieldComponent from 'components/FormFields/TextField';

const SecretWrapper = styled.section`
  padding: 2rem;
  background: #1f232e;
  border: 0.15rem solid #000;
  display: flex;
  flex-direction: column;
  .MuiFilledInput-root {
    border-radius: 0;
    :before,
    :after,
    :hover:before {
      border: 0;
    }
  }
  .MuiFilledInput-input {
    padding: 1rem 0.5rem;
  }
  .MuiFormLabel-root {
    margin-bottom: 1.2rem;
  }
`;

const KeyIdInputRequirements = styled.p`
  color: #9e9e9e;
  font-size: 1rem;
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

  return (
    <SecretWrapper>
      <h1>Add Secrets</h1>
      <form>
        <InputLabel>Key Id</InputLabel>
        <TextFieldComponent
          placeholder="Key Id"
          value={keyId || ''}
          onChange={(e) => setKeyId(e.target.value)}
          variant="light"
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
          variant="light"
        />
        <CancelSaveWrapper>
          <CancelButton>
            <ButtonComponent
              label="Cancel"
              buttonType="containedPrimary"
              onClick={() => handleSecretCancel(false)}
            />
          </CancelButton>
          <ButtonComponent
            label="Create"
            icon="add"
            buttonType="containedSecondary"
            onClick={() => handleSecretSave(secret)}
          />
        </CancelSaveWrapper>
      </form>
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
