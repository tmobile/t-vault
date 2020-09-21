import React, { useState, useEffect, useCallback } from 'react';
import { debounce } from 'lodash';
import Radio from '@material-ui/core/Radio';
import { makeStyles } from '@material-ui/core/styles';
import RadioGroup from '@material-ui/core/RadioGroup';
import { InputLabel, Typography } from '@material-ui/core';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import FormControl from '@material-ui/core/FormControl';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import AutoCompleteComponent from '../../../../../components/FormFields/AutoComplete';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import apiService from '../../apiService';
import Loader from '../Loader';

const { small } = mediaBreakpoints;

const PermissionWrapper = styled.div`
  padding: 1rem 4rem 4rem 4rem;
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
const RequiredText = styled.span`
  font-size: 1.6rem;
  color: #5e627c;
  margin-left: 0.5rem;
  ${small} {
    font-size: 1.4rem;
  }
`;

const RequiredCircle = styled.span`
  width: 0.6rem;
  height: 0.6rem;
  background-color: #e20074;
  border-radius: 50%;
  margin-left: ${(props) => props.margin || '0'};
`;

const InputWrapper = styled.div`
  margin-top: 4rem;
  margin-bottom: 2.4rem;
  position: relative;
  .MuiInputLabel-root {
    display: flex;
    align-items: center;
  }
`;
const InstructionText = styled.p`
  margin-top: 1.4rem;
  color: #bbbbbb;
  font-size: 1.2rem;
  margin-bottom: 0rem;
  ${small} {
    font-size: 1.3rem;
    opacity: 0.4;
  }
`;
const RadioButtonWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  ${small} {
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
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${small} {
    width: 100%;
  }
`;

const customStyle = css`
  position: absolute;
  right: 12px;
  top: 33px;
  color: red;
`;

const useStyles = makeStyles(() => ({
  icon: {
    color: '#5e627c',
    fontSize: '2rem',
  },
}));

const AddUser = (props) => {
  const { handleCancelClick, handleSaveClick, username, access } = props;
  const classes = useStyles();
  const [radioValue, setRadioValue] = useState('read');
  const [searchValue, setSearchValue] = useState('');
  const [options, setOptions] = useState([]);
  const [disabledSave, setDisabledSave] = useState(true);
  const [searchLoader, setSearchLoader] = useState(false);
  const isMobileScreen = useMediaQuery(small);

  useEffect(() => {
    setSearchValue(username);
    setRadioValue(access);
  }, [username, access]);

  useEffect(() => {
    if (searchValue !== '') {
      setDisabledSave(false);
    } else {
      setDisabledSave(true);
    }
  }, [searchValue]);

  const handleChange = (event) => {
    setRadioValue(event.target.value);
  };

  const callSearchApi = useCallback(
    debounce(
      (value) => {
        setSearchLoader(true);
        apiService
          .getApiCall(`/vault/v2/ldap/corpusers?CorpId=${value}`)
          .then((res) => {
            setOptions([]);
            if (res?.data?.data?.values?.length > 0) {
              const array = [];
              setSearchLoader(false);
              res.data.data.values.map((item) => {
                if (item.userName) {
                  return array.push(item.userName);
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

  const onSearchChange = (text) => {
    setSearchValue(text);
    if (text !== '' && text?.length > 2) {
      callSearchApi(text);
    }
  };

  const onSelected = (e, val) => {
    setSearchValue(val);
  };

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
            User Name
            <RequiredCircle margin="0.5rem" />
          </InputLabel>
          <AutoCompleteComponent
            options={options}
            icon="search"
            classes={classes}
            searchValue={searchValue}
            onSelected={(e, val) => onSelected(e, val)}
            onChange={(e) => onSearchChange(e)}
          />
          <InstructionText>
            Search the T-Mobile system to add users
          </InstructionText>
          {searchLoader && <Loader customStyle={customStyle} />}
        </InputWrapper>
        <RadioButtonWrapper>
          <FormControl component="fieldset">
            <RadioGroup
              row
              aria-label="permissions"
              name="permissions1"
              value={radioValue}
              onChange={handleChange}
            >
              <FormControlLabel
                value="read"
                control={<Radio color="default" />}
                label="Read"
              />
              <FormControlLabel
                value="write"
                control={<Radio color="default" />}
                label="Write"
              />
            </RadioGroup>
          </FormControl>
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
              label="Save"
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

AddUser.propTypes = {
  handleSaveClick: PropTypes.func.isRequired,
  handleCancelClick: PropTypes.func.isRequired,
  username: PropTypes.string,
  access: PropTypes.string,
};

AddUser.defaultProps = {
  username: '',
  access: 'read',
};

export default AddUser;
