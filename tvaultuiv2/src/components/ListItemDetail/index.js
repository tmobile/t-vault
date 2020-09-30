/* eslint-disable react/forbid-prop-types */
/* eslint-disable react/require-default-props */
/* eslint-disable react/no-unused-prop-types */
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
// import { useHistory, useLocation } from 'react-router-dom';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import { BackArrow } from '../../assets/SvgIcons';
import mediaBreakpoints from '../../breakpoints';
import ListDetailHeader from '../ListDetailHeader';

// styled components goes here
const Section = styled('section')`
  flex-direction: column;
  display: flex;
  z-index: 2;
  width: 100%;
  height: 100%;
`;

const BackButton = styled.div`
  display: flex;
  align-items: center;
  padding: 2rem 0 0 2rem;
  span {
    margin-left: 1rem;
  }
`;

const ListItemDetail = (props) => {
  const { setActiveFolders, ListDetailHeaderBg, renderContent } = props;
  // use history of page
  //   const history = useHistory();
  //   const location = useLocation();
  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  // route component data
  const goBackToList = () => {
    setActiveFolders();
  };
  useEffect(() => {}, []);

  return (
    <ComponentError>
      <Section>
        {isMobileScreen ? (
          <BackButton onClick={goBackToList}>
            <BackArrow />
            {/* <span>{listItem.name || 'No safe'}</span> */}
          </BackButton>
        ) : null}

        <ListDetailHeader
          //   title={listItem?.name}
          //   description={listItem?.description}
          bgImage={ListDetailHeaderBg}
        />

        {renderContent}
      </Section>
    </ComponentError>
  );
};
ListItemDetail.propTypes = {
  detailData: PropTypes.arrayOf(PropTypes.array),
  params: PropTypes.objectOf(PropTypes.object),
  setActiveFolders: PropTypes.func,
  ListDetailHeaderBg: PropTypes.string,
  renderContent: PropTypes.node,
};
ListItemDetail.defaultProps = {
  detailData: [],
  params: {},
  setActiveFolders: () => {},
  ListDetailHeaderBg: '',
  renderContent: <div />,
};

export default ListItemDetail;
