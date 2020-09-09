/* eslint-disable no-return-assign */
/* eslint-disable import/no-unresolved */
import React, { useState, useEffect } from 'react';
import InfiniteScroll from 'react-infinite-scroller';
import PropTypes from 'prop-types';
import { Link, Route, Switch } from 'react-router-dom';
import styled from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import NoData from 'components/NoData';
import NoSafesIcon from 'assets/no-data-safes.svg';
import safeIcon from 'assets/icon_safes.svg';
import SelectDropDown from 'components/SelectDropDown';
import FloatingActionButtonComponent from 'components/FormFields/FloatingActionButton';
import SafeDetails from '../SafeDetails';
import ListItem from '../ListItem';
import PsudoPopper from '../PsudoPopper';

// mock data
import { safeDetail } from './__mock/safeDashboard';

// styled components
const ColumnSection = styled('section')`
  position: relative;
  width: ${(props) => props.width || '50%'};
  padding: ${(props) => props.padding || '0'};
  background: ${(props) => props.backgroundColor || '#151820'};
`;
const SectionPreview = styled('main')`
  display: flex;
  height: 100%;
`;
const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  padding: 0.5em;
  justify-content: space-between;
  border-bottom: 0.1rem solid #1d212c;
`;
const StyledInfiniteScroll = styled(InfiniteScroll)`
  width: 100%;
  max-height: 61vh;
`;

const SafeListContainer = styled.div`
  overflow: auto;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const NoDataWrapper = styled.div`
  height: 61vh;
  display: flex;
  justify-content: center;
  align-items: center;
`;
const SafeFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  text-decoration: none;
  align-items: center;
  flex-direction: column;
  padding: 1.2rem 1.8rem 1.2rem 3.4rem;
  cursor: pointer;
  background-image: ${(props) =>
    props.active ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active ? '#fff' : '#4a4a4a')};
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
    color: #fff;
  }
`;
const PopperWrap = styled.div`
  position: absolute;
  top: 50%;
  right: 0%;
  z-index: 2;
  width: 5.5rem;
  transform: translate(-50%, -50%);
`;

const NoSafeWrap = styled.div`
  width: 35%;
`;

const BorderLine = styled.div`
  border-bottom: 0.1rem solid #1d212c;
  width: 90%;
  position: absolute;
  bottom: 0;
`;
const FloatBtnWrapper = styled('div')`
  position: absolute;
  bottom: 2.8rem;
  right: 2.5rem;
`;
const SafeDashboard = (props) => {
  const { routeProps, safes } = props;
  const [safeList, setSafeList] = useState([]);
  const [moreData, setMoreData] = useState(false);

  const [activeSafeFolders, setActiveSafeFolders] = useState([]);
  // const [showPopper, setShowPopper] = useState(false);

  /**
   * safe detail page route change handling function
   * @param {string}
   * @param {object}
   */
  const showSafeDetails = (active) => {
    const activeSafes = [];
    activeSafes.push(active);
    setActiveSafeFolders([...activeSafes]);
  };

  /**
   * renders safe details page route
   * @param {string}
   * @param {object}
   */

  useEffect(() => {
    console.log('routeProps', routeProps);
    console.log('safeList', safes);
    safes.map((item) => {
      return setSafeList((prev) => [...prev, item]);
    });
    setMoreData(true);
  }, []);

  // const getSafesList = () => {
  //   return new Promise((resolve) =>
  //     setTimeout(() => {
  //       resolve({
  //         name: `safe-${Math.ceil(Math.random() * 100)}`,
  //         desc:
  //           'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
  //         date: '2 days ago , 9:20 pm',
  //         flagType: 'new',
  //       });
  //     }, 1000)
  //   );
  // };

  const loadMoreData = () => {
    // getSafesList().then((res) => {
    //   setMoreData(false);
    //   setSafeList((prev) => [...prev, res]);
    // });
  };

  let scrollParentRef = null;
  const renderSafes = () => {
    return safeList.map((safe) => (
      <SafeFolderWrap
        key={safe.safeName}
        to={`${routeProps.match.url}/${safe.safeName}`}
        active={activeSafeFolders.includes(safe.safeName)}
        onClick={() => showSafeDetails(safe.safeName)}
      >
        <ListItem
          title={safe.safeName}
          subTitle={safe.date}
          flag={safe.type}
          icon={safeIcon}
        />
        <BorderLine />
        {activeSafeFolders.includes(safe.safeName) ? (
          <PopperWrap>
            <PsudoPopper />
          </PopperWrap>
        ) : null}
      </SafeFolderWrap>
    ));
  };

  return (
    <ComponentError>
      <SectionPreview title="safe-section">
        <ColumnSection width="52.9rem">
          <ColumnHeader>
            <SelectDropDown />
          </ColumnHeader>

          {safeList && safeList.length ? (
            <SafeListContainer ref={(ref) => (scrollParentRef = ref)}>
              <StyledInfiniteScroll
                pageStart={0}
                loadMore={() => {
                  console.log('Load more data called---');
                  loadMoreData();
                }}
                hasMore={moreData}
                threshold={100}
                loader={<div key={0}>Loading...</div>}
                useWindow={false}
                getScrollParent={() => scrollParentRef}
              >
                {renderSafes()}
              </StyledInfiniteScroll>
            </SafeListContainer>
          ) : (
            <NoDataWrapper>
              {' '}
              <NoSafeWrap>
                <NoData
                  imageSrc={NoSafesIcon}
                  description="Create a Safe to get started!"
                  actionButton={
                    // eslint-disable-next-line react/jsx-wrap-multilines
                    <FloatingActionButtonComponent
                      href="/safe/create-safe"
                      color="secondary"
                      icon="addd"
                      tooltipTitle="Create New Safe"
                      tooltipPos="bottom"
                    />
                  }
                />
              </NoSafeWrap>
            </NoDataWrapper>
          )}
          <FloatBtnWrapper>
            <FloatingActionButtonComponent
              href="/safe/create-safe"
              color="secondary"
              icon="addd"
              tooltipTitle="Create New Safe"
              tooltipPos="left"
            />
          </FloatBtnWrapper>
        </ColumnSection>

        <ColumnSection
          backgroundColor="linear-gradient(to top, #151820, #2c3040)"
          padding="0"
          width="77.1rem"
        >
          <Switch>
            {' '}
            <Route
              path="/:tab/:safeName"
              render={(routerProps) => (
                <SafeDetails detailData={safes} params={routerProps} />
              )}
            />
          </Switch>
        </ColumnSection>
      </SectionPreview>
    </ComponentError>
  );
};

SafeDashboard.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  routeProps: PropTypes.object,
  // eslint-disable-next-line react/forbid-prop-types
  safes: PropTypes.array,
};
SafeDashboard.defaultProps = {
  routeProps: {},
  safes: [],
};

export default SafeDashboard;
