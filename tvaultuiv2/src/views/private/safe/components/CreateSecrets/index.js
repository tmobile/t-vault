/* eslint-disable react/jsx-curly-newline */
/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import { InputLabel, Typography } from '@material-ui/core';
import styled from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ButtonComponent from 'components/FormFields/ActionButton';
import TextFieldComponent from 'components/FormFields/TextField';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from 'breakpoints';

const SecretWrapper = styled.section`
  padding: 3rem;
  display: flex;
  flex-direction: column;
  ${mediaBreakpoints.small} {
    padding: 2rem;
  }
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
  ${mediaBreakpoints.small} {
    justify-content: space-between;
  }
`;

const BtnWrapper = styled.div`
  width: 19rem;
  display: flex;
  justify-content: space-between;
  ${mediaBreakpoints.small} {
    width: 100%;
  }
`;

const CreateSecret = (props) => {
  const { handleSecretSave, handleSecretCancel } = props;
  const [secret, setSecret] = useState('');
  const [keyId, setKeyId] = useState('');
  const [keyErrorMessage, setKeyErrorMessage] = useState('');
  const [valueErrorMessage, setValueErrorMessage] = useState('');

  // screen resolution handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

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
    <ComponentError>
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
            Please enter a minimum of 3 characters lowercase alphabets, number
            and underscores only
          </KeyIdInputRequirements>
          <InputLabel>Secret</InputLabel>
          <TextFieldComponent
            placeholder="Secret"
            value={secret || ''}
            onChange={(e) => setSecret(e.target.value)}
            fullWidth
          />
          <CancelSaveWrapper>
            <BtnWrapper>
              {' '}
              <ButtonComponent
                label="Cancel"
                color="primary"
                onClick={() => handleSecretCancel(false)}
                width={isMobileScreen ? '48%' : ''}
              />
              <ButtonComponent
                label="Create"
                icon="add"
                color="secondary"
                width={isMobileScreen ? '48%' : ''}
                onClick={() =>
                  handleSecretSave({
                    labelKey: keyId,
                    labelValue: secret,
                    type: 'file',
                  })
                }
              />
            </BtnWrapper>
          </CancelSaveWrapper>
        </FormWrapper>
      </SecretWrapper>
    </ComponentError>
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
