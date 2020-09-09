/* eslint-disable no-return-assign */
/* eslint-disable import/no-unresolved */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import { TitleOne } from 'styles/GlobalStyles';
import safeIcon from 'assets/icon_safes.svg';

const FolderWrap = styled('div')`
  position: relative;
  display: flex;
  width: 100%;
  text-decoration: none;
  align-items: center;
`;
const SafeDetailBox = styled('div')`
  padding-left: 1.7rem;
`;
const SafeAvatarWrap = styled.div`
  .MuiAvatar-root {
    width: 3.4rem;
    height: 3.9rem;
    border-radius: 0;
  }
  display: flex;
  align-items: center;
`;
const Flag = styled('span')`
  opacity: 0.7;
  margin-left: 0.5rem;
  font-size: ${(props) => props.fontSize};
  font-style: ${(props) => (props.fontStyle ? props.fontStyle : '')};
`;

const ListItem = (props) => {
  const { title, subTitle, flag, icon } = props;
  return (
    <ComponentError>
      <FolderWrap>
        <SafeAvatarWrap>
          <Avatar alt="safe_icon" src={icon} />
        </SafeAvatarWrap>
        <SafeDetailBox>
          <TitleOne>
            {title}
            <Flag fontSize="0.85rem" fontStyle="italic">
              {flag}
            </Flag>
          </TitleOne>
          <Flag fontSize="1rem">{subTitle}</Flag>
        </SafeDetailBox>
      </FolderWrap>
    </ComponentError>
  );
};
ListItem.propTypes = {
  subTitle: PropTypes.string,
  title: PropTypes.string,
  icon: PropTypes.string,
  flag: PropTypes.string,
};
ListItem.defaultProps = {
  subTitle: '',
  title: '',
  flag: '',
  icon: safeIcon,
};
export default ListItem;
