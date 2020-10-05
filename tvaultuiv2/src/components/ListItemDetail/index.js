import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useHistory, useLocation } from 'react-router-dom';
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
    description,
  } = props;
  const location = useLocation();
  const history = useHistory();
  const [data, setData] = useState({});
  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  useEffect(() => {
    if (!location?.state?.data) {
      if (listItemDetails && listItemDetails.length) {
        const activeSafeDetail = listItemDetails.filter(
          (item) =>
            item?.name?.toLowerCase() ===
            history.location.pathname.split('/')[2]
        );
        setData(activeSafeDetail);
      }
      return;
    }
    setData(location?.state?.data);
  }, [location.state, listItemDetails, history.location.pathname]);

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
  listItemDetails: PropTypes.arrayOf(PropTypes.any),
  backToLists: PropTypes.func,
};
ListItemDetail.defaultProps = {
  ListDetailHeaderBg: '',
  renderContent: <div />,
  listItemDetails: {},
  backToLists: () => {},
  description: 'No details available',
};

export default ListItemDetail;
