/* eslint-disable react/jsx-props-no-spreading */
/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ExpandLessIcon from '@material-ui/icons/ExpandLess';
import IconFolderActive from 'assets/icon_folder_active.png';
import IconFolderInactive from 'assets/icon_folder.png';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import { TitleTwo, BackgroundColor } from 'styles/GlobalStyles';
import {
  IconDeleteActive,
  IconEdit,
  IconAddFolder,
  IconAddSecret,
} from 'assets/SvgIcons';
import PopperElement from '../../Popper';

const StyledFolder = styled.div`
  background: ${BackgroundColor.listBg};
  padding-left: ${(props) => (props.parent ? '0' : '2rem')};
  outline: none;
  .folder--label {
    outline: none;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: ${(props) => props.padding || '0'};
    :hover {
      background-image: ${(props) => props.theme.gradients.list || 'none'};
      color: #fff;
    }
    span {
      margin-left: 0.5rem;
    }
  }
`;
// const accordian = keyframes`
//   0% { transform:translateY(-10em); }
//   100% { transform:translateY(0); }
// `;

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
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
    :hover {
      background: #151820;
      border-radius: 50%;
    }
  }
`;

const LabelWrap = styled.div`
  display: flex;
  align-items: center;
  padding-left: 2rem;
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
  span {
    margin-right: 0.75rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
`;
const Folder = (props) => {
  const { folderInfo, children } = props;
  const [isOpen, setIsOpen] = useState(false);
  const handleToggle = (e) => {
    e.preventDefault();
    setIsOpen(!isOpen);
  };
  return (
    <ComponentError>
      <StyledFolder parent={folderInfo.parent} padding="1.2rem 0">
        <div
          role="button"
          className="folder--label"
          onClick={(e) => handleToggle(e)}
          tabIndex={0}
        >
          <LabelWrap>
            {isOpen ? <ExpandMoreIcon /> : <ExpandLessIcon />}

            {isOpen ? (
              <Icon alt="folder--icon" src={IconFolderActive} />
            ) : (
              <Icon alt="folder--icon" src={IconFolderInactive} />
            )}

            <TitleTwo extraCss={titleStyles}>{folderInfo.labelText}</TitleTwo>
          </LabelWrap>

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
              <PopperItem>
                <IconAddFolder />
                <span>Create Folder</span>
              </PopperItem>
              <PopperItem>
                <IconAddSecret />
                <span>Create Secret</span>
              </PopperItem>
              <PopperItem>
                <IconEdit />
                <span>Edit</span>
              </PopperItem>
              <PopperItem>
                <IconDeleteActive />
                <span> Delete</span>
              </PopperItem>
            </PopperElement>
          </FolderIconWrap>
        </div>
        <Collapsible isOpen={isOpen}>{children}</Collapsible>
      </StyledFolder>
    </ComponentError>
  );
};

Folder.propTypes = {
  folderInfo: PropTypes.objectOf(PropTypes.object),
  children: PropTypes.node,
};
Folder.defaultProps = {
  folderInfo: {},
  children: <div />,
};

export default Folder;
