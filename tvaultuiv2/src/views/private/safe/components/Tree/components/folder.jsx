/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import MoreVertOutlinedIcon from '@material-ui/icons/MoreVertOutlined';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ExpandLessIcon from '@material-ui/icons/ExpandLess';
import IconFolderActive from 'assets/icon_folder_active';
import IconFolderInactive from 'assets/icon_folder';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import { TitleTwo, BackgroundColor } from 'styles/GlobalStyles';
import PopperElement from '../../Popper';

const StyledFolder = styled.div`
  background: ${BackgroundColor.listBg};
  padding-left: ${(props) => props.padding || '0'};
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
    color: #fff;
  }
  .folder--label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    span {
      margin-left: 0.5rem;
    }
  }
`;
const Collapsible = styled.div`
  /* set the height depending on isOpen prop */
  height: ${(p) => (p.isOpen ? 'auto' : '0')};
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

const Folder = (props) => {
  const { name, children, popperClickHandler } = props;
  const [isOpen, setIsOpen] = useState(false);
  const [isPopperOpen, setIsPopperOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const handleToggle = (e) => {
    e.preventDefault();
    setIsOpen(!isOpen);
  };

  const enablePopper = (e) => {
    setIsPopperOpen(true);
    setAnchorEl(e.currentTarget);
  };

  return (
    <ComponentError>
      <StyledFolder>
        <div
          role="button"
          className="folder--label"
          onClick={handleToggle}
          tabIndex={0}
        >
          <LabelWrap>
            {isOpen ? <ExpandMoreIcon /> : <ExpandLessIcon />}
            <IconFolderActive />
            <TitleTwo extraCss={titleStyles}>{name}</TitleTwo>
          </LabelWrap>

          <FolderIconWrap onClick={(e) => enablePopper(e)}>
            <MoreVertOutlinedIcon />
            <PopperElement
              open={isPopperOpen}
              popperContent={<span>create folder</span>}
              position="left-start"
              anchorEl={anchorEl}
              handlePopperClick={popperClickHandler}
            />
          </FolderIconWrap>
        </div>
        <Collapsible isOpen={isOpen}>{children}</Collapsible>
      </StyledFolder>
    </ComponentError>
  );
};

Folder.propTypes = {
  name: PropTypes.string,
  children: PropTypes.node,
  popperClickHandler: PropTypes.func,
};
Folder.defaultProps = {
  name: '',
  children: <div />,
  popperClickHandler: () => {},
};

export default Folder;
