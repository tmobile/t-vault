import React from 'react';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import mediaBreakpoints from '../../breakpoints';
import { TitleFour } from '../../styles/GlobalStyles';

const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  position: relative;
  height: 17.1rem;
  padding: 2rem;
  .list-title-wrap {
    width: 70%;
  }
  ${mediaBreakpoints.small} {
    height: 18rem;
    padding: 1rem;
  }
`;
const ListTitle = styled('h5')`
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
  ${mediaBreakpoints.small} {
    z-index: -1;
  }
`;

const ListDetailHeader = (props) => {
  const { title, description, bgImage } = props;
  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  return (
    <ColumnHeader>
      <HeaderBg bgImage={bgImage} />
      <div className="list-title-wrap">
        {!isMobileScreen && <ListTitle>{title || 'No Title'}</ListTitle>}
        <TitleFour color="#c4c4c4">
          {description ||
            'Create a service to see your secrets, folders and permissions here'}
        </TitleFour>
      </div>
    </ColumnHeader>
  );
};

ListDetailHeader.propTypes = {
  title: PropTypes.string,
  description: PropTypes.string,
  bgImage: PropTypes.string,
};
ListDetailHeader.defaultProps = {
  title: '',
  description: '',
  bgImage: '',
};
export default ListDetailHeader;
