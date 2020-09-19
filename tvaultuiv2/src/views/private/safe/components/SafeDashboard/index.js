/* eslint-disable no-unused-vars */
/* eslint-disable no-return-assign */
import React, { useState, useEffect } from 'react';
import InfiniteScroll from 'react-infinite-scroller';
import PropTypes from 'prop-types';
import { Link, Route, Switch } from 'react-router-dom';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import SelectDropDown from '../../../../../components/SelectDropDown';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import NoSafesIcon from '../../../../../assets/no-data-safes.svg';
import safeIcon from '../../../../../assets/icon_safes.svg';
import FloatingActionButtonComponent from '../../../../../components/FormFields/FloatingActionButton';
import mediaBreakpoints from '../../../../../breakpoints';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import SafeDetails from '../SafeDetails';
import ListItem from '../ListItem';
import PsudoPopper from '../PsudoPopper';
import {
  makeSafesList,
  createArrayOfObject,
  removeDuplicate,
} from '../../../../../services/helper-function';

// mock data
// import { safes } from './__mock/safeDashboard';
import apiService from '../../apiService';

// styled components
const ColumnSection = styled('section')`
  position: relative;
  width: ${(props) => props.width || '50%'};
  padding: ${(props) => props.padding || '0'};
  background: ${(props) => props.backgroundColor || '#151820'};
  ${mediaBreakpoints.small} {
    ${(props) => (props.mobileScreenCss ? props.mobileScreenCss : '')}
  }
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
  ${mediaBreakpoints.small} {
    max-height: 78vh;
  }
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
  z-index: 1;
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

const MobileViewForSafeDetailsPage = css`
  position: fixed;
  right: 0;
  left: 0;
  bottom: 0;
  top: 0;
  z-index: 1;
  overflow-y: auto;
