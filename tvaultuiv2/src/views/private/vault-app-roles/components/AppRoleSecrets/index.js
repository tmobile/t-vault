/* eslint-disable react/jsx-one-expression-per-line */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState } from 'react';
import styled, { css } from 'styled-components';
import { CopyToClipboard } from 'react-copy-to-clipboard';
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0;
  border-bottom: 1px solid #323649;
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
`;

const Secret = styled.div`
  font-size: 1.2rem;
  color: #5a637a;
  word-break: break-all;
  text-overflow: ellipsis;
`;

const customStyle = css`
  height: 100%;
`;

const Icon = styled.img`
  width: 2.2rem;
  height: 2.2rem;
  margin-right: 1.6rem;
  margin-left: 2rem;
`;

const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  cursor: pointer;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
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
`;

const AppRoleSecrets = (props) => {
  const { secretIds, deleteSecretIds } = props;
  const [deleteSecretId, setDeleteSecretId] = useState(false);
  const [status, setStatus] = useState(null);
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

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
        {status.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        <SecretsList>
          {secretIds?.map((secretId) => (
            <SecretIdWrap key={secretId}>
              <Icon src={lock} alt="lock" />

              <Secret>{secretId}</Secret>

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
