/* eslint-disable import/no-unresolved */
import React from 'react';
import styled from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import PropTypes from 'prop-types';
import { IconLock, IconDeleteActive, IconEdit } from 'assets/SvgIcons';
import IconRefreshCC from 'assets/refresh-ccw.svg';
import { TitleThree, BackgroundColor } from 'styles/GlobalStyles';
import PopperElement from '../../Popper';

const StyledFile = styled.div`
  padding: 2rem 0 2rem 4.2rem;
  // padding-left: 2rem;
  background: ${BackgroundColor.secretBg};
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
  -webkit-text-security: disc;
  text-security: disc;
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
      background: #151820;
      border-radius: 50%;
    }
  }
`;
const PopperItem = styled.div`
  padding: 0.5em;
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
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
  const { secretKey, secretValue } = props;

  return (
    <ComponentError>
      <StyledFile>
        <LabelWrap>
          <IconWrap>
            <IconLock />
          </IconWrap>
          <TitleThree>{secretKey}</TitleThree>
        </LabelWrap>
        <SecretWrap>{secretValue}</SecretWrap>
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
              <img alt="refersh-ic" src={IconRefreshCC} />
              <span>Rotate Secret</span>
            </PopperItem>
            <PopperItem>
              <IconEdit />
              <span>Edit</span>
            </PopperItem>
            <PopperItem>
              <IconDeleteActive />
              <span>Delete</span>
            </PopperItem>
          </PopperElement>
        </FolderIconWrap>
      </StyledFile>
    </ComponentError>
  );
};
File.propTypes = {
  secretKey: PropTypes.string,
  secretValue: PropTypes.string,
};
File.defaultProps = {
  secretKey: '',
  secretValue: '',
};
export default File;
