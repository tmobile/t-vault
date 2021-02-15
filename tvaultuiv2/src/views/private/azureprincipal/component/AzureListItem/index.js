import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import { TitleOne } from '../../../../../styles/GlobalStyles';

const FolderWrap = styled('div')`
  position: relative;
  display: flex;
  width: 100%;
  text-decoration: none;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
`;
const ListItemDetailBox = styled('div')`
  padding-left: 1.7rem;
  width: 80%;
`;
const ListItemAvatarWrap = styled.div`
  .MuiAvatar-root {
    border-radius: 0;
    width: 5rem;
  }
  display: flex;
  align-items: center;
`;

const LabelWrap = styled.div`
  display: flex;
  align-items: center;
  width: 100%;
`;
const ListTitleStyles = css`
  color: #d0d0d0;
  text-overflow: ellipsis;
  overflow: hidden;
`;

const AzureListItem = (props) => {
  const { title, icon } = props;

  return (
    <ComponentError>
      <FolderWrap>
        <LabelWrap>
          <ListItemAvatarWrap>
            <Avatar alt="ListItem_icon" src={icon} />
          </ListItemAvatarWrap>
          <ListItemDetailBox>
            <TitleOne extraCss={ListTitleStyles}>{title}</TitleOne>
          </ListItemDetailBox>
        </LabelWrap>
      </FolderWrap>
    </ComponentError>
  );
};
AzureListItem.propTypes = {
  title: PropTypes.string,
  icon: PropTypes.string.isRequired,
};
AzureListItem.defaultProps = {
  title: '',
};
export default AzureListItem;
