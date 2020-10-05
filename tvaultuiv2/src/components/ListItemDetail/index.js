/* eslint-disable react/forbid-prop-types */
/* eslint-disable react/require-default-props */
/* eslint-disable react/no-unused-prop-types */
import React from 'react';
import PropTypes from 'prop-types';
// import { useHistory, useLocation } from 'react-router-dom';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import { BackArrow } from '../../assets/SvgIcons';
import mediaBreakpoints from '../../breakpoints';
import ListDetailHeader from '../ListDetailHeader';
import { TitleOne } from '../../styles/GlobalStyles';

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

  ${mediaBreakpoints.small} {
    position: absolute;
    z-index: 2;
  }
`;

const ListItemDetail = (props) => {
  const {
    ListDetailHeaderBg,
    renderContent,
    listItemDetails,
    backToLists,
  } = props;
  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  // route component data
  const goBackToList = () => {
    backToLists();
  };

  return (
    <ComponentError>
      <Section>
        {isMobileScreen ? (
          <BackButton onClick={goBackToList}>
            <BackArrow />
            <TitleOne extraCss="font-weight:bold;margin-left:1rem;">
              {listItemDetails.name || ''}
            </TitleOne>
          </BackButton>
        ) : null}

        <ListDetailHeader
          title={listItemDetails?.name}
          description={listItemDetails?.description}
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
  ListDetailHeaderBg: PropTypes.string,
  renderContent: PropTypes.node,
  listItemDetails: PropTypes.objectOf(PropTypes.object),
  backToLists: PropTypes.func,
};
ListItemDetail.defaultProps = {
  detailData: [],
  params: {},
  ListDetailHeaderBg: '',
  renderContent: <div />,
  listItemDetails: {},
  backToLists: () => {},
};

export default ListItemDetail;
