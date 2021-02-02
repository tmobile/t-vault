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

const AddUser = (props) => {
  const {
    users,
    handleCancelClick,
    handleSaveClick,
    username,
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
  const [isValidUserName, setIsValidUserName] = useState(true);
  const [radioArray, setRadioArray] = useState([]);
  const isMobileScreen = useMediaQuery(small);
  const [selectedUser, setSelectedUser] = useState({});
  const [existingUser, setExistingUser] = useState(false);

  useEffect(() => {
    setSearchValue(username);
    setRadioValue(access);
  }, [username, access]);

  const getName = (displayName) => {
    if (displayName?.match(/(.*)\[(.*)\]/)) {
      const lastFirstName = displayName?.match(/(.*)\[(.*)\]/)[1].split(', ');
      const name = `${lastFirstName[1]} ${lastFirstName[0]}`;
      const optionalDetail = displayName?.match(/(.*)\[(.*)\]/)[2];
      return `${name}, ${optionalDetail}`;
    }
    if (displayName?.match(/(.*), (.*)/)) {
      const lastFirstName = displayName?.split(', ');
      const name = `${lastFirstName[1]} ${lastFirstName[0]}`;
      return name;
    }
    return displayName;
  };

  useEffect(() => {
    if (configData.AD_USERS_AUTOCOMPLETE) {
      if (username) {
        if (access === radioValue) {
          setDisabledSave(true);
        } else {
          setDisabledSave(false);
        }
      } else if (!isValidUserName || searchValue === '') {
        setDisabledSave(true);
      } else {
        setDisabledSave(false);
      }
    } else {
      setDisabledSave(false);
    }
  }, [searchValue, radioValue, access, username, isValidUserName]);

  const handleChange = (event) => {
    setRadioValue(event.target.value);
  };

  const callSearchApi = useCallback(
    debounce(
      (value) => {
        setSearchLoader(true);
        const userNameSearch = apiService.getUserName(value);
        const emailSearch = apiService.getOwnerEmail(value);
        Promise.all([userNameSearch, emailSearch])
          .then((responses) => {
            setOptions([]);
            const array = new Set([]);
            if (responses[0]?.data?.data?.values?.length > 0) {
              responses[0].data.data.values.map((item) => {
                if (item.userName) {
                  return array.add(item);
                }
                return null;
              });
            }
            if (responses[1]?.data?.data?.values?.length > 0) {
              responses[1].data.data.values.map((item) => {
                if (item.userName) {
                  return array.add(item);
                }
                return null;
              });
            }
            setOptions([...array]);
            setSearchLoader(false);
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

  const onSearchChange = (e) => {
    if (e && e?.target?.value !== undefined) {
      setSearchValue(e?.target?.value);
      setExistingUser(false);
      setIsValidUserName(true);
      if (e?.target?.value !== '' && e?.target?.value?.length > 2) {
        callSearchApi(e.target.value);
      }
    }
  };

  const onSelected = (e, val) => {
    if (val) {
      setSearchValue(val?.split(', ')[1]);
      setSelectedUser(
        options.filter(
          (i) => i?.userEmail?.toLowerCase() === val?.split(', ')[0]
        )[0]
      );
      setOptions([]);
    }
  };

  const onSaveClick = () => {
    if (username && access) {
      const result = username?.match(/\((.*)\)/)[1];
      handleSaveClick(result?.toLowerCase(), radioValue);
    }
    if (
      getName(selectedUser?.displayName?.toLowerCase())?.split(', ')[0] !==
      searchValue
    ) {
      setIsValidUserName(false);
    } else {
      if (
        Object.keys(users).includes(selectedUser?.userName?.toLowerCase()) &&
        users[selectedUser?.userName?.toLowerCase()] !== 'sudo'
      ) {
        setExistingUser(true);
        return;
      }
      setIsValidUserName(true);
      const result = selectedUser?.userName;
      handleSaveClick(result?.toLowerCase(), radioValue);
    }
  };

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
          <Typography variant="h5">Add User</Typography>
          <div>
            <RequiredCircle />
            <RequiredText>Required</RequiredText>
          </div>
        </HeaderWrapper>
        <InputWrapper>
          <InputLabel>
            User
            <RequiredCircle margin="0.5rem" />
          </InputLabel>
          {configData.AD_USERS_AUTOCOMPLETE ? (
            <>
              <TypeAheadComponent
                options={options.map(
                  (item) =>
                    `${item?.userEmail?.toLowerCase()}, ${getName(
                      item?.displayName?.toLowerCase()
                    )}, ${item?.userName?.toLowerCase()}`
                )}
                loader={searchLoader}
                icon="search"
                disabled={!!(access && username)}
                placeholder="Search by NTID, Email or Name"
                userInput={searchValue}
                name="userVal"
                error={
                  (username !== searchValue && !isValidUserName) || existingUser
                }
                helperText={
                  username !== searchValue && !isValidUserName
                    ? `User ${searchValue} does not exist!`
                    : existingUser
                    ? 'Permission already exists!'
                    : ''
                }
                onSelected={(e, val) => onSelected(e, val)}
                onChange={(e) => onSearchChange(e)}
                styling={{
                  padding: '0.6rem',
                  maxHeight: '16rem',
                }}
              />
              <InstructionText>
                Search the T-Mobile system to add users
              </InstructionText>
              {searchLoader && <LoaderSpinner customStyle={customStyle} />}
            </>
          ) : (
            <TextFieldComponent
              value={searchValue}
              placeholder="Username"
              fullWidth
              name="searchValue"
              onChange={(e) => setSearchValue(e.target.value)}
            />
          )}
        </InputWrapper>
        <RadioButtonWrapper>
          <RadioButtonComponent
            menu={radioArray}
            handleChange={(e) => handleChange(e)}
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
              label={username && access ? 'Edit' : 'Save'}
              color="secondary"
              onClick={() => onSaveClick(searchValue, radioValue)}
              disabled={disabledSave}
              width={isMobileScreen ? '100%' : ''}
            />
          </CancelSaveWrapper>
        </RadioButtonWrapper>
      </PermissionWrapper>
    </ComponentError>
  );
};

AddUser.propTypes = {
  handleSaveClick: PropTypes.func,
  handleCancelClick: PropTypes.func,
  username: PropTypes.string,
  access: PropTypes.string,
  users: PropTypes.objectOf(PropTypes.any),
  isSvcAccount: PropTypes.bool,
  isCertificate: PropTypes.bool,
  isIamAzureSvcAccount: PropTypes.bool,
};

AddUser.defaultProps = {
  username: '',
  access: 'read',
  handleCancelClick: () => {},
  handleSaveClick: () => {},
  users: {},
  isSvcAccount: false,
  isCertificate: false,
  isIamAzureSvcAccount: false,
};

export default AddUser;
