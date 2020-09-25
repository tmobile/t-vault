/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable no-nested-ternary */
/* eslint-disable no-param-reassign */
/* eslint-disable no-unused-vars */
/* eslint-disable no-return-assign */
import React, { useState, useEffect, useCallback } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import InfiniteScroll from 'react-infinite-scroller';
import PropTypes from 'prop-types';
import { Link, Route, Switch, Redirect } from 'react-router-dom';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import { values } from 'lodash';
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
import SelectComponent from '../../../../../components/FormFields/SelectFields';
import Error from '../../../../../components/Error';
import {
  makeSafesList,
  createSafeArray,
} from '../../../../../services/helper-function';
import SnackbarComponent from '../../../../../components/Snackbar';

// mock data
// import { safes } from './__mock/safeDashboard';
import apiService from '../../apiService';
import Loader from '../../../../../components/Loader';

import ConfirmationModal from '../../../../../components/ConfirmationModal';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';

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

const SearchWrap = styled.div`
  width: 30.9rem;
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
const SearchBox = styled.div`
  display: flex;
  flex: 1;
`;
const EmptySecretBox = styled('div')`
  width: 100%;
  position: absolute;
  display: flex;
  justify-content: center;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
`;

const useStyles = makeStyles((theme) => ({
  select: {
    backgroundColor: 'transparent',
    fontSize: '1.6rem',
    textTransform: 'uppercase',
    color: '#fff',
    fontWeight: 'bold',
    width: '22rem',
    marginRight: '2.5rem',
    '& .Mui-selected': {
      color: 'red',
    },
  },
}));

const SafeDashboard = (props) => {
  const classes = useStyles();
  const { routeProps } = props;
  const [safes, setSafes] = useState({
    users: [],
    apps: [],
    shared: [],
  });
  const [safeList, setSafeList] = useState([]);
  const [moreData, setMoreData] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [activeSafeFolders, setActiveSafeFolders] = useState([]);
  const [menu] = useState([
    'All Safes',
    'User Safe',
    'Shared Safe',
    'Application Safe',
  ]);
  const [selectList] = useState([
    { selected: 'User Safe', path: 'users' },
    { selected: 'Shared Safe', path: 'shared' },
    { selected: 'Application Safe', path: 'apps' },
  ]);
  const [safeType, setSafeType] = useState('All Safes');
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [deletionPath, setDeletionPath] = useState('');
  const [toast, setToast] = useState(null);
  const handleClose = () => {
    setOpenConfirmationModal(false);
  };
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

  const compareSafesAndList = useCallback((listArray, type, safesObject) => {
    const value = createSafeArray(listArray, type);
    safesObject[type].map((item) => {
      if (!listArray.includes(item.name)) {
        item.manage = false;
      }
      return null;
    });
    value.map((item) => {
      if (!safesObject[type].some((list) => list.name === item.name)) {
        return safesObject[type].push(item);
      }
      return null;
    });
  }, []);

  /**
   * renders safe details page route
   * @param {string}
   * @param {object}
   */

  const fetchData = useCallback(async () => {
    setResponseType(0);
    const safesApiResponse = await apiService.getSafes();
    const usersListApiResponse = await apiService.getManageUsersList();
    const sharedListApiResponse = await apiService.getManageSharedList();
    const appsListApiResponse = await apiService.getManageAppsList();
    const allApiResponse = Promise.all([
      safesApiResponse,
      usersListApiResponse,
      sharedListApiResponse,
      appsListApiResponse,
    ]);
    allApiResponse
      .then((response) => {
        const safesObject = { users: [], apps: [], shared: [] };
        if (response[0] && response[0].data) {
          Object.keys(response[0].data).forEach((item) => {
            const data = makeSafesList(response[0].data[item], item);
            data.map((value) => {
              return safesObject[item].push(value);
            });
          });
        }
        if (response[1] && response[1]?.data?.keys) {
          compareSafesAndList(response[1].data.keys, 'users', safesObject);
        }
        if (response[2] && response[2]?.data?.keys) {
          compareSafesAndList(response[2].data.keys, 'shared', safesObject);
        }
        if (response[3] && response[3]?.data?.keys) {
          compareSafesAndList(response[3].data.keys, 'apps', safesObject);
        }
        setSafes(safesObject);
        setSafeList([
          ...safesObject.users,
          ...safesObject.shared,
          ...safesObject.apps,
        ]);
        setResponseType(1);
      })
      .catch((err) => {
        setResponseType(-1);
      });
  }, [compareSafesAndList]);

  useEffect(() => {
    fetchData().catch((error) => {
      setResponseType(-1);
    });
  }, [fetchData]);

  const onSelectChange = (value) => {
    setSafeType(value);
    if (value !== 'All Safes') {
      const obj = selectList.find((item) => item.selected === value);
      setSafeList([...safes[obj.path]]);
    } else {
      setSafeList([...safes.users, ...safes.shared, ...safes.apps]);
    }
  };

  const onSearchChange = (value) => {
    setInputSearchValue(value);
  };

  const loadMoreData = () => {
    setIsLoading(true);
  };

  const onDeleteSafeClicked = (path) => {
    setOpenConfirmationModal(true);
    setDeletionPath(path);
  };

  const onDeleteSafeConfirmClicked = () => {
    setResponseType(0);
    setSafes({ users: [], apps: [], shared: [] });
    setSafeList([]);
    setOpenConfirmationModal(false);
    apiService
      .deleteSafe(deletionPath)
      .then((res) => {
        setDeletionPath('');
        setResponseType(1);
        setToast(1);
        fetchData();
      })
      // eslint-disable-next-line no-console
      .catch((e) => {
        setDeletionPath('');
        setToast(-1);
      });
  };
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setToast(null);
  };

  let scrollParentRef = null;

  const renderSafes = () => {
    return safeList.map((safe) => (
      <SafeFolderWrap
        key={safe.name}
        to={{
          pathname: `${routeProps.match.url}/${safe.name}`,
          state: { safe },
        }}
        active={
          activeSafeFolders.includes(safe.name) ||
          window.location.pathname.includes(safe.name)
        }
        onMouseLeave={() => setActiveSafeFolders([])}
        onClick={() => showSafeDetails(safe.name, safe)}
        onMouseEnter={() => showSafeDetails(safe.name, safe)}
      >
        <ListItem
          title={safe.name}
          subTitle={safe.date}
          flag={safe.type}
          icon={safeIcon}
          manage={safe.manage}
        />
        <BorderLine />
        {activeSafeFolders.includes(safe.name) && safe.manage ? (
          <PopperWrap>
            <PsudoPopper
              onDeleteSafeClicked={() => onDeleteSafeClicked(safe.path)}
              safe={safe}
            />
          </PopperWrap>
        ) : null}
      </SafeFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleClose}
          title="Are you sure you want to delete this safe?"
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleClose()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
          confirmButton={
            <ButtonComponent
              label="Confirm"
              color="secondary"
              onClick={() => onDeleteSafeConfirmClicked()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
        <SectionPreview title="safe-section">
          <ColumnSection width={isMobileScreen ? '100%' : '52.9rem'}>
            <ColumnHeader>
              <SelectComponent
                menu={menu}
                value={safeType}
                color="secondary"
                classes={classes}
                fullWidth={false}
                onChange={(e) => onSelectChange(e.target.value)}
              />
              <SearchWrap>
                <TextFieldComponent
                  placeholder="Search"
                  icon="search"
                  fullWidth
                  onChange={(e) => onSearchChange(e.target.value)}
                  value={inputSearchValue || ''}
                  color="secondary"
                />
              </SearchWrap>
            </ColumnHeader>
            {responseType === -1 && !safeList?.length && (
              <EmptySecretBox>
                {' '}
                <Error description="Error while fetching safes!" />
              </EmptySecretBox>
            )}
            {responseType === 0 ? (
              <Loader contentHeight="80%" contentWidth="100%" />
            ) : safeList && safeList.length > 0 ? (
              <SafeListContainer ref={(ref) => (scrollParentRef = ref)}>
                <StyledInfiniteScroll
                  pageStart={0}
                  loadMore={() => {
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
              safeList &&
              safeList.length === 0 &&
              responseType !== -1 &&
              responseType !== 0 && (
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
              )
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
                {safeList[0]?.name && (
                  <Redirect
                    exact
                    from="/safe"
                    to={{
                      pathname: `/safe/${safeList[0]?.name}`,
                      state: { safe: safeList[0] },
                    }}
                  />
                )}
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
                <Route
                  path="/"
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
        {toast === -1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message="Something went wrong!"
          />
        )}
        {toast === 1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message="Safe deleted successfully!"
          />
        )}
      </>
    </ComponentError>
  );
};

SafeDashboard.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  routeProps: PropTypes.object,
};
SafeDashboard.defaultProps = {
  routeProps: {},
};

export default SafeDashboard;
