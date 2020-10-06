import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useLocation } from 'react-router-dom';
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
    backToLists,
    description,
    listItemDetails,
  } = props;
  const location = useLocation();
  const [data, setData] = useState({});
  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  useEffect(() => {
    if (location?.state?.data) {
      setData(location?.state?.data);
    }
    if (listItemDetails) {
      setData({ ...listItemDetails });
    }
  }, [location.state, listItemDetails]);

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
              {data.name || ''}
            </TitleOne>
          </BackButton>
        ) : null}

        <ListDetailHeader
          title={data?.name}
          description={description}
          bgImage={ListDetailHeaderBg}
        />
        {renderContent}
      </Section>
    </ComponentError>
  );
};
ListItemDetail.propTypes = {
  description: PropTypes.string,
  ListDetailHeaderBg: PropTypes.string,
  renderContent: PropTypes.node,
  backToLists: PropTypes.func,
  listItemDetails: PropTypes.objectOf(PropTypes.any),
};

ListItemDetail.defaultProps = {
  ListDetailHeaderBg: '',
  renderContent: <div />,
  backToLists: () => {},
  listItemDetails: {},
  description: 'No details available',
};

export default ListItemDetail;
