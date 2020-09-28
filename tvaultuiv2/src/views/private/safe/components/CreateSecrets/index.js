/* eslint-disable react/jsx-curly-newline */
import React, { useState } from 'react';
import { InputLabel, Typography } from '@material-ui/core';
import styled from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';

const SecretWrapper = styled.section`
  padding: 3rem;
  display: flex;
  flex-direction: column;
  background-color: ${(props) =>
    props.theme.palette.background.paper || '#20232e'};
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
  const {
    handleSecretSave,
    handleSecretCancel,
    parentId,
    secretprefilledData,
  } = props;
  const [secret, setSecret] = useState('');
  const [keyId, setKeyId] = useState('');
  const [keyErrorMessage, setKeyErrorMessage] = useState(null);
  const [valueErrorMessage, setValueErrorMessage] = useState(null);

  // screen resolution handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  // prefetch input data if any(i.e while editing)
  const { secretData } = secretprefilledData;

  const handleValidation = (value, type) => {
    if (type === 'key') {
      setKeyErrorMessage(value.length < 3 || !value.match(/^[a-zA-Z0-9_]*$/g));
    } else {
      setValueErrorMessage(value.length < 3);
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
            value={secretData ? Object.keys(secretData)[0] : keyId || ''}
            onChange={(e) => handleKeyChange(e.target.value)}
            fullWidth
            error={keyErrorMessage}
            helperText={
              keyErrorMessage
                ? 'Please enter a minimum of 3 characters lowercase alphabets, number and underscore only.'
                : ''
            }
          />
          <KeyIdInputRequirements>
            Please enter a minimum of 3 characters lowercase alphabets, number
            and underscores only
          </KeyIdInputRequirements>
          <InputLabel>Secret</InputLabel>
          <TextFieldComponent
            placeholder="Secret"
            value={secretData ? Object.values(secretData)[0] : secret || ''}
            onChange={(e) => handleValueChange(e.target.value)}
            fullWidth
            error={valueErrorMessage}
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
                disabled={
                  !secret || !keyId || valueErrorMessage || keyErrorMessage
                }
                onClick={() =>
                  handleSecretSave({
                    key: keyId,
                    value: secret,
                    type: 'secret',
                    parentId,
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
  parentId: PropTypes.string,
  secretprefilledData: PropTypes.objectOf(PropTypes.object),
};
CreateSecret.defaultProps = {
  handleSecretSave: () => {},
  handleSecretCancel: () => {},
  parentId: '',
  secretprefilledData: {},
};

export default CreateSecret;
