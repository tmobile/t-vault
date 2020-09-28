import React, { useState, useEffect } from 'react';
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
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import SelectComponent from '../../../../../components/FormFields/SelectFields';
import apiService from '../../apiService';
import LoaderSpinner from '../../../../../components/LoaderSpinner';

const { small } = mediaBreakpoints;

const PermissionWrapper = styled.div`
  padding: 1rem 4rem 4rem 4rem;
  background-color: #1f232e;
  display: flex;
  flex-direction: column;
  margin-top: 2rem;
  position: relative;
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
  margin-top: 4rem;
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

const ErrorMessage = styled.p`
  color: ${(props) => props.theme.palette.error.main || 'red'};
  font-size: 1.4rem;
  margin: 0;
`;
const customStyle = css`
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 2;
  transform: translate(-50%, -50%);
`;

const useStyles = makeStyles(() => ({
  select: {
    '&.MuiFilledInput-root.Mui-focused': {
      backgroundColor: '#fff',
    },
  },
  dropdownStyle: {
    backgroundColor: '#fff',
  },
}));

const AddAppRole = (props) => {
  const classes = useStyles();
  const { handleSaveClick, handleCancelClick } = props;
  const [radioValue, setRadioValue] = useState('read');
  const [selectedValue, setSelectedValue] = useState('role');
  const [disabledSave, setDisabledSave] = useState(true);
  const [menu, setMenu] = useState([]);
  const isMobileScreen = useMediaQuery(small);
  const [loader, setLoader] = useState(false);

  useEffect(() => {
    setLoader(true);
    apiService
      .getExistingAppRole()
      .then((res) => {
        if (res && res.data?.keys) {
          setLoader(false);
          if (res.data.keys.length > 0) {
            setMenu([...res.data.keys]);
            setSelectedValue(res.data.keys[0]);
          }
        }
      })
      .catch(() => {
        setLoader(true);
      });
  }, []);

  useEffect(() => {
    if (selectedValue === '' || menu.length === 0) {
      setDisabledSave(true);
    } else {
      setDisabledSave(false);
    }
  }, [selectedValue]);

  const handleChange = (event) => {
    setRadioValue(event.target.value);
  };

  return (
    <ComponentError>
      <PermissionWrapper>
        {loader && <LoaderSpinner customStyle={customStyle} />}
        <HeaderWrapper>
          <Typography variant="h5">Add your app</Typography>
        </HeaderWrapper>
        <InputWrapper>
          <InputLabel required>App Role</InputLabel>
          <SelectComponent
            menu={menu}
            value={selectedValue}
            classes={classes}
            readOnly={menu.length === 0}
            onChange={(e) => setSelectedValue(e.target.value)}
          />
        </InputWrapper>
        {menu.length === 0 && (
          <ErrorMessage>No app role is available</ErrorMessage>
        )}
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
                width={isMobileScreen ? '100%' : ''}
                onClick={() => handleCancelClick()}
              />
            </CancelButton>
            <ButtonComponent
              label="Save"
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
};

export default AddAppRole;