`;

const SafeDashboard = (props) => {
  const { routeProps } = props;
  const [safes, setSafes] = useState({
    users: [],
    apps: [],
    shared: [],
  });
  const [safeList, setSafeList] = useState([]);
  const [moreData, setMoreData] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [usersSafeList, setUsersSafeList] = useState([]);
  const [appsSafeList, setAppsSafeList] = useState([]);
  const [sharedSafeList, setSharedSafeList] = useState([]);

  const [activeSafeFolders, setActiveSafeFolders] = useState([]);
  // const [showPopper, setShowPopper] = useState(false);
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  /**
   * safe detail page route change handling function
   * @param {string}
   * @param {object}
   */
  const showSafeDetails = (active) => {
    const activeSafes = [];
    activeSafes.push(active);
    // setResponseType(0);
    setActiveSafeFolders([...activeSafes]);
  };

  /**
   * renders safe details page route
   * @param {string}
   * @param {object}
   */

  useEffect(() => {
    Promise.all([
      apiService.getApiCall(`/ss/sdb/safes`),
      apiService.getApiCall(`/ss/sdb/list?path=users`),
      apiService.getApiCall(`/ss/sdb/list?path=shared`),
      apiService.getApiCall(`/ss/sdb/list?path=apps`),
    ])
      .then((responses) => {
        // Get a JSON object from each of the responses
        return Promise.all(
          responses.map((response) => {
            return response;
          })
        );
      })
      .then((safeData) => {
        // take safes api data
        if (safeData[0] && safeData[0].data) {
          if (safeData[0].data.users?.length > 0) {
            const data = makeSafesList(safeData[0].data.users, 'users');
            data.map((item) => {
              return safes.users.push(item);
            });
          }
          if (safeData[0].data.shared?.length > 0) {
            const data = makeSafesList(safeData[0].data.shared, 'shared');
            data.map((item) => {
              return safes.shared.push(item);
            });
          }
          if (safeData[0].data.apps?.length > 0) {
            const data = makeSafesList(safeData[0].data.apps, 'apps');
            data.map((item) => {
              return safes.apps.push(item);
            });
          }
        }

        // Take list api response and update safe hook data
        if (safeData[1] && safeData[1].data?.keys?.length > 0) {
          const value = createArrayOfObject(safeData[1].data.keys, 'users');
          value.map((item) => {
            if (!safes.users.some((list) => list.name === item.name)) {
              return safes.users.push(item);
            }
          });
        }
        if (safeData[2] && safeData[2].data?.keys?.length > 0) {
          const value = createArrayOfObject(safeData[2].data.keys, 'shared');
          value.map((item) => {
            if (!safes.shared.some((list) => list.name === item.name)) {
              return safes.shared.push(item);
            }
          });
        }
        if (safeData[3] && safeData[3].data?.keys?.length > 0) {
          const value = createArrayOfObject(safeData[3].data.keys, 'apps');
          value.map((item) => {
            if (!safes.apps.some((list) => list.name === item.name)) {
              return safes.apps.push(item);
            }
          });
        }
        // set safe hook
        setSafes(safes);
        setSafeList([...safes.shared, ...safes.users, ...safes.apps]);
      })
      .catch((error) => {
        // if there's an error, log it
        console.log(error);
      });
  }, [safes]);

  //   useEffect(() => {
  //      const filteredArr = arr.reduce((acc, current) => {
  //   const x = acc.find(item => item.id === current.id);
  //   if (!x) {
  //     return acc.concat([current]);
  //   } else {
  //     return acc;
  //   }
  // }, []);
  //   }, [safeList.length> 0]);

  const handleChange = (e) => {
    setInputSearchValue(e.target.value);
  };

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
    setIsLoading(true);
    // getSafesList().then((res) => {
    //   setMoreData(false);
    //   setSafeList((prev) => [...prev, res]);
    // });
  };

  let scrollParentRef = null;

  const renderSafes = () => {
    return safeList.map((safe) => (
      <SafeFolderWrap
        key={safe.name}
        to={`${routeProps.match.url}/${safe.name}`}
        active={activeSafeFolders.includes(safe.name)}
        onClick={() => showSafeDetails(safe.name)}
      >
        <ListItem
          title={safe.name}
          subTitle={safe.date}
          flag={safe.type}
          icon={safeIcon}
        />
        <BorderLine />
        {activeSafeFolders.includes(safe.name) ? (
          <PopperWrap>
            <PsudoPopper />
          </PopperWrap>
        ) : null}
      </SafeFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        <SectionPreview title="safe-section">
          <ColumnSection width={isMobileScreen ? '100%' : '52.9rem'}>
            <ColumnHeader>
              <SelectDropDown />
              <TextFieldComponent
                placeholder="Search"
                icon="search"
                onChange={(e) => handleChange(e)}
                value={inputSearchValue || ''}
                color="secondary"
              />
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
                  loader={!isLoading ? <div key={0}>Loading...</div> : <></>}
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
            {safeList?.length ? (
              <FloatBtnWrapper>
                <FloatingActionButtonComponent
                  href="/safe/create-safe"
                  color="secondary"
                  icon="addd"
                  tooltipTitle="Create New Safe"
                  tooltipPos="left"
                />
              </FloatBtnWrapper>
            ) : (
              <></>
            )}
          </ColumnSection>

          {!isMobileScreen || activeSafeFolders?.length ? (
            <ColumnSection
              backgroundColor="linear-gradient(to bottom, #151820, #2c3040)"
              padding="0"
              width={isMobileScreen ? '100%' : '77.1rem'}
              mobileScreenCss={MobileViewForSafeDetailsPage}
            >
              <Switch>
                {' '}
                <Route
                  path="/:tab/:safeName"
                  render={(routerProps) => (
                    <SafeDetails
                      detailData={safes}
                      params={routerProps}
                      setActiveSafeFolders={() => setActiveSafeFolders([])}
                    />
                  )}
                />
              </Switch>
            </ColumnSection>
          ) : (
            <></>
          )}
        </SectionPreview>
      </>
    </ComponentError>
  );
};

SafeDashboard.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  routeProps: PropTypes.object,
  // eslint-disable-next-line react/forbid-prop-types
  // safes: PropTypes.array,
};
SafeDashboard.defaultProps = {
  routeProps: {},
  // safes: [],
};

export default SafeDashboard;
