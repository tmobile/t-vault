/* eslint-disable no-return-assign */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable no-param-reassign */
import React, { useState, useEffect, useCallback, lazy } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Link, Route, Switch, Redirect, useHistory } from 'react-router-dom';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import NoSafesIcon from '../../../../../assets/no-data-safes.svg';
import safeIcon from '../../../../../assets/icon_safes.svg';
import FloatingActionButtonComponent from '../../../../../components/FormFields/FloatingActionButton';
import mediaBreakpoints from '../../../../../breakpoints';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import SafeDetails from '../SafeDetails';
import SelectionTabs from '../Tabs';
import ListItem from '../ListItem';
import PsudoPopper from '../PsudoPopper';
import Error from '../../../../../components/Error';
import {
  makeSafesList,
  createSafeArray,
} from '../../../../../services/helper-function';
import SnackbarComponent from '../../../../../components/Snackbar';

import apiService from '../../apiService';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';

import ConfirmationModal from '../../../../../components/ConfirmationModal';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import EditDeletePopper from '../EditDeletePopper';
import SelectWithCountComponent from '../../../../../components/FormFields/SelectWithCount';
import {
  ListContainer,
  StyledInfiniteScroll,
} from '../../../../../styles/GlobalStyles/listingStyle';

const CreateSafe = lazy(() => import('../../CreateSafe'));

// styled components
const ColumnSection = styled('section')`
  position: relative;
  background: ${(props) => props.backgroundColor || '#151820'};
`;

const RightColumnSection = styled(ColumnSection)`
  width: 59.23%;
  padding: 0;
  ${mediaBreakpoints.small} {
    width: 100%;
    display: ${(props) => (props.clicked ? 'block' : 'none')};
    position: fixed;
    top: 0;
    overflow-y: scroll;
    max-height: 100%;
  }
`;
const LeftColumnSection = styled(ColumnSection)`
  width: 40.77%;
  ${mediaBreakpoints.small} {
    display: ${(props) => (props.clicked ? 'none' : 'block')};
    width: 100%;
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

const NoDataWrapper = styled.div`
  height: 61vh;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #5e627c;
  span {
    margin: 0 0.4rem;
    color: #fff;
    font-weight: bold;
    text-transform: uppercase;
  }
`;

const PopperWrap = styled.div`
  position: absolute;
  top: 50%;
  right: 0%;
  z-index: 1;
  width: 5.5rem;
  transform: translate(-50%, -50%);
  display: none;
`;

const SafeFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  align-items: center;
  text-decoration: none;
  justify-content: space-between;
  padding: 1.2rem 1.8rem 1.2rem 3.8rem;
  background-image: ${(props) =>
    props.active ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active ? '#fff' : '#4a4a4a')};
  ${mediaBreakpoints.belowLarge} {
    padding: 2rem 1.1rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
    color: #fff;
    ${PopperWrap} {
      display: block;
    }
  }
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
  bottom: 3rem;
  right: 2.5rem;
  z-index: 1;
`;

const SearchWrap = styled.div`
  width: 30.9rem;
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

const noDataStyle = css`
  width: 100%;
