/* eslint-disable no-return-assign */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import { TitleOne } from '../../../../../styles/GlobalStyles';
import safeIcon from '../../../../../assets/icon_safes.svg';
import { customColor } from '../../../../../theme';
import mediaBreakpoints from '../../../../../breakpoints';
import { IconDeleteActive, IconEdit } from '../../../../../assets/SvgIcons';
import PopperElement from '../Popper';

const FolderWrap = styled('div')`
  position: relative;
  display: flex;
  width: 100%;
  text-decoration: none;
  align-items: center;
  justify-content: space-between;
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

const FolderIconWrap = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  padding: 0.5rem;
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
  cursor: pointer;
  span {
    margin-right: 0.75rem;
  }
  :hover {
    background: ${customColor.magenta};
  }
`;
const LabelWrap = styled.div`
  display: flex;
  align-items: center;
`;

const ListItem = (props) => {
  const { title, subTitle, flag, icon, manage } = props;

  // scree handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  // const isDeskTopView = useMediaQuery(mediaBreakpoints.desktop);

  return (
    <ComponentError>
      <FolderWrap>
        <LabelWrap>
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
        </LabelWrap>
        {isMobileScreen && manage ? (
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
                <IconEdit />
                <span>Edit</span>
              </PopperItem>
              <PopperItem>
                <IconDeleteActive />
                <span> Delete</span>
              </PopperItem>
            </PopperElement>
          </FolderIconWrap>
        ) : (
          <></>
        )}
      </FolderWrap>
    </ComponentError>
  );
};
ListItem.propTypes = {
  subTitle: PropTypes.string,
  title: PropTypes.string,
  icon: PropTypes.string,
  flag: PropTypes.string,
  manage: PropTypes.bool.isRequired,
};
ListItem.defaultProps = {
  subTitle: '',
  title: '',
  flag: '',
  icon: safeIcon,
};
export default ListItem;
