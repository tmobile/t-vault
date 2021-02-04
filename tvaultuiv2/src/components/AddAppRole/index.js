/* eslint-disable no-nested-ternary */

import React, { useState, useEffect } from 'react';
import { InputLabel, Typography } from '@material-ui/core';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../breakpoints';
import ButtonComponent from '../FormFields/ActionButton';
import apiService from '../../views/private/safe/apiService';
import LoaderSpinner from '../Loaders/LoaderSpinner';
import RadioButtonComponent from '../FormFields/RadioButton';
import {
  RequiredCircle,
  RequiredText,
  RequiredWrap,
} from '../../styles/GlobalStyles';
import TextFieldSelect from '../FormFields/SelectFields';

const { small, smallAndMedium } = mediaBreakpoints;

const PermissionWrapper = styled.div`
  padding: 3rem 4rem 4rem 4rem;
  background-color: #1f232e;
  display: flex;
  flex-direction: column;
  margin-top: 2rem;
  position: relative;
  overflow: hidden;
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

const HelperText = styled.div`
  color: #ee4e4e;
  font-size: 1.2rem;
`;

const InputWrapper = styled.div`
  margin-top: 3rem;
  margin-bottom: 2.4rem;
  position: relative;
  .MuiInputLabel-root {
    display: flex;
    align-items: center;
  }
  .MuiSelect-icon {
    top: auto;
    color: #000;
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

const customStyle = css`
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 2;
  transform: translate(-50%, -50%);
`;

const AddAppRole = (props) => {
  const {
    roles,
    handleSaveClick,
    handleCancelClick,
    editClicked,
    role,
    access,
    isSvcAccount,
    isCertificate,
    isIamAzureSvcAccount,
  } = props;
  const [radioValue, setRadioValue] = useState('read');
  const [selectedValue, setSelectedValue] = useState('');
  const [disabledSave, setDisabledSave] = useState(true);
  const [menu, setMenu] = useState([]);
  const isMobileScreen = useMediaQuery(small);
  const [radioArray, setRadioArray] = useState([]);
  const [loader, setLoader] = useState(false);
  const [existingRole, setExistingRole] = useState(false);

  useEffect(() => {
    if (role && access) {
      setSelectedValue(role);
      setMenu([role]);
      setRadioValue(access);
    }
  }, [role, access]);

  useEffect(() => {
    if (!editClicked) {
      setLoader(true);
      apiService
        .getExistingAppRole()
        .then((res) => {
          if (res && res.data?.keys) {
            setLoader(false);
            if (res.data.keys.length > 0) {
              setMenu([...res.data.keys]);
            }
          }
        })
        .catch(() => {
          setLoader(true);
        });
    }
  }, [editClicked]);

  useEffect(() => {
    if (selectedValue) {
      if (roles && !Object.keys(roles).includes(selectedValue.toLowerCase())) {
        if (editClicked) {
          if (access === radioValue) {
            setDisabledSave(true);
          } else {
            setDisabledSave(false);
          }
        } else if (selectedValue === '' || menu.length === 0) {
          setDisabledSave(true);
        } else {
          setDisabledSave(false);
        }
        setExistingRole(false);
      } else {
        setDisabledSave(true);
        setExistingRole(true);
      }
    }
  }, [selectedValue, roles, radioValue, menu, access, editClicked]);

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
        {loader && <LoaderSpinner customStyle={customStyle} />}
        <HeaderWrapper>
          <Typography variant="h5">Add App Role</Typography>
          <RequiredWrap>
            <RequiredCircle />
            <RequiredText>Required</RequiredText>
          </RequiredWrap>
        </HeaderWrapper>
        <InputWrapper>
          <InputLabel>
            App Role
            <RequiredCircle margin="0.5rem" />
          </InputLabel>
          <TextFieldSelect
            menu={menu}
            value={selectedValue}
            disabled={editClicked}
            readOnly={(menu.length === 0 && !loader) || editClicked}
            onChange={(e) => setSelectedValue(e)}
            filledText={
              menu.length === 0 && !loader
                ? 'No app role available'
                : 'Select role name'
            }
          />
          {existingRole ? (
            <HelperText>Permission Already exists!</HelperText>
          ) : (
            <HelperText />
          )}
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
                width={isMobileScreen ? '100%' : ''}
                onClick={() => handleCancelClick()}
              />
            </CancelButton>
            <ButtonComponent
              label={editClicked ? 'Edit' : 'Save'}
              color="secondary"
              onClick={() => handleSaveClick(selectedValue, radioValue)}
              disabled={disabledSave}
              width={isMobileScreen ? '100%' : ''}
            />
          </CancelSaveWrapper>
        </RadioButtonWrapper>
      </PermissionWrapper>
    </ComponentError>
  );
};

AddAppRole.propTypes = {
  handleCancelClick: PropTypes.func.isRequired,
  handleSaveClick: PropTypes.func.isRequired,
  editClicked: PropTypes.bool,
  role: PropTypes.string,
  access: PropTypes.string,
  isSvcAccount: PropTypes.bool,
  isCertificate: PropTypes.bool,
  isIamAzureSvcAccount: PropTypes.bool,
  roles: PropTypes.objectOf(PropTypes.any),
};

AddAppRole.defaultProps = {
  access: 'read',
  role: '',
  editClicked: false,
  isSvcAccount: false,
  isCertificate: false,
  isIamAzureSvcAccount: false,
  roles: {},
};

export default AddAppRole;
