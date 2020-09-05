/* eslint-disable no-return-assign */
/* eslint-disable import/no-unresolved */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import ComponentError from 'errorBoundaries/ComponentError/component-error';

import FolderOutlinedIcon from '@material-ui/icons/FolderOutlined';

const FolderWrap = styled('div')`
  position: relative;
  display: flex;
  text-decoration: none;
  align-items: center;
`;
const SafeDetailBox = styled('div')`
  padding-left: 1em;
`;
const SafeAvatarWrap = styled.div`
  width: 4em;
  height: 4em;
`;
const SafeName = styled.div`
  font-size: 1.25rem;
`;
const Flag = styled('span')`
  opacity: 0.7;
  margin-left: 0.5rem;
  font-size: ${(props) => props.fontSize};
  font-style: ${(props) => (props.fontStyle ? props.fontStyle : '')};
`;

const FolderItem = (props) => {
  const { title, subTitle, flag, icon } = props;
  return (
    <ComponentError>
      {' '}
      <FolderWrap>
        <SafeAvatarWrap>
          <Avatar>{icon}</Avatar>
        </SafeAvatarWrap>
        <SafeDetailBox>
          <SafeName>
            {title}
            <Flag fontSize="0.85rem" fontStyle="italic">
              {flag}
            </Flag>
          </SafeName>
          <Flag fontSize="1rem">{subTitle}</Flag>
        </SafeDetailBox>
      </FolderWrap>
    </ComponentError>
  );
};
FolderItem.propTypes = {
  subTitle: PropTypes.string,
  title: PropTypes.string,
  icon: PropTypes.node,
  flag: PropTypes.string,
};
FolderItem.defaultProps = {
  subTitle: '',
  title: '',
  flag: '',
  icon: <FolderOutlinedIcon />,
};
export default FolderItem;
