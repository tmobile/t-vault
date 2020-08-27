import React, { useState } from 'react';
import TextField from '@material-ui/core/TextField';
import styled from 'styled-components';
import ButtonComponent from './buttonComponent';

const SecretWrapper = styled.section`
  padding: 2rem;
  width: 50%;
  border: 1.5px solid #000;
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
`;
const Title = styled.p`
  margin-bottom: 0.8rem;
`;
const KeyIdInputRequirements = styled.p`
  color: #9e9e9e;
  font-size: 1rem;
  margin-bottom: 2rem;
`;
const CancelSaveWrapper = styled.div`
  align-self: flex-end;
  margin-top: 2rem;
`;

const Secrets = () => {
  const [, setSecret] = useState('');
  const [, setKeyId] = useState('');

  return (
    <SecretWrapper>
      <Title>Key Id</Title>
      <TextField
        id="filled-basic"
        variant="filled"
        onChange={(e) => setKeyId(e.target.value)}
      />
      <KeyIdInputRequirements>
        Please enter a minimum of 3 characters lowercase alphabets, number and
        underscores only
      </KeyIdInputRequirements>
      <Title>Secret</Title>
      <TextField
        id="filled-basic"
        variant="filled"
        onChange={(e) => setSecret(e.target.value)}
      />
      <CancelSaveWrapper>
        <ButtonComponent label="CANCEL" color="default" type="contained" />
        <ButtonComponent label="SAVE" type="contained" color="primary" />
      </CancelSaveWrapper>
    </SecretWrapper>
  );
};

export default Secrets;
