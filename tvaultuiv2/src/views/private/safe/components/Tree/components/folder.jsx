/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import MoreVertOutlinedIcon from '@material-ui/icons/MoreVertOutlined';
import FolderOutlinedIcon from '@material-ui/icons/FolderOutlined';
import PopperElement from 'components/Popper';
import ComponentError from 'errorBoundaries/ComponentError/component-error';

const StyledFolder = styled.div`
  padding-left: 20px;

  .folder--label {
    display: flex;
    align-items: center;
    span {
      margin-left: 5px;
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
          <FolderOutlinedIcon />
          <span>{name}</span>
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
