import React from 'react';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import mediaBreakpoints from '../../../../../breakpoints';
import { TitleFour } from '../../../../../styles/GlobalStyles';

const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  position: relative;
  height: 17.1rem;
  padding: 2rem;
  background-color: #151820;
  .safe-title-wrap {
    width: 70%;
  }
  ${mediaBreakpoints.small} {
    height: 13rem;
    padding: 1rem;
  }
`;
const SafeTitle = styled('h5')`
  font-size: ${(props) => props.theme.typography};
  margin: 1rem 0 1.2rem;
  text-overflow: ellipsis;
  overflow: hidden;
`;
const HeaderBg = styled('div')`
  position: absolute;
  top: -0.8rem;
  left: 0;
  right: 0;
  bottom: 0;
  background: url(${(props) => props.bgImage || ''});
`;

const SafeDetailHeader = (props) => {
  const { title, description, bgImage } = props;

  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  return (
    <ColumnHeader>
      <HeaderBg bgImage={bgImage} />
      <div className="safe-title-wrap">
        {!isMobileScreen && <SafeTitle>{title || '...'}</SafeTitle>}
        <TitleFour color="#c4c4c4">
          {description ||
            'Create a Safe to see your secrets, folders and permissions here'}
        </TitleFour>
      </div>
    </ColumnHeader>
  );
};

SafeDetailHeader.propTypes = {
  title: PropTypes.string,
  description: PropTypes.string,
  bgImage: PropTypes.string,
};
SafeDetailHeader.defaultProps = {
  title: '',
  description: '',
  bgImage: '',
};
export default SafeDetailHeader;
