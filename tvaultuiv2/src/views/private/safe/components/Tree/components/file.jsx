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
  const { setInputType, setIsAddInput, secret } = props;
  const [viewSecretValue, setViewSecretValue] = useState(false);

  // handle popper click
  const handlePopperClick = (e, type) => {
    setInputType(type);
    setIsAddInput(e);
  };
  const toggleSecretValue = (val) => {
    setViewSecretValue(val);
  };

  const secretData = secret && JSON.parse(secret);
  const { data } = secretData;
  return (
    <ComponentError>
      <FileWrap>
        <StyledFile>
          <LabelWrap>
            <IconWrap>
              <IconLock />
            </IconWrap>
            <TitleThree>{data && Object.keys(data)[0]}</TitleThree>
          </LabelWrap>
          <SecretWrap type="password" viewSecret={viewSecretValue}>
            {data && Object.values(data)[0]}
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
              <PopperItem>
                <IconEdit />
                <span>Edit</span>
              </PopperItem>
              <PopperItem onClick={() => handlePopperClick(true, 'folder')}>
                <IconDeleteActive />
                <span>Delete</span>
              </PopperItem>
            </PopperElement>
          </FolderIconWrap>
        </StyledFile>
      </FileWrap>
    </ComponentError>
  );
};
File.propTypes = {
  setInputType: PropTypes.func,
  setIsAddInput: PropTypes.func,
  secret: PropTypes.string,
};
File.defaultProps = {
  setInputType: () => {},
  setIsAddInput: () => {},
  secret: '',
};
export default File;
