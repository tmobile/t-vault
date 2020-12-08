import React, { useState } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../../errorBoundaries/ComponentError/component-error';
import {
  IconLock,
  IconDeleteActive,
  IconEdit,
} from '../../../../../../assets/SvgIcons';
import IconRefreshCC from '../../../../../../assets/refresh-ccw.svg';
import {
  TitleThree,
  BackgroundColor,
} from '../../../../../../styles/GlobalStyles';
import PopperElement from '../../Popper';

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
  padding-left: 2rem;
`;
const LabelWrap = styled('div')`
  display: flex;
  align-items: center;
`;
const IconWrap = styled('div')`
  margin-right: 1rem;
  display: flex;
  align-items: center;
`;
const SecretWrap = styled('div')`
  -webkit-text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  color: #5a637a;
`;
const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  cursor: pointer;
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
    }
  };

  return (
    <ComponentError>
      <FileWrap>
        <StyledFile>
          <LabelWrap>
            <IconWrap>
              <IconLock />
            </IconWrap>
            <TitleThree>{secret && Object.keys(secret)[0]}</TitleThree>
          </LabelWrap>
          <SecretWrap type="password" viewSecret={viewSecretValue}>
            {secret && Object.values(secret)[0]}
          </SecretWrap>
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
                <img alt="refersh-ic" src={IconRefreshCC} />
                <span>{viewSecretValue ? 'Hide secret' : 'View Secret'}</span>
              </PopperItem>
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
