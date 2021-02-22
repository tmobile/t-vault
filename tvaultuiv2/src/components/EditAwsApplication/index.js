/* eslint-disable no-nested-ternary */

import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { InputLabel, Typography } from '@material-ui/core';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import ButtonComponent from '../FormFields/ActionButton';
import TextFieldComponent from '../FormFields/TextField';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../breakpoints';
import RadioButtonComponent from '../FormFields/RadioButton';

const { small, smallAndMedium } = mediaBreakpoints;

const InputWrapper = styled.div`
  margin-top: 3rem;
  margin-bottom: 2.4rem;
  position: relative;
  .MuiInputLabel-root {
    display: flex;
    align-items: center;
  }
`;

const PermissionWrapper = styled.div`
  padding: 3rem 4rem 4rem 4rem;
  background-color: #1f232e;
  display: flex;
  flex-direction: column;
  margin-top: 2rem;
  ${small} {
    padding: 2.2rem 2.4rem 2.4rem 2.4rem;
  }
`;
const HeaderWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  div {
    display: flex;
    align-items: center;
  }
  .MuiTypography-h5 {
    font-weight: normal;
    ${small} {
      font-size: 1.6rem;
    }
  }
`;

const RadioButtonWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  ${smallAndMedium} {
    flex-direction: column;
  }
  fieldset {
    ${small} {
      margin-bottom: 4.5rem;
    }
  }
`;
const CancelSaveWrapper = styled.div`
  display: flex;
  ${smallAndMedium} {
    align-self: flex-end;
    margin-top: 3rem;
  }
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${small} {
    width: 100%;
  }
`;

const EditAwsApplication = (props) => {
  const {
    handleCancelClick,
    handleSaveClick,
    awsName,
    access,
    isSvcAccount,
    isCertificate,
    isIamAzureSvcAccount,
  } = props;
  const [radioValue, setRadioValue] = useState('read');
  const [value, setValue] = useState('');
  const [disabledSave, setDisabledSave] = useState(true);
  const isMobileScreen = useMediaQuery(small);
  const [radioArray, setRadioArray] = useState([]);

  useEffect(() => {
    setValue(awsName);
    setRadioValue(access);
  }, [awsName, access]);

  useEffect(() => {
    if (radioValue === access) {
      setDisabledSave(true);
    } else {
      setDisabledSave(false);
    }
  }, [radioValue, access]);

  useEffect(() => {
    if (isIamAzureSvcAccount) {
      setRadioArray(['read', 'rotate', 'deny']);
    } else if (isCertificate) {
      setRadioArray(['read', 'deny']);
    } else if (isSvcAccount) {
      setRadioArray(['read', 'reset', 'deny']);
    } else {
      setRadioArray(['read', 'write', 'deny']);
    }
  }, [isIamAzureSvcAccount, isSvcAccount, isCertificate]);

  return (
    <ComponentError>
      <PermissionWrapper>
        <HeaderWrapper>
          <Typography variant="h5">AWS Configuration</Typography>
        </HeaderWrapper>
        <InputWrapper>
          <InputLabel>Aws Application Name</InputLabel>
          <TextFieldComponent
            value={value}
            placeholder="AWS Application Name"
            fullWidth
            readOnly
            name="name"
          />
        </InputWrapper>
        <RadioButtonWrapper>
          <RadioButtonComponent
            menu={radioArray}
            handleChange={(e) => setRadioValue(e.target.value)}
            value={radioValue}
          />
          <CancelSaveWrapper>
            <CancelButton>
              <ButtonComponent
                label="Cancel"
                color="primary"
                onClick={handleCancelClick}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelButton>
            <ButtonComponent
              label={awsName && access ? 'Edit' : 'Save'}
              color="secondary"
              onClick={() => handleSaveClick(value, radioValue)}
              disabled={disabledSave}
              width={isMobileScreen ? '100%' : ''}
            />
          </CancelSaveWrapper>
        </RadioButtonWrapper>
      </PermissionWrapper>
    </ComponentError>
  );
};

EditAwsApplication.propTypes = {
  handleSaveClick: PropTypes.func.isRequired,
  handleCancelClick: PropTypes.func.isRequired,
  access: PropTypes.string.isRequired,
  awsName: PropTypes.string.isRequired,
  isSvcAccount: PropTypes.bool,
  isCertificate: PropTypes.bool,
  isIamAzureSvcAccount: PropTypes.bool,
};

EditAwsApplication.defaultProps = {
  isSvcAccount: false,
  isCertificate: false,
  isIamAzureSvcAccount: false,
};

export default EditAwsApplication;
