import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import FileCopyIcon from '@material-ui/icons/FileCopy';
import VisibilityIcon from '@material-ui/icons/Visibility';
import VisibilityOffIcon from '@material-ui/icons/VisibilityOff';
import ComponentError from '../../../../../../errorBoundaries/ComponentError/component-error';
import {
  IconLock,
  IconDeleteActive,
  IconEdit,
} from '../../../../../../assets/SvgIcons';
import {
  BackgroundColor,
  TitleOne,
} from '../../../../../../styles/GlobalStyles';
import PopperElement from '../../Popper';
import SnackbarComponent from '../../../../../../components/Snackbar';

const StyledFile = styled.div`
  background: ${BackgroundColor.secretBg};
  padding: 1.2rem 0 1.2rem 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  :hover {
    background: ${BackgroundColor.secretHoverBg};
  }
  span {
    margin-left: 5px;
  }
`;
const FileWrap = styled.div`
  padding-left: 0rem;
`;
const LabelWrap = styled('div')`
  display: flex;
  align-items: center;
  width: 33%;
`;
const IconWrap = styled('div')`
  margin-right: 1rem;
  display: flex;
  align-items: center;
`;
const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  cursor: pointer;
  width: 33%;
  justify-content: flex-end;
  .MuiSvgIcon-root {
    width: 2.4rem;
    height: 2.4rem;
    :hover {
      background: ${(props) =>
        props.theme.customColor.hoverColor.list || '#151820'};
      border-radius: 50%;
    }
  }
`;
const PopperItem = styled.div`
  padding: 0.5em;
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
  cursor: pointer;
  span {
    margin-right: 0.75rem;
  }
  img {
    width: 2rem;
    height: 2rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
`;

const SecretInputfield = styled.input`
  padding: 0;
  outline: none;
  border: none;
  background: transparent;
  font-size: 1.2rem;
  color: #5a637a;
  word-break: break-all;
  font-size: 1.8rem;
  text-align: left;
  width: 33%;
`;

const extraCss = css`
  word-break: break-all;
  margin-right: 17px;
`;

const File = (props) => {
  const {
    parentId,
    secret,
    id,
    onDeleteTreeItem,
    setSecretprefilledData,
    type,
    setIsAddInput,
    setInputType,
    userHavePermission,
  } = props;
  const [viewSecretValue, setViewSecretValue] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [secretArray, setSecretArray] = useState([]);

  const toggleSecretValue = (val) => {
    setViewSecretValue(val);
  };

  // delete folder
  const deleteNode = (treeItem) => {
    onDeleteTreeItem(treeItem);
  };

  const editNode = () => {
    setIsAddInput(true);
    setInputType({
      type: 'secret',
      currentNode: id,
    });
    if (secret) {
      setSecretprefilledData(secret);
    } else {
      setSecretprefilledData({});
    }
  };

  const onCopyClicked = () => {
    setResponseType(1);
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  useEffect(() => {
    if (secret && Object.keys(secret).length > 0) {
      setSecretArray([
        { name: 'Copy Key', value: Object.keys(secret)[0] },
        { name: 'Copy Secret', value: Object.values(secret)[0] },
      ]);
    }
    setViewSecretValue(false);
  }, [secret]);

  return (
    <ComponentError>
      <FileWrap>
        <StyledFile>
          <LabelWrap>
            <IconWrap>
              <IconLock />
            </IconWrap>
            <TitleOne extraCss={extraCss}>
              {secret && Object.keys(secret)[0]}
            </TitleOne>
          </LabelWrap>
          <SecretInputfield
            type={viewSecretValue ? 'text' : 'password'}
            value={secret && Object.values(secret)[0]}
            readOnly
          />
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
              <PopperItem onClick={() => toggleSecretValue(!viewSecretValue)}>
                {viewSecretValue ? <VisibilityOffIcon /> : <VisibilityIcon />}
                <span>{viewSecretValue ? 'Hide secret' : 'View Secret'}</span>
              </PopperItem>
              {secretArray?.length > 0 &&
                secretArray.map((item) => (
                  <CopyToClipboard
                    text={item.value}
                    onCopy={() => onCopyClicked()}
                  >
                    <PopperItem>
                      <FileCopyIcon />
                      <span>{item.name}</span>
                    </PopperItem>
                  </CopyToClipboard>
                ))}
              {userHavePermission?.type === 'write' && (
                <PopperItem onClick={() => editNode()}>
                  <IconEdit />
                  <span>Edit</span>
                </PopperItem>
              )}
              {userHavePermission?.type === 'write' && (
                <PopperItem
                  onClick={
                    () =>
                      deleteNode({
                        id,
                        type,
                        key: Object.keys(secret)[0],
                        parentId,
                      })
                    // eslint-disable-next-line react/jsx-curly-newline
                  }
                >
                  <IconDeleteActive />
                  <span>Delete</span>
                </PopperItem>
              )}
            </PopperElement>
          </FolderIconWrap>
        </StyledFile>
        {responseType === 1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message="Copied successfully!"
          />
        )}
      </FileWrap>
    </ComponentError>
  );
};
File.propTypes = {
  secret: PropTypes.objectOf(PropTypes.any),
  id: PropTypes.string,
  onDeleteTreeItem: PropTypes.func,
  parentId: PropTypes.string,
  type: PropTypes.string,
  setIsAddInput: PropTypes.func,
  setInputType: PropTypes.func,
  setSecretprefilledData: PropTypes.func,
  userHavePermission: PropTypes.objectOf(PropTypes.any).isRequired,
};
File.defaultProps = {
  onDeleteTreeItem: () => {},
  setIsAddInput: () => {},
  setSecretprefilledData: () => {},
  secret: {},
  id: '',
  parentId: '',
  type: '',
  setInputType: 'secret',
};
export default File;
