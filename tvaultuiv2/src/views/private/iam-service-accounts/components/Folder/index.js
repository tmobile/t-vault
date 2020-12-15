/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-props-no-spreading */
/* eslint-disable jsx-a11y/click-events-have-key-events */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import IconFolderActive from '../../../../../assets/icon_folder_active.png';
import IconFolderInactive from '../../../../../assets/icon_folder.png';

import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import { TitleTwo, BackgroundColor } from '../../../../../styles/GlobalStyles';

const FolderContainer = styled.div``;

const StyledFolder = styled.div`
  background: ${BackgroundColor.listBg};
  cursor: pointer;
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

const LabelWrap = styled.div`
  display: flex;
  align-items: center;
  padding-left: 2rem;
  width: 100%;
`;

const titleStyles = css`
  margin-left: 1.6rem;
`;

const Icon = styled('img')`
  width: 4rem;
  height: 4rem;
  margin-left: 0.8rem;
`;

const Folder = (props) => {
  const { labelValue, children, onClick } = props;
  const [isOpen, setIsOpen] = useState(false);
  const [activeSecrets, setActiveSecrets] = useState([]);

  const handleToggle = (e) => {
    e.preventDefault();
    setIsOpen(!isOpen);
    if (!isOpen) {
      onClick(labelValue);
    }
  };

  const handleActiveSecrets = (folder) => {
    const activeSecretsArr = [];
    activeSecretsArr.push(folder);
    setActiveSecrets([...activeSecretsArr]);
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
          </div>
        </StyledFolder>
        <Collapsible isOpen={isOpen}>{children}</Collapsible>
      </FolderContainer>
    </ComponentError>
  );
};

Folder.propTypes = {
  labelValue: PropTypes.string,
  children: PropTypes.node,
  onClick: PropTypes.func,
};
Folder.defaultProps = {
  labelValue: '',
  children: <div />,
  onClick: () => {},
};

export default Folder;