`;

const EditDeletePopperWrap = styled.div``;

const useStyles = makeStyles(() => ({
  select: {
    backgroundColor: 'transparent',
    fontSize: '1.6rem',
    textTransform: 'uppercase',
    color: '#fff',
    fontWeight: 'bold',
    maxWidth: '23rem',
    marginRight: '2.5rem',
    '& .Mui-selected': {
      color: 'red',
    },
  },
}));
const iconStyles = makeStyles(() => ({
  root: {
    width: '3.4rem',
    height: '3.9rem',
  },
}));
const SafeDashboard = () => {
  const classes = useStyles();
  const [safes, setSafes] = useState({
    users: [],
    apps: [],
    shared: [],
  });
  const [safeList, setSafeList] = useState([]);
  const [moreData] = useState(false);
  const [status, setStatus] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [menu, setMenu] = useState([]);
  const [selectList] = useState([
    { selected: 'User Safes', path: 'users' },
    { selected: 'Shared Safes', path: 'shared' },
    { selected: 'Application Safes', path: 'apps' },
  ]);
  const [safeType, setSafeType] = useState('All Safes');
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [deletionPath, setDeletionPath] = useState('');
  const [toast, setToast] = useState(null);
  const [safeClicked, setSafeClicked] = useState(false);
  const [allSafeList, setAllSafeList] = useState([]);
  const [goodToRoute, setGoodToRoute] = useState(false);
  const [selectedSafeDetails, setSelectedSafeDetails] = useState({});
  const handleClose = () => {
    setOpenConfirmationModal(false);
  };
  const listIconStyles = iconStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const history = useHistory();

  /**
   * @function compareSafesAndList
   * @description function compare safe and manage safes and remove duplication.
   */
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
   * @function fetchData
   * @description function call all the manage and safe api.
   */
  const fetchData = useCallback(async () => {
    setStatus({ status: 'loading', message: 'Loading...' });
    setInputSearchValue('');
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
        setAllSafeList([
          ...safesObject.users,
          ...safesObject.shared,
          ...safesObject.apps,
        ]);
        setGoodToRoute(true);
        setStatus({ status: 'success', message: '' });
      })
      .catch(() => {
        setStatus({ status: 'failed', message: 'failed' });
      });
  }, [compareSafesAndList]);

  /**
   * @description On component load call fetchData function.
   */
  useEffect(() => {
    fetchData().catch(() => {
      setStatus({ status: 'failed', message: 'failed' });
    });
  }, [fetchData]);

  useEffect(() => {
    setMenu([
      { name: 'All Safes', count: allSafeList?.length || 0 },
      { name: 'User Safes', count: safes?.users?.length || 0 },
      { name: 'Shared Safes', count: safes?.shared?.length || 0 },
      { name: 'Application Safes', count: safes?.apps?.length || 0 },
    ]);

    if (safeList && safeList.length) {
      const activeSafeDetail = safeList.filter(
        (item) =>
          item?.name?.toLowerCase() === history.location.pathname.split('/')[2]
      );
      setSelectedSafeDetails(activeSafeDetail[0]);
    }
  }, [allSafeList, safes, history.location.pathname, safeList]);

  /**
   * @function onSearchChange
   * @description function to search safe.
   * @param {string} value searched input value.
   */
  const onSearchChange = (value) => {
    setInputSearchValue(value);
    if (value !== '') {
      const array = allSafeList.filter((item) => {
        return String(item.name).startsWith(value);
      });
      setSafeList([...array]);
    } else {
      setSafeList([...allSafeList]);
    }
  };

  /**
   * @function onSelectChange
   * @description function to filter safe.
   * @param {string} value selected filter value.
   */
  const onSelectChange = (value) => {
    setSafeType(value);
    if (value !== 'All Safes') {
      const obj = selectList?.find((item) => item.selected === value);

      setSafeList([...safes[obj.path]]);
    } else {
      setSafeList([...allSafeList]);
    }
  };

  // when both search and filter value is available.
  useEffect(() => {
    if (safeType !== 'All Safes' && inputSearchValue) {
      const obj = selectList.find((item) => item.selected === safeType);
      const array = allSafeList.filter(
        (item) =>
          item.path.split('/')[0] === obj.path &&
          String(item.name).startsWith(inputSearchValue)
      );
      setSafeList([...array]);
    } else if (safeType === 'All Safes' && inputSearchValue) {
      onSearchChange(inputSearchValue);
    } else if (inputSearchValue === '') {
      onSelectChange(safeType);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inputSearchValue, safeType]);

  const loadMoreData = () => {
    setIsLoading(true);
  };

  /**
   * @function onActionClicked
   * @description function to prevent default click.
   * @param {object} e event
   */
  const onActionClicked = (e) => {
    e.stopPropagation();
    e.preventDefault();
  };

  /**
   * @function onDeleteSafeClicked
   * @description function to call delete confirmation modal for safe.
   * @param {string} path path of the clicked safe.
   */
  const onDeleteSafeClicked = (e, path) => {
    onActionClicked(e);
    setOpenConfirmationModal(true);
    setDeletionPath(path);
  };

  /**
   * @function onDeleteSafeConfirmClicked
   * @description function to delete safe.
   */
  const onDeleteSafeConfirmClicked = () => {
    setStatus({ status: 'loading', message: 'loading' });
    setSafes({ users: [], apps: [], shared: [] });
    setSafeList([]);
    setOpenConfirmationModal(false);
    apiService
      .deleteSafe(deletionPath)
      .then(() => {
        setDeletionPath('');
        setStatus({ status: 'success', message: 'success' });
        setToast(1);
        fetchData();
      })
      .catch(() => {
        setDeletionPath('');
        setToast(-1);
      });
  };

  /**
   * @function onToastClose
   * @description function to close the toast message.
   */
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setToast(null);
  };

  /**
   * @function onLinkClicked
   * @description function to check if mobile screen the make safeClicked true
   * based on that value display left and right side.
   */
  const onLinkClicked = () => {
    if (isMobileScreen) {
      setSafeClicked(true);
    }
  };

  /**
   * @function onResetClicked
   * @description function to check if mobile screen then make safeClicked to false
   * based on that value display left and right side.
   */
  const onResetClicked = () => {
    if (isMobileScreen) {
      setSafeClicked(false);
    }
  };

  /**
   * @function onEditSafeClicked
   * @description function to edit the safe.
   * @param {object} safe safe details.
   */
  const onEditSafeClicked = (safe) => {
    history.push({ pathname: '/safes/edit-safe', state: { safe } });
  };

  let scrollParentRef = null;
  const renderSafes = () => {
    return safeList.map((safe) => {
      return (
        <SafeFolderWrap
          key={safe.name}
          to={{
            pathname: `/safes/${safe.name}`,
            state: { safe },
          }}
          onClick={() => onLinkClicked()}
          active={history.location.pathname === `/safes/${safe.name}`}
        >
          <ListItem
            title={safe.name}
            subTitle={safe.safeType}
            flag={safe.type}
            icon={safeIcon}
            manage={safe.manage}
            listIconStyles={listIconStyles}
          />
          <BorderLine />
          {safe.name && safe.manage && !isMobileScreen ? (
            <PopperWrap onClick={(e) => onActionClicked(e)}>
              <PsudoPopper
                onDeleteSafeClicked={(e) => onDeleteSafeClicked(e, safe.path)}
                safe={safe}
                path="/safes/edit-safe"
              />
            </PopperWrap>
          ) : null}
          {isMobileScreen && safe.manage && (
            <EditDeletePopperWrap onClick={(e) => onActionClicked(e)}>
              <EditDeletePopper
                onDeleteClicked={(e) => onDeleteSafeClicked(e, safe.path)}
                onEditClicked={() => onEditSafeClicked(safe)}
              />
            </EditDeletePopperWrap>
          )}
        </SafeFolderWrap>
      );
    });
  };
  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleClose}
          title="Confirmation"
          description="Are you sure you want to delete this safe?"
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleClose()}
              width="100%"
            />
          }
          confirmButton={
            <ButtonComponent
              label="Confirm"
              color="secondary"
              onClick={() => onDeleteSafeConfirmClicked()}
            />
          }
        />
        <SectionPreview title="safe-section">
          <LeftColumnSection clicked={safeClicked}>
            <ColumnHeader>
              <SelectWithCountComponent
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
            {status.status === 'loading' && (
              <ScaledLoader contentHeight="80%" contentWidth="100%" />
            )}
            {status.status === 'failed' && !safeList?.length && (
              <EmptySecretBox>
                {' '}
                <Error description="Error while fetching safes!" />
              </EmptySecretBox>
            )}
            {safeList && safeList.length > 0 ? (
              <ListContainer ref={(ref) => (scrollParentRef = ref)}>
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
              </ListContainer>
            ) : (
              safeList?.length === 0 &&
              status.status === 'success' && (
                <>
                  {inputSearchValue ? (
                    <NoDataWrapper>
                      No safe found with name
                      <span>{inputSearchValue}</span>
                      {safeType !== 'All Safes' && (
                        <>
                          and filter by
                          <span>{safeType}</span>
                        </>
                      )}
                      {' . '}
                    </NoDataWrapper>
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
                              href="/safes/create-safe"
                              color="secondary"
                              icon="addd"
                              tooltipTitle="Create New Safe"
                              tooltipPos="bottom"
                            />
                          }
                          customStyle={noDataStyle}
                        />
                      </NoSafeWrap>
                    </NoDataWrapper>
                  )}
                </>
              )
            )}
            {safeList?.length ? (
              <FloatBtnWrapper>
                <FloatingActionButtonComponent
                  href="/safes/create-safe"
                  color="secondary"
                  icon="addd"
                  tooltipTitle="Create New Safe"
                  tooltipPos="left"
                />
              </FloatBtnWrapper>
            ) : (
              <></>
            )}
          </LeftColumnSection>

          <RightColumnSection clicked={safeClicked}>
            <Switch>
              {' '}
              {safeList[0]?.name && (
                <Redirect
                  exact
                  from="/safes"
                  to={{
                    pathname: `/safes/${safeList[0]?.name}`,
                    state: { safe: safeList[0] },
                  }}
                />
              )}
              <Route
                path="/safes/:safeName"
                render={(routerProps) => (
                  <SafeDetails
                    resetClicked={() => onResetClicked()}
                    detailData={safeList}
                    params={routerProps}
                    goodToRoute={goodToRoute}
                    refresh={fetchData}
                    renderContent={
                      <SelectionTabs
                        safeDetail={selectedSafeDetails}
                        refresh={fetchData}
                      />
                    }
                  />
                )}
              />
              <Route
                path="/"
                render={(routerProps) => (
                  <SafeDetails
                    detailData={safeList}
                    params={routerProps}
                    resetClicked={() => onResetClicked()}
                  />
                )}
              />
            </Switch>
          </RightColumnSection>
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
        <Switch>
          <Route
            exact
            path="/safes/create-safe"
            render={(routeProps) => (
              <CreateSafe routeProps={{ ...routeProps }} refresh={fetchData} />
            )}
          />
          <Route
            exact
            path="/safes/edit-safe"
            render={(routeProps) => (
              <CreateSafe routeProps={{ ...routeProps }} refresh={fetchData} />
            )}
          />
        </Switch>
      </>
    </ComponentError>
  );
};

export default SafeDashboard;
