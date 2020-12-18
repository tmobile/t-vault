/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useCallback, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { debounce } from 'lodash';
import { Typography, InputLabel } from '@material-ui/core';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import leftArrowIcon from '../../../../../assets/left-arrow.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import AutoCompleteComponent from '../../../../../components/FormFields/AutoComplete';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import apiService from '../../apiService';
import { validateEmail } from '../../../../../services/helper-function';
import {
  InstructionText,
  RequiredCircle,
} from '../../../../../styles/GlobalStyles';

const { small } = mediaBreakpoints;

const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 3rem;
  ${small} {
    margin-top: 1rem;
  }
`;

const LeftIcon = styled.img`
  display: none;
  ${small} {
    display: block;
    margin-right: 1.4rem;
    margin-top: 0.3rem;
  }
`;

const InputFieldLabelWrapper = styled.div`
  margin: 2rem 0 3rem 0;
  position: ${(props) => (props.postion ? 'relative' : '')};
  .MuiSelect-icon {
    top: auto;
    color: ${(props) => props.theme.customColor.primary.color};
  }
`;

const autoLoaderStyle = css`
  position: absolute;
  top: 3rem;
  right: 1rem;
  color: red;
`;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
  justify-content: flex-end;
  ${small} {
    margin-top: 5.3rem;
  }
  button {
    ${small} {
      height: 4.5rem;
    }
  }
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${small} {
    margin-right: 1rem;
    width: 100%;
  }
`;

const EachValueWrap = styled.div`
  display: flex;
  font-size: 1.4rem;
  margin: 0 0 2rem 0;
  p {
    margin: 0;
  }
`;
const Label = styled.p`
  color: ${(props) => props.theme.customColor.label.color};
  margin-right: 0.5rem !important;
`;

const Value = styled.p`
  text-transform: ${(props) => props.capitalize || ''};
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
  icon: {
    color: '#5e627c',
    fontSize: '2rem',
  },
}));

const TransferSafeOwner = (props) => {
  const {
    onTransferCancelClicked,
    transferData,
    onTransferOwnerConfirmationClicked,
  } = props;

  const [owner, setOwner] = useState('');
  const [options, setOptions] = useState([]);
  const [autoLoader, setAutoLoader] = useState(false);
  const classes = useStyles();
  const [isValidEmail, setIsValidEmail] = useState(false);
  const [emailError, setEmailError] = useState(false);
  const [disabledTransfer, setDisabledTransfer] = useState(true);

  useEffect(() => {
    if (owner?.length > 2) {
      if (!autoLoader) {
        if (options.length === 0 || !options.includes(owner)) {
          setIsValidEmail(false);
        } else {
          setIsValidEmail(true);
        }
      }
    }
  }, [owner, autoLoader, options]);

  useEffect(() => {
    if (emailError || !isValidEmail) {
      setDisabledTransfer(true);
    } else {
      setDisabledTransfer(false);
    }
  }, [emailError, owner, isValidEmail]);

  const callSearchApi = useCallback(
    debounce(
      (value) => {
        setAutoLoader(true);
        apiService
          .getOwnerTransferEmail(value)
          .then((res) => {
            setOptions([]);
            const array = [];
            setAutoLoader(false);
            if (res?.data?.data?.values?.length > 0) {
              res.data.data.values.map((item) => {
                if (item.userEmail) {
                  return array.push(item.userEmail);
                }
                return null;
              });
              setOptions([...array]);
            }
          })
          .catch(() => setAutoLoader(false));
      },
      1000,
      true
    ),
    []
  );

  const onOwnerChange = (e) => {
    if (e && e?.target?.value) {
      setOwner(e.target.value);
      if (e.target.value && e.target.value?.length > 2) {
        callSearchApi(e.target.value);
        if (validateEmail(owner)) {
          setEmailError(false);
        } else {
          setEmailError(true);
        }
      }
    } else {
      setOwner('');
    }
  };

  const onSelected = (e, val) => {
    setOwner(val);
    setEmailError(false);
  };

  const onTransfer = () => {
    const payload = {
      newOwnerEmail: owner,
      safeName: transferData.name,
      safeType: transferData.type,
    };
    onTransferOwnerConfirmationClicked(payload);
  };

  return (
    <ComponentError>
      <HeaderWrapper>
        <LeftIcon
          src={leftArrowIcon}
          alt="go-back"
          onClick={() => onTransferCancelClicked()}
        />
        <Typography variant="h5">Transfer Safe Ownership</Typography>
      </HeaderWrapper>
      <EachValueWrap>
        <Label>Safe Name:</Label>
        <Value>{transferData?.name}</Value>
      </EachValueWrap>
      <EachValueWrap>
        <Label>Safe Type:</Label>
        <Value capitalize="capitalize">{transferData?.type} Safe</Value>
      </EachValueWrap>
      <EachValueWrap>
        <Label>Current Owner:</Label>
        <Value>{transferData?.owner}</Value>
      </EachValueWrap>
      <InputFieldLabelWrapper postion>
        <InputLabel>
          New Owner Email ID
          <RequiredCircle margin="0.5rem" />
        </InputLabel>
        <AutoCompleteComponent
          options={options}
          classes={classes}
          searchValue={owner}
          icon="search"
          name="owner"
          onSelected={(e, val) => onSelected(e, val)}
          onChange={(e) => onOwnerChange(e)}
          placeholder="Email address- Enter min 3 characters"
          error={owner?.length > 2 && (emailError || !isValidEmail)}
          helperText={
            owner?.length > 2 && (emailError || !isValidEmail)
              ? 'Please enter a valid email address or not available!'
              : ''
          }
        />
        <InstructionText>Search the T-Mobile system by email.</InstructionText>
        {autoLoader && <LoaderSpinner customStyle={autoLoaderStyle} />}
      </InputFieldLabelWrapper>
      <CancelSaveWrapper>
        <CancelButton>
          <ButtonComponent
            label="Cancel"
            color="primary"
            onClick={() => onTransferCancelClicked()}
          />
        </CancelButton>
        <ButtonComponent
          label="Transfer"
          color="secondary"
          disabled={disabledTransfer}
          onClick={() => onTransfer()}
        />
      </CancelSaveWrapper>
    </ComponentError>
  );
};

TransferSafeOwner.propTypes = {
  transferData: PropTypes.objectOf(PropTypes.any).isRequired,
  onTransferCancelClicked: PropTypes.func.isRequired,
  onTransferOwnerConfirmationClicked: PropTypes.func.isRequired,
};

export default TransferSafeOwner;
