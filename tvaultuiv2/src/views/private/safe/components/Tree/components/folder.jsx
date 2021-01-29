/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-props-no-spreading */
/* eslint-disable jsx-a11y/click-events-have-key-events */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import IconFolderActive from '../../../../../../assets/icon_folder_active.png';
import IconFolderInactive from '../../../../../../assets/icon_folder.png';

import ComponentError from '../../../../../../errorBoundaries/ComponentError/component-error';
import {
  TitleTwo,
  BackgroundColor,
} from '../../../../../../styles/GlobalStyles';
import {
  IconDeleteActive,
  // IconEdit,
  IconAddFolder,
  IconAddSecret,
} from '../../../../../../assets/SvgIcons';
import PopperElement from '../../Popper';

const FolderContainer = styled.div`
  padding-left: 0rem;
`;

const StyledFolder = styled.div`
  background: ${BackgroundColor.listBg};
  outline: none;
  :hover {
    background-image: ${(props) =>
      props.active ? props.theme.gradients.list : 'none'};
    color: #fff;
  }
  .folder--label {
    outline: none;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: ${(props) => props.padding || '0'};

    span {
      margin-left: 0.5rem;
    }
  }
`;

const Collapsible = styled.div`
  /* set the height depending on isOpen prop */
  height: ${(p) => (p.isOpen ? 'auto' : '0')};
  animation: accordian 0.4s 0s;
  /* hide the excess content */
  overflow: hidden;
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

const LabelWrap = styled.div`
  display: flex;
  align-items: center;
  padding-left: 2rem;
  width: 100%;
  cursor: pointer;
`;

const titleStyles = css`
  margin-left: 1.6rem;
`;

const Icon = styled('img')`
  width: 4rem;
  height: 4rem;
  margin-left: 0.8rem;
`;

const PopperItem = styled.div`
  padding: 0.5rem;
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
  cursor: pointer;
  span {
    margin-right: 0.75rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
`;

const Folder = (props) => {
  const {
    folderInfo,
    children,
    value,
    onFolderClosed,
    setOnFolderClosed,
    setInputType,
    setIsAddInput,
    getChildNodes,
    id,
    onDeleteTreeItem,
    setCurrentNode,
    userHavePermission,
  } = props;

  const [isOpen, setIsOpen] = useState(false);
  const [activeSecrets, setActiveSecrets] = useState([]);

  const handleToggle = (e) => {
    e.preventDefault();
    setIsOpen(!isOpen);
    setCurrentNode(id);
    setOnFolderClosed(!isOpen);
    if (!isOpen) getChildNodes(id);
  };

  useEffect(() => {
    if (onFolderClosed) {
      setIsOpen(false);
    }
  }, [onFolderClosed]);

  useEffect(() => setIsOpen(false), [value]);

  const handlePopperClick = (e, type) => {
    getChildNodes(id, undefined, undefined, false);
    setInputType(type);
    setIsAddInput(e);
    setIsOpen(e);
  };

  const handleActiveSecrets = (folder) => {
    const activeSecretsArr = [];
    activeSecretsArr.push(folder);
    setActiveSecrets([...activeSecretsArr]);
  };
  const labelValue = folderInfo?.value?.split('/')[
    folderInfo.value.split('/').length - 1
  ];

  // delete folder
  const deleteNode = (treeItem) => {
    onDeleteTreeItem(treeItem);
  };

  return (
    <ComponentError>
      <FolderContainer>
        <StyledFolder
          padding="1.2rem 0"
          onMouseEnter={() => handleActiveSecrets(labelValue)}
          onMouseLeave={() => setActiveSecrets([])}
          active={activeSecrets.includes(labelValue)}
        >
          <div role="button" className="folder--label" tabIndex={0}>
            <LabelWrap onClick={(e) => handleToggle(e)}>
              {isOpen ? <ExpandMoreIcon /> : <ChevronRightIcon />}

              {isOpen ? (
                <Icon alt="folder--icon" src={IconFolderActive} />
              ) : (
                <Icon alt="folder--icon" src={IconFolderInactive} />
              )}

              <TitleTwo extraCss={titleStyles}>{labelValue}</TitleTwo>
            </LabelWrap>

            {userHavePermission?.type === 'write' && (
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
                  <PopperItem
                    onClick={() =>
                      handlePopperClick(true, {
                        type: 'folder',
                        currentNode: folderInfo.value,
                      })
                    }
                  >
                    <IconAddFolder />
                    <span>Create Folder</span>
                  </PopperItem>
                  <PopperItem
                    onClick={() =>
                      handlePopperClick(true, {
                        type: 'secret',
                        currentNode: folderInfo.value,
                      })
                    }
                  >
                    <IconAddSecret />
                    <span>Create Secret</span>
                  </PopperItem>
                  <PopperItem
                    onClick={() =>
                      deleteNode({
                        id: folderInfo.id,
                        type: folderInfo.type,
                        parentId: folderInfo.parentId,
                      })
                    }
                  >
                    <IconDeleteActive />
                    <span> Delete</span>
                  </PopperItem>
                </PopperElement>
              </FolderIconWrap>
            )}
          </div>
        </StyledFolder>
        <Collapsible isOpen={isOpen}>{children}</Collapsible>
      </FolderContainer>
    </ComponentError>
  );
};

Folder.propTypes = {
  folderInfo: PropTypes.objectOf(PropTypes.any),
  children: PropTypes.node,
  setInputType: PropTypes.func,
  setIsAddInput: PropTypes.func,
  getChildNodes: PropTypes.func,
  setCurrentNode: PropTypes.func,
  id: PropTypes.string,
  onDeleteTreeItem: PropTypes.func,
  value: PropTypes.number.isRequired,
  userHavePermission: PropTypes.objectOf(PropTypes.any).isRequired,
  onFolderClosed: PropTypes.bool,
  setOnFolderClosed: PropTypes.func.isRequired,
};
Folder.defaultProps = {
  folderInfo: {},
  children: <div />,
  setInputType: () => {},
  setIsAddInput: () => {},
  getChildNodes: () => {},
  onDeleteTreeItem: () => {},
  setCurrentNode: () => {},
  id: '',
  onFolderClosed: true,
};

export default Folder;
