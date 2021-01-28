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
  background-color: #151820;
  padding: 2rem;
  .list-title-wrap {
    width: 67%;
    z-index: 2;
    ${mediaBreakpoints.small} {
      width: 60%;
      margin-left: 11rem;
      margin-bottom: 2rem;
    }
    ${mediaBreakpoints.medium} {
      width: 60%;
    }
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
  ${mediaBreakpoints.small} {
    font-size: 1.1rem;
    p {
      margin: 0;
    }
  }
`;

const BackButton = styled.div`
  display: flex;
  align-items: center;
  cursor: pointer;
  position: absolute;
  top: 1.5rem;
  left: 2rem;
  span {
    margin-left: 1rem;
    font-size: 1.8rem;
    font-weight: bold;
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
    ${mediaBreakpoints.small} {
      width: 25rem;
    }
  }
`;

const HeaderBg = styled('div')`
  position: absolute;
  top: -0.8rem;
  left: 0;
  right: 0;
  bottom: 0rem;
  background: url(${(props) => props.bgImage || ''});
  background-repeat: no-repeat;
  background-size: cover;
  ${mediaBreakpoints.medium} {
    top: 0;
  }
  ${mediaBreakpoints.small} {
    z-index: -1;
  }
`;

const ListDetailHeader = (props) => {
  const { title, description, bgImage, goBackToList } = props;
  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  return (
    <ColumnHeader>
      <HeaderBg bgImage={bgImage} />
      {isMobileScreen ? (
        <BackButton onClick={goBackToList}>
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
  goBackToList: PropTypes.func,
};
ListDetailHeader.defaultProps = {
  title: '',
  description: '',
  bgImage: '',
  goBackToList: () => {},
};
export default ListDetailHeader;
