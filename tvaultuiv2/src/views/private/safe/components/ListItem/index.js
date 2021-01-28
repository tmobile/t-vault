import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import { TitleOne } from '../../../../../styles/GlobalStyles';
import safeIcon from '../../../../../assets/icon_safes.svg';
import mediaBreakpoints from '../../../../../breakpoints';

const FolderWrap = styled('div')`
  position: relative;
  display: flex;
  width: 100%;
  height: 4rem;
  text-decoration: none;
  align-items: center;
  justify-content: space-between;
`;
const SafeDetailBox = styled('div')`
  padding-left: 1.7rem;
  width: 80%;
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
  opacity: 1;
  font-size: ${(props) => props.fontSize};
  font-style: ${(props) => (props.fontStyle ? props.fontStyle : '')};
`;

const LabelWrap = styled.div`
  display: flex;
  align-items: center;
  width: 100%;
`;

const extraCss = css`
  overflow: hidden;
  text-overflow: ellipsis;
  ${mediaBreakpoints.belowLarge} {
    width: 28rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 1.6rem;
  }
  ${mediaBreakpoints.smallAndMedium} {
    width: 17rem;
  }
`;

const ListItem = (props) => {
  const { title, subTitle, flag, icon } = props;
  return (
    <ComponentError>
      <FolderWrap>
        <LabelWrap>
          <SafeAvatarWrap>
            <Avatar alt="safe_icon" src={icon} />
          </SafeAvatarWrap>
          <SafeDetailBox>
            <TitleOne extraCss={extraCss}>
              {title}
              <Flag fontSize="0.85rem" fontStyle="italic">
                {flag}
              </Flag>
            </TitleOne>
            <Flag fontSize="1.3rem">{subTitle}</Flag>
          </SafeDetailBox>
        </LabelWrap>
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
