/* eslint-disable no-nested-ternary */
import React, { useState, useEffect, useCallback } from 'react';
import { debounce } from 'lodash';
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
import configData from '../../config/config';
import TextFieldComponent from '../FormFields/TextField';
import {
  InstructionText,
  RequiredCircle,
  RequiredText,
} from '../../styles/GlobalStyles';
import TypeAheadComponent from '../TypeAheadComponent';

const { small, smallAndMedium } = mediaBreakpoints;

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

const InputWrapper = styled.div`
  margin-top: 3rem;
  margin-bottom: 2.4rem;
  position: relative;
  .MuiInputLabel-root {
    display: flex;
    align-items: center;
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
  right: 1.2rem;
  top: 2.6rem;
  color: red;
`;

const AddGroup = (props) => {
  const {
    handleCancelClick,
    handleSaveClick,
    groups,
    groupname,
    access,
    isSvcAccount,
    isCertificate,
    isIamAzureSvcAccount,
  } = props;
  const [radioValue, setRadioValue] = useState('read');
  const [searchValue, setSearchValue] = useState('');
  const [options, setOptions] = useState([]);
  const [disabledSave, setDisabledSave] = useState(true);
  const [searchLoader, setSearchLoader] = useState(false);
  const [isValidGroupName, setIsValidGroupName] = useState(false);
  const [radioArray, setRadioArray] = useState([]);
  const isMobileScreen = useMediaQuery(small);
  const [existingGroup, setExistingGroup] = useState(false);

  useEffect(() => {
    setSearchValue(groupname);
    setRadioValue(access);
  }, [groupname, access]);

  useEffect(() => {
    if (searchValue?.length > 2) {
      if (!searchLoader) {
        if (options.length === 0 || !options.includes(searchValue)) {
          setIsValidGroupName(false);
        } else {
          setIsValidGroupName(true);
        }
      }
    }
  }, [searchValue, searchLoader, options]);

  useEffect(() => {
    if (searchValue && configData.AD_GROUP_AUTOCOMPLETE) {
      if (
        groups &&
        !Object.keys(groups)?.includes(searchValue?.toLowerCase()) &&
        !Object.keys(groups)?.includes(searchValue)
      ) {
        setExistingGroup(false);
        if (groupname) {
          if (access === radioValue) {
            setDisabledSave(true);
          } else {
            setDisabledSave(false);
          }
        } else if (!isValidGroupName || searchValue === '') {
          setDisabledSave(true);
        } else {
          setDisabledSave(false);
        }
      } else {
        setExistingGroup(true);
        setDisabledSave(true);
      }
    } else {
      setDisabledSave(false);
    }
  }, [
    searchValue,
    existingGroup,
    groups,
    radioValue,
    access,
    groupname,
    isValidGroupName,
  ]);

  const callSearchApi = useCallback(
    debounce(
      (value) => {
        setSearchLoader(true);
        apiService
          .getGroupsName(value)
          .then((res) => {
            setOptions([]);
            setSearchLoader(false);
            if (res?.data?.data?.values?.length > 0) {
              const array = [];
              res.data.data.values.map((item) => {
                if (item.displayName) {
                  return array.push(item.displayName);
                }
                return null;
              });
              setOptions([...array]);
            }
          })
          .catch(() => {
            setSearchLoader(false);
          });
      },
      1000,
      true
    ),
    []
  );

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

  const onSearchChange = (e) => {
    if (e && e?.target?.value) {
      setSearchValue(e.target.value);
      if (e.target?.value !== '' && e.target?.value?.length > 2) {
        callSearchApi(e.target.value);
      }
    } else {
      setSearchValue('');
    }
  };

  const onSelected = (e, val) => {
    if (val) {
      setSearchValue(val);
    }
  };

  return (
    <ComponentError>
      <PermissionWrapper>
        <HeaderWrapper>
          <Typography variant="h5">Add Group</Typography>
          <div>
            <RequiredCircle />
            <RequiredText>Required</RequiredText>
          </div>
        </HeaderWrapper>
        <InputWrapper>
          <InputLabel>
            Group Name
            <RequiredCircle margin="0.5rem" />
          </InputLabel>
          {configData.AD_GROUP_AUTOCOMPLETE ? (
            <>
              <TypeAheadComponent
                options={options}
                icon="search"
                loader={searchLoader}
                disabled={!!(groupname && access)}
                userInput={searchValue}
                onSelected={(e, val) => onSelected(e, val)}
                onChange={(e) => onSearchChange(e)}
                placeholder="Groupname - Enter min 3 characters"
                name="grpVal"
                error={
                  (groupname !== searchValue && !isValidGroupName) ||
                  existingGroup
                }
                helperText={
                  groupname !== searchValue && !isValidGroupName
                    ? `Group name ${searchValue} does not exist!`
                    : existingGroup
                    ? 'Permission already exists!'
                    : ''
                }
                styling={{
                  padding: '0.6rem',
                  maxHeight: '16rem',
                }}
              />
              <InstructionText>
                Search the T-Mobile system to add groups
              </InstructionText>
              {searchLoader && <LoaderSpinner customStyle={customStyle} />}
            </>
          ) : (
            <TextFieldComponent
              value={searchValue}
              placeholder="Groupname"
              fullWidth
              name="searchValue"
              onChange={(e) => setSearchValue(e.target.value)}
            />
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
                onClick={handleCancelClick}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelButton>
            <ButtonComponent
              label={groupname && access ? 'Edit' : 'Save'}
              color="secondary"
              onClick={() => handleSaveClick(searchValue, radioValue)}
              disabled={disabledSave}
              width={isMobileScreen ? '100%' : ''}
            />
          </CancelSaveWrapper>
        </RadioButtonWrapper>
      </PermissionWrapper>
    </ComponentError>
  );
};

AddGroup.propTypes = {
  handleSaveClick: PropTypes.func,
  handleCancelClick: PropTypes.func,
  groupname: PropTypes.string,
  access: PropTypes.string,
  isSvcAccount: PropTypes.bool,
  isCertificate: PropTypes.bool,
  groups: PropTypes.objectOf(PropTypes.any),
  isIamAzureSvcAccount: PropTypes.bool,
};

AddGroup.defaultProps = {
  handleSaveClick: () => {},
  handleCancelClick: () => {},
  groupname: '',
  access: 'read',
  groups: {},
  isSvcAccount: false,
  isCertificate: false,
  isIamAzureSvcAccount: false,
};

export default AddGroup;
