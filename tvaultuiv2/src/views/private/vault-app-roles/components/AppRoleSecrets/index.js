/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState } from 'react';
import styled, { css } from 'styled-components';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import { makeStyles } from '@material-ui/core/styles';
import Checkbox from '@material-ui/core/Checkbox';
import PropTypes from 'prop-types';
// import VisibilityIcon from '@material-ui/icons/Visibility';
// import VisibilityOffIcon from '@material-ui/icons/VisibilityOff';

import useMediaQuery from '@material-ui/core/useMediaQuery';
import FileCopyIcon from '@material-ui/icons/FileCopy';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import lock from '../../../../../assets/icon_lock.svg';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import mediaBreakpoints from '../../../../../breakpoints';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import {
  PopperItem,
  BackgroundColor,
} from '../../../../../styles/GlobalStyles';
import PopperElement from '../../../../../components/Popper';

const SecretsList = styled.div`
  margin-top: 2rem;
  justify-content: space-between;
  align-items: center;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0 0;
  height: calc(100% - 3.8rem);
  overflow: auto;
  border-bottom: 1px solid #323649;
`;

const Secret = styled.div`
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  word-break: break-all;
`;

const customStyle = css`
  height: 100%;
`;

const Icon = styled.img`
  width: 1.5rem;
  margin-right: 1.6rem;
  margin-left: 2rem;
`;

const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  cursor: pointer;
  .MuiSvgIcon-root {
    width: 2.5rem;
    height: 2.5rem;
    :hover {
      background: ${(props) =>
        props.theme.customColor.hoverColor.list || '#151820'};
      border-radius: 50%;
    }
  }
`;
const SecretIdWrap = styled.div`
  display: flex;
  align-items: Center;
  justify-content: space-between;
  background: ${BackgroundColor.secretBg || '#2a2e3e'};
  padding: 1.2rem 0;
  text-overflow: ellipsis;
  :hover {
    background: ${BackgroundColor.secretHoverBg};
  }
`;
const checkBoxStyles = makeStyles((theme) => ({
  root: {
    color: theme.customColor.checkbox.color,
    '&$checked': {},
  },
  checked: {},
}));
const AppRoleSecrets = (props) => {
  const { secretIds, deleteSecretIds } = props;
  const [deleteSecretId, setDeleteSecretId] = useState(false);
  const [status, setStatus] = useState(null);
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [checkedSecretIds, setCheckedSecretIds] = useState([]);
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const checkBoxClasses = checkBoxStyles();
  /**
   * @function handleClose
   * @description function to handle opening and closing of confirmation modal.
   */
  const handleClose = () => {
    setOpenConfirmationModal(false);
  };

  /**
   * @function onCopyClicked
   * @description function to copy the secret.
   */
  const onCopyClicked = () => {
    // setResponseType(1);
    setStatus({ status: 'success', message: 'Secret copied to clipboard' });
  };

  /**
   * @function onDeleteClicked
   * @description function to reset secret when the confirm is clicked.
   */
  const onDeleteClicked = () => {
    // const payload = {};
    setOpenConfirmationModal(false);
    deleteSecretIds(deleteSecretId);
  };
  const onDeleteSecretId = (secretId) => {
    setOpenConfirmationModal(true);
    setDeleteSecretId(secretId);
  };

  /**
   * @function handleSecretCheckbox
   * @param secretId id of secret to check
   */

  const handleSecretCheckbox = (secretId) => {
    const tempChecks = [...checkedSecretIds];
    const indexOfSecretToCheck = tempChecks.indexOf(secretId);
    if (tempChecks?.includes(secretId)) {
      tempChecks.splice(indexOfSecretToCheck, 1);
      setCheckedSecretIds([...tempChecks]);
      return;
    }
    tempChecks.push(secretId);
    setCheckedSecretIds([...tempChecks]);
  };
  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleClose}
          title="Confirmation"
          description="Are you sure you want to Delete the secretId?"
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleClose()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
          confirmButton={
            <ButtonComponent
              label="Confirm"
              color="secondary"
              onClick={() => onDeleteClicked()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
        {status?.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        <SecretsList>
          {secretIds?.map((secretId) => (
            <SecretIdWrap key={secretId}>
              <Secret>
                {' '}
                <Checkbox
                  checked={checkedSecretIds?.includes(secretId)}
                  color="secondary"
                  classes={checkBoxClasses}
                  inputProps={{ 'aria-label': 'secondary checkbox' }}
                  onChange={() => handleSecretCheckbox(secretId)}
                />
                <Icon src={lock} alt="lock" />
                <span>{secretId}</span>
              </Secret>

              <FolderIconWrap>
                <PopperElement
                  anchorOrigin={{
                    vertical: 'bottom',
                    horizontal: 'right',
                  }}
                  transformOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                  }}
                >
                  <PopperItem onClick={() => onDeleteSecretId(secretId)}>
                    <span>Delete</span>
                  </PopperItem>
                  <CopyToClipboard
                    text={secretId}
                    onCopy={() => onCopyClicked()}
                  >
                    <PopperItem>
                      <FileCopyIcon />
                      <span>Copy SecretId</span>
                    </PopperItem>
                  </CopyToClipboard>
                </PopperElement>
              </FolderIconWrap>
            </SecretIdWrap>
          ))}
        </SecretsList>
      </>
    </ComponentError>
  );
};

AppRoleSecrets.propTypes = {
  secretIds: PropTypes.arrayOf(PropTypes.array).isRequired,
  deleteSecretIds: PropTypes.func,
};

AppRoleSecrets.defaultProps = { deleteSecretIds: () => {} };

export default AppRoleSecrets;
