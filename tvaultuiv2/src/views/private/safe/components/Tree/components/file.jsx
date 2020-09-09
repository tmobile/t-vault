/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import styled from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import PropTypes from 'prop-types';
import IconLock from 'assets/icon_lock';
import MoreVertOutlinedIcon from '@material-ui/icons/MoreVertOutlined';
import { TitleThree, BackgroundColor } from 'styles/GlobalStyles';
import PopperElement from '../../Popper';

const StyledFile = styled.div`
  padding-left: 20px;
  background: ${BackgroundColor.secretBg};
  display: flex;
  align-items: center;
  :hover {
    background: ${BackgroundColor.secretHoverBg};
  }
  span {
    margin-left: 5px;
  }
`;
const LabelWrap = styled('div')``;
const SecretWrap = styled('div')``;
const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
  }
`;
const File = (props) => {
  const { secretKey, secretValue } = props;
  const [isPopperOpen, setIsPopperOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);

  const enablePopper = (e) => {
    setIsPopperOpen(true);
    setAnchorEl(e.currentTarget);
  };

  return (
    <ComponentError>
      <StyledFile>
        <LabelWrap>
          <IconLock />
          <TitleThree>{secretKey}</TitleThree>
        </LabelWrap>
        <SecretWrap>{secretValue}</SecretWrap>
        <FolderIconWrap onClick={(e) => enablePopper(e)}>
          <MoreVertOutlinedIcon />
          <PopperElement
            open={isPopperOpen}
            position="left-start"
            anchorEl={anchorEl}
          >
            <div>create folder</div>
            <div>create secret</div>
            <div>edit</div>
            <div>delete</div>
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
