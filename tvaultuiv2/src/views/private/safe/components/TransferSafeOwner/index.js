/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useCallback, useEffect } from 'react';
import { debounce } from 'lodash';
import { Typography, InputLabel } from '@material-ui/core';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import leftArrowIcon from '../../../../../assets/left-arrow.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import apiService from '../../apiService';
import { validateEmail } from '../../../../../services/helper-function';
import {
  InstructionText,
  RequiredCircle,
} from '../../../../../styles/GlobalStyles';
import TypeAheadComponent from '../../../../../components/TypeAheadComponent';

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
  position: ${(props) => (props?.postion ? 'relative' : '')};
  .MuiSelect-icon {
    top: auto;
    color: ${(props) => props?.theme?.customColor?.primary?.color};
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
  color: ${(props) => props?.theme?.customColor?.label?.color};
  margin-right: 0.5rem !important;
`;

const Value = styled.p`
  text-transform: ${(props) => props.capitalize || ''};
`;

const TransferSafeOwner = (props) => {
  const {
    onTransferCancelClicked,
    transferData,
    onTransferOwnerConfirmationClicked,
  } = props;

  const [owner, setOwner] = useState('');
  const [ownerSelected, setOwnerSelected] = useState({});
  const [options, setOptions] = useState([]);
  const [autoLoader, setAutoLoader] = useState(false);
  const [isValidEmail, setIsValidEmail] = useState(false);
  const [emailError, setEmailError] = useState(false);
  const [disabledTransfer, setDisabledTransfer] = useState(true);

  useEffect(() => {
    if (owner?.length > 2 && ownerSelected?.userEmail) {
      if (!autoLoader) {
        if (ownerSelected?.userEmail.toLowerCase() !== owner) {
          setIsValidEmail(false);
        } else {
          setIsValidEmail(true);
        }
      }
    }
  }, [owner, ownerSelected, autoLoader]);

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
        const userNameSearch = apiService.getUserName(value);
        const emailSearch = apiService.getOwnerTransferEmail(value);
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
            setAutoLoader(false);
          })
          .catch(() => {
            setAutoLoader(false);
          });
      },
      1000,
      true
    ),
    []
  );

  const onOwnerChange = (e) => {
    if (e && e?.target?.value !== undefined) {
      setOwner(e.target.value);
      if (e.target.value && e.target.value?.length > 2) {
        callSearchApi(e.target.value);
        if (validateEmail(owner)) {
          setEmailError(false);
        } else {
          setEmailError(true);
        }
      }
    }
  };

  const onSelected = (e, val) => {
    const ownerEmail = val?.split(', ')[0];
    setOwnerSelected(
      options.filter((i) => i?.userEmail?.toLowerCase() === ownerEmail)[0]
    );
    setOwner(ownerEmail);
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
          New Owner
          <RequiredCircle margin="0.5rem" />
        </InputLabel>
        <TypeAheadComponent
          options={options?.map(
            (item) =>
              `${item?.userEmail?.toLowerCase()}, ${getName(
                item?.displayName?.toLowerCase()
              )}, ${item?.userName?.toLowerCase()}`
          )}
          loader={autoLoader}
          userInput={owner}
          icon="search"
          name="owner"
          onSelected={(e, val) => onSelected(e, val)}
          onChange={(e) => onOwnerChange(e)}
          placeholder="Search by NTID, Email or Name "
          error={owner?.length > 2 && (emailError || !isValidEmail)}
          helperText={
            owner?.length > 2 && (emailError || !isValidEmail)
              ? 'Please enter a valid value or not available!'
              : ''
          }
        />
        <InstructionText>
          Search the T-Mobile system to add users
        </InstructionText>
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
