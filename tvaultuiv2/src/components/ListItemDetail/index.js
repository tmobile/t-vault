import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import ListDetailHeader from '../ListDetailHeader';

// styled components goes here
const Section = styled('section')`
  flex-direction: column;
  display: flex;
  z-index: 2;
  width: 100%;
  height: 100%;
`;

const ListItemDetail = (props) => {
  const {
    ListDetailHeaderBg,
    renderContent,
    backToLists,
    description,
    listItemDetails,
  } = props;

  const [data, setData] = useState({});
  // screen view handler

  useEffect(() => {
    if (listItemDetails && Object.keys(listItemDetails).length > 0) {
      setData({ ...listItemDetails });
    } else {
      setData({});
    }
  }, [listItemDetails]);

  // route component data
  const goBackToList = () => {
    backToLists();
  };

  return (
    <ComponentError>
      <Section>
        <ListDetailHeader
          title={data?.name || '...'}
          description={description}
          bgImage={ListDetailHeaderBg}
          goBackToList={goBackToList}
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
