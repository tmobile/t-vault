/* eslint-disable react/jsx-curly-newline */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { InputLabel } from '@material-ui/core';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import { RequiredCircle, SubHeading } from '../../../../../styles/GlobalStyles';
import { BackArrow } from '../../../../../assets/SvgIcons';

const AddFolderNameWrapper = styled.div`
  padding: 5.5rem 6rem 6rem 6rem;
  background-color: ${(props) =>
    props.theme?.palette?.background?.modal
      ? props.theme.palette.background.modal
      : '#2a2e3e'};
  width: 70rem;
  ${mediaBreakpoints.semiLarge} {
    padding: 4.5rem 5rem 5rem 5rem;
  }
  ${mediaBreakpoints.small} {
    padding: 3rem 2rem 2rem 2rem;
    height: 100%;
    display: flex;
    flex-direction: column;
  }
`;

const FormWrapper = styled.form`
  margin-top: 5rem;
`;

const ButtonWrapper = styled('div')`
  display: flex;
  justify-content: flex-end;
  margin-top: 5rem;
  height: 100%;
`;

const SaveButton = styled.div`
  ${mediaBreakpoints.small} {
    align-self: flex-end;
    width: 100%;
  }
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${mediaBreakpoints.small} {
    width: 100%;
    align-self: flex-end;
  }
`;

const BackButton = styled.span`
  display: none;
  ${mediaBreakpoints.small} {
    display: flex;
    align-items: center;
    margin-right: 1.4rem;
  }
`;

const extraCss = css`
  display: flex;
`;

const AddFolder = (props) => {
  const { handleCancelClick, handleSaveClick, parentId, childrens } = props;
  const [inputValue, setInputValue] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [error, setError] = useState(null);
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  const handleValidation = (value) => {
    setErrorMessage(null);
    const isFolderExist = childrens.find((item) => {
      const arr = item.id.split('/');
      return arr[arr.length - 1].toLowerCase() === value.toLowerCase();
    });
    if (isFolderExist) {
      setErrorMessage(
        "Folder already exists, You can't store secrets in folders having same name "
      );
      setError(true);
    }
    if (value.length < 3 || !value.match(/^[a-zA-Z0-9_]*$/g)) {
      setError(true);
    } else {
      setError(false);
    }
  };

  const handleChange = (e) => {
    setInputValue(e.target.value);
    handleValidation(e.target.value);
  };
  return (
    <ComponentError>
      <AddFolderNameWrapper>
        <SubHeading extraCss={extraCss}>
          {isMobileScreen && (
            <BackButton onClick={() => handleCancelClick(false)}>
              <BackArrow />
            </BackButton>
          )}
          Add Folder
        </SubHeading>
        <FormWrapper>
          <InputLabel>
            Folder Name
            <RequiredCircle margin="0.5rem" />
          </InputLabel>
          <TextFieldComponent
            placeholder="Add folder"
            onChange={(e) => handleChange(e)}
            value={inputValue || ''}
            fullWidth
            name="folderName"
            error={error}
            helperText={
              errorMessage && errorMessage.includes('Folder already exist')
                ? errorMessage
                : 'Please enter a minimum of 3 characters lowercase alphabets, number and underscore only.'
            }
          />
        </FormWrapper>
        <ButtonWrapper>
          <CancelButton>
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleCancelClick(false)}
              width={isMobileScreen ? '100%' : ''}
            />
          </CancelButton>
          <SaveButton>
            <ButtonComponent
              label="Save"
              color="secondary"
              buttonType="containedSecondary"
              disabled={!inputValue || error}
              onClick={() =>
                handleSaveClick({
                  value: inputValue.toLowerCase(),
                  type: 'folder',
                  parentId,
                })
              }
              width={isMobileScreen ? '100%' : ''}
            />
          </SaveButton>
        </ButtonWrapper>
      </AddFolderNameWrapper>
    </ComponentError>
  );
};
AddFolder.propTypes = {
  handleCancelClick: PropTypes.func,
  handleSaveClick: PropTypes.func,
  parentId: PropTypes.string,
  childrens: PropTypes.arrayOf(PropTypes.any),
};
AddFolder.defaultProps = {
  handleSaveClick: () => {},
  handleCancelClick: () => {},
  parentId: '',
  childrens: [],
};
export default AddFolder;
