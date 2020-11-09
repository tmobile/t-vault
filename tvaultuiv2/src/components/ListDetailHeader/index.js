import React from 'react';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import ReactHtmlParser from 'react-html-parser';
import mediaBreakpoints from '../../breakpoints';
import { BackArrow } from '../../assets/SvgIcons';

const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  position: relative;
  height: 17.1rem;
  padding: 2rem;
  .list-title-wrap {
    width: 70%;
    z-index: 2;
    margin-left: 9rem;
    margin-bottom: 3rem;
  }
  ${mediaBreakpoints.small} {
    height: 18rem;
    padding: 1rem;
    flex-direction: column;
  }
`;
const ListTitle = styled('h5')`
  font-size: ${(props) => props.theme.typography.h5};
  margin: 1rem 0 1.2rem;
  text-overflow: ellipsis;
  overflow: hidden;
  text-transform: capitalize;
  ${mediaBreakpoints.medium} {
    font-size: 1.8rem;
  }
`;

const Description = styled.div`
  font-size: 1.4rem;
  color: #c4c4c4;
  ${mediaBreakpoints.medium} {
    font-size: 1.2rem;
  }
  ${mediaBreakpoints.medium} {
    font-size: 1.3rem;
  }
`;

const BackButton = styled.div`
  display: flex;
  align-items: center;
  padding: 2rem 0 0 2rem;
  cursor: pointer;
  position: absolute;
  top: 0;
  left: 0;
  span {
    margin-left: 1rem;
    text-transform: capitalize;
  }
`;
const HeaderBg = styled('div')`
  position: absolute;
  top: -0.8rem;
  left: 0;
  right: 0;
  bottom: 0;
  background: url(${(props) => props.bgImage || ''});
  background-repeat: no-repeat;
  ${mediaBreakpoints.medium} {
    top: 0;
  }
  ${mediaBreakpoints.small} {
    z-index: -1;
  }
`;

const ListDetailHeader = (props) => {
  const { title, description, bgImage, goBackToSafeList } = props;
  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  return (
    <ColumnHeader>
      <HeaderBg bgImage={bgImage} />
      {isMobileScreen ? (
        <BackButton onClick={goBackToSafeList}>
          <BackArrow />
          <span>{title}</span>
        </BackButton>
      ) : null}
      <div className="list-title-wrap">
        {!isMobileScreen && <ListTitle>{title}</ListTitle>}
        <Description>
          {ReactHtmlParser(description) ||
            'Create a service to see your secrets, folders and permissions here'}
        </Description>
      </div>
    </ColumnHeader>
  );
};

ListDetailHeader.propTypes = {
  title: PropTypes.string,
  description: PropTypes.string,
  bgImage: PropTypes.string,
  goBackToSafeList: PropTypes.func,
};
ListDetailHeader.defaultProps = {
  title: '',
  description: '',
  bgImage: '',
  goBackToSafeList: () => {},
};
export default ListDetailHeader;
