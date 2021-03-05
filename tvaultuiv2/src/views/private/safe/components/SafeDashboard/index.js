/* eslint-disable react/no-array-index-key */
/* eslint-disable no-return-assign */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable no-param-reassign */
import React, { useState, useEffect, useCallback, lazy } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import {
  Link,
  Route,
  Switch,
  Redirect,
  useHistory,
  useLocation,
} from 'react-router-dom';
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
  NoResultFound,
} from '../../../../../styles/GlobalStyles/listingStyle';
import configData from '../../../../../config/config';

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
    overflow-y: auto;
    max-height: 100%;
    z-index: 20;
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
    props.active === 'true' ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active === 'true' ? '#fff' : '#4a4a4a')};
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
  bottom: 1rem;
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
  justify-content: center;
`;

const ScaledLoaderContainer = styled.div`
  height: 5rem;
  display: flex;
  align-items: center;
`;

const scaledLoaderFirstChild = css`
  width: 1.5rem;
  height: 1.5rem;
`;

const scaledLoaderLastChild = css`
  width: 3rem;
  height: 3rem;
  left: -1.7rem;
  top: -0.3rem;
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
  const [safeList, setSafeList] = useState([]);
  const [response, setResponse] = useState({});
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [menu] = useState([
    { name: 'User Safes' },
    { name: 'Shared Safes' },
    { name: 'Application Safes' },
  ]);
  const [safeType, setSafeType] = useState('User Safes');
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [deletionPath, setDeletionPath] = useState('');
  const [toast, setToast] = useState(null);
  const [safeClicked, setSafeClicked] = useState(false);
  const [allSafeList, setAllSafeList] = useState([]);
  const location = useLocation();
  const [selectedSafeDetails, setSelectedSafeDetails] = useState({});
  const handleClose = () => {
    setOpenConfirmationModal(false);
  };
  const listIconStyles = iconStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const isTabAndMobScreen = useMediaQuery(mediaBreakpoints.smallAndMedium);
  const history = useHistory();
  const [isInfiniteScrollLoading, setIsInfiniteScrollLoading] = useState(false);
  const [hasMoreData, setHasMoreData] = useState(true);
  const [safeOffset, setSafeOffset] = useState(0);
  const [clearedData, setClearedData] = useState(false);
  const [arrayList, setArrayList] = useState([]);
  const [limit] = useState(20);
  const isAdmin = JSON.parse(sessionStorage.getItem('isAdmin'));

  /**
   * @function compareSafesAndList
   * @description function compare safe and manage safes and remove duplication.
   */
  const compareSafesAndList = (listArray, type, safesObject) => {
    const value = createSafeArray(listArray, type);
    safesObject[type].map((item) => {
      if (!listArray.includes(item.name)) {
        item.manage = false;
      }
      return null;
    });
    value.map((item) => {
      const obj = arrayList.find((ele) => ele.name === item.name);
      if (!safesObject[type].some((list) => list.name === item.name) && !obj) {
        return safesObject[type].push(item);
      }
      return null;
    });
  };

  const clearData = () => {
    const arr = [];
    setSafeOffset(0);
    setAllSafeList([]);
    setArrayList(arr);
    setHasMoreData(true);
    setClearedData(true);
  };

  const safesOidcResponse = (safeTypeList, type, safeObject) => {
    const listApiCallCount =
      JSON.parse(sessionStorage.getItem('safesApiCount')) || 0;
    const data = makeSafesList(safeTypeList, type, listApiCallCount, isAdmin);
    data.map((value) => {
      const obj = arrayList.find((item) => item.name === value.name);
      if (!obj) {
        return safeObject[type].push(value);
      }
      return null;
    });
  };

  const safesLdapUserPassResponse = (type, safeObject) => {
    const access = JSON.parse(sessionStorage.getItem('access'));
    if (Object.keys(access).length > 0) {
      Object.keys(access).forEach((item) => {
        if (item === type) {
          const listApiCallCount =
            JSON.parse(sessionStorage.getItem('count')) || 0;
          const data = makeSafesList(
            access[item],
            item,
            listApiCallCount,
            isAdmin
          );
          data.map((value) => {
            const obj = arrayList.find((ele) => ele.name === value.name);
            if (!obj) {
              return safeObject[type].push(value);
            }
            return null;
          });
        }
      });
    }
  };

  const checkHasMoreData = (safesArray, listArray) => {
    if (safesArray?.next === '-1' && listArray?.data?.next === -1 && isAdmin) {
      setHasMoreData(false);
    } else if (safesArray?.next === '-1' && !isAdmin) {
      setHasMoreData(false);
    } else {
      setHasMoreData(true);
    }
  };

  const onResponseVariableSet = (safeObject) => {
    Object.keys(safeObject).map((item) => {
      return safeObject[item].map((ele) => {
        return arrayList.push(ele);
      });
    });
    setSafeList([...arrayList]);
    setAllSafeList([...arrayList]);
    setIsInfiniteScrollLoading(false);
    setResponse({ status: 'success', message: '' });
  };

  /**
   * @function fetchUserSafesData
   * @description function call all the manage and users safe api.
   */
  const fetchUserSafesData = useCallback(async () => {
    let safesApiResponse = [];
    let usersListApiResponse = [];
    if (configData.AUTH_TYPE === 'oidc') {
      safesApiResponse = await apiService.getSafes(limit, safeOffset);
    }
    if (isAdmin) {
      usersListApiResponse = await apiService.getManageUsersList(
        limit,
        safeOffset
      );
    } else if (
      !isAdmin &&
      JSON.parse(sessionStorage.getItem('safesApiCount')) === 0
    ) {
      usersListApiResponse = await apiService.getManageUsersList('', '');
    }
    const allApiResponse = Promise.all([
      safesApiResponse,
      usersListApiResponse,
    ]);
    allApiResponse
      .then(async (result) => {
        setSafeOffset(limit + safeOffset);
        const safesObject = { users: [] };
        if (configData.AUTH_TYPE === 'oidc') {
          if (result && result[0]?.data) {
            safesOidcResponse(result[0].data.users, 'users', safesObject);
          }
        } else {
          safesLdapUserPassResponse('users', safesObject);
        }
        if (result && result[1]?.data?.keys) {
          compareSafesAndList(result[1].data.keys, 'users', safesObject);
        }
        checkHasMoreData(result[0]?.data?.userSafeCount[0], result[1]);
        onResponseVariableSet(safesObject);
      })
      .catch(() => {
        setResponse({ status: 'failed', message: 'failed' });
      });
    // eslint-disable-next-line
  }, [safeOffset, arrayList]);

  /**
   * @description On component load call fetchUserSafesData function.
   */
  useEffect(() => {
    if (!isAdmin) {
      sessionStorage.setItem('safesApiCount', 0);
    }
    setResponse({ status: 'loading', message: 'Loading...' });
    setInputSearchValue('');
    setSafeType('User Safes');
    fetchUserSafesData().catch(() => {
      setResponse({ status: 'failed', message: 'failed' });
    });
    // eslint-disable-next-line
  }, []);

  /**
   * @function fetchData
   * @description function call all the manage and shared safe api.
   */
  const fetchSharedSafesData = useCallback(async () => {
    let safesApiResponse = [];
    let sharedListApiResponse = [];
    if (configData.AUTH_TYPE === 'oidc') {
      safesApiResponse = await apiService.getSafes(limit, safeOffset);
    }
    if (isAdmin) {
      sharedListApiResponse = await apiService.getManageSharedList(
        limit,
        safeOffset
      );
    } else if (
      !isAdmin &&
      JSON.parse(sessionStorage.getItem('safesApiCount')) === 0
    ) {
      sharedListApiResponse = await apiService.getManageSharedList('', '');
    }
    const allApiResponse = Promise.all([
      safesApiResponse,
      sharedListApiResponse,
    ]);
    allApiResponse
      .then((result) => {
        setSafeOffset(limit + safeOffset);
        const safesObject = { shared: [] };
        if (configData.AUTH_TYPE === 'oidc') {
          if (result[0] && result[0].data) {
            safesOidcResponse(result[0].data.shared, 'shared', safesObject);
          }
        } else {
          safesLdapUserPassResponse('shared', safesObject);
        }
        if (result[1] && result[1]?.data?.keys) {
          compareSafesAndList(result[1].data.keys, 'shared', safesObject);
        }
        checkHasMoreData(result[0]?.data?.sharedSafeCount[0], result[1]);
        onResponseVariableSet(safesObject);
      })
      .catch(() => {
        setResponse({ status: 'failed', message: 'failed' });
      });
    // eslint-disable-next-line
  }, [safeOffset, arrayList]);

  /**
   * @function fetchAppSafesData
   * @description function call all the manage and apps safe api.
   */
  const fetchAppSafesData = useCallback(async () => {
    let safesApiResponse = [];
    let appsListApiResponse = [];
    if (configData.AUTH_TYPE === 'oidc') {
      safesApiResponse = await apiService.getSafes(limit, safeOffset);
    }
    if (isAdmin) {
      appsListApiResponse = await apiService.getManageAppsList(
        limit,
        safeOffset
      );
    } else if (
      !isAdmin &&
      JSON.parse(sessionStorage.getItem('safesApiCount')) === 0
    ) {
      appsListApiResponse = await apiService.getManageAppsList('', '');
    }
    const allApiResponse = Promise.all([safesApiResponse, appsListApiResponse]);
    allApiResponse
      .then((result) => {
        setSafeOffset(limit + safeOffset);
        const safesObject = { apps: [] };
        if (configData.AUTH_TYPE === 'oidc') {
          if (result && result[0]?.data) {
            safesOidcResponse(result[0].data.apps, 'apps', safesObject);
          }
        } else {
          safesLdapUserPassResponse('apps', safesObject);
        }
        if (result[1] && result[1]?.data?.keys) {
          compareSafesAndList(result[1].data.keys, 'apps', safesObject);
        }
        checkHasMoreData(result[0]?.data?.appSafeCount[0], result[1]);
        onResponseVariableSet(safesObject);
      })
      .catch(() => {
        setResponse({ status: 'failed', message: 'failed' });
      });
    // eslint-disable-next-line
  }, [arrayList, safeOffset]);

  useEffect(() => {
    if (allSafeList.length > 0) {
      const val = location.pathname.split('/');
      const safeName = val[val.length - 1];
      if (safeName !== 'create-safe' && safeName !== 'edit-safe') {
        const obj = allSafeList.find((safe) => safe.name === safeName);
        if (obj) {
          setSelectedSafeDetails({ ...obj });
        } else {
          setSelectedSafeDetails(allSafeList[0]);
          history.push(`/safes/${allSafeList[0].name}`);
        }
      }
    } else {
      setSelectedSafeDetails({});
    }
    // eslint-disable-next-line
  }, [allSafeList, location, history]);

  /**
   * @function onSearchChange
   * @description function to search safe.
   * @param {string} value searched input value.
   */
  const onSearchChange = (value) => {
    setInputSearchValue(value);
    if (value.length > 2) {
      const array = allSafeList.filter((item) =>
        item?.name?.toLowerCase().includes(value?.toLowerCase().trim())
      );
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
    setResponse({ status: 'loading', message: 'Loading...' });
    clearData();
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

  const callApiBasedOnSafeType = async () => {
    if (safeType === 'User Safes') {
      await fetchUserSafesData();
    } else if (safeType === 'Shared Safes') {
      await fetchSharedSafesData();
    } else {
      await fetchAppSafesData();
    }
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
    setResponse({ status: 'loading', message: 'loading' });
    setSafeList([]);
    setOpenConfirmationModal(false);
    apiService
      .deleteSafe(deletionPath)
      .then(() => {
        setDeletionPath('');
        setToast(1);
        clearData();
      })
      .catch(() => {
        setResponse({ status: 'success', message: 'success' });
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

  const loadMoreData = () => {
    setIsInfiniteScrollLoading(true);
    callApiBasedOnSafeType();
  };

  const handleListScroll = () => {
    const element = document.getElementById('scrollList');
    if (
      element.scrollHeight - element.offsetHeight - 250 < element.scrollTop &&
      hasMoreData &&
      !isInfiniteScrollLoading
    ) {
      if (!isAdmin) {
        sessionStorage.setItem('safesApiCount', 1);
      }
      loadMoreData();
    }
  };

  useEffect(() => {
    if (clearedData) {
      callApiBasedOnSafeType();
      setClearedData(false);
      if (!isAdmin) {
        sessionStorage.setItem('safesApiCount', 0);
      }
    }
    // eslint-disable-next-line
  }, [clearedData]);

  const fetchData = () => {
    setResponse({ status: 'loading', message: 'loading' });
    clearData();
  };

  const renderSafes = () => {
    return safeList.map((safe, index) => {
      return (
        <SafeFolderWrap
          key={index}
          to={{
            pathname: `/safes/${safe.name}`,
            state: { safe },
          }}
          onClick={() => onLinkClicked()}
          active={
            history.location.pathname === `/safes/${safe.name}`
              ? 'true'
              : 'false'
          }
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
          {(isAdmin || safe.manage) && !isTabAndMobScreen ? (
            <PopperWrap onClick={(e) => onActionClicked(e)}>
              <PsudoPopper
                onDeleteSafeClicked={(e) => onDeleteSafeClicked(e, safe.path)}
                safe={safe}
                path="/safes/edit-safe"
              />
            </PopperWrap>
          ) : null}
          {isTabAndMobScreen && (safe.manage || isAdmin) && (
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
          title="Delete Safe"
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
        <SectionPreview>
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
                  placeholder="Search - Enter min 3 characters"
                  icon="search"
                  fullWidth
                  onChange={(e) => onSearchChange(e.target.value)}
                  value={inputSearchValue || ''}
                  color="secondary"
                  characterLimit={40}
                />
              </SearchWrap>
            </ColumnHeader>
            {response.status === 'loading' && (
              <ScaledLoader contentHeight="80%" contentWidth="100%" />
            )}
            {response.status === 'failed' && (
              <EmptySecretBox>
                <Error description="Error while fetching safes!" />
              </EmptySecretBox>
            )}
            {response.status === 'success' && (
              <>
                {safeList.length > 0 ? (
                  <ListContainer>
                    <div
                      onScroll={() => handleListScroll()}
                      id="scrollList"
                      style={{
                        height: '100%',
                        overflow: 'auto',
                        width: '100%',
                      }}
                    >
                      {renderSafes()}

                      {isInfiniteScrollLoading && (
                        <ScaledLoaderContainer>
                          <ScaledLoader
                            contentHeight="80%"
                            contentWidth="100%"
                            notAbsolute
                            scaledLoaderLastChild={scaledLoaderLastChild}
                            scaledLoaderFirstChild={scaledLoaderFirstChild}
                          />
                        </ScaledLoaderContainer>
                      )}
                    </div>
                  </ListContainer>
                ) : (
                  <>
                    {inputSearchValue ? (
                      <NoResultFound>
                        No safe found with name
                        <div>{inputSearchValue}</div>
                      </NoResultFound>
                    ) : (
                      <NoDataWrapper>
                        <NoSafeWrap>
                          <NoData
                            imageSrc={NoSafesIcon}
                            description="Create a safe to get started!"
                            actionButton={
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
                )}
              </>
            )}
            {safeList?.length > 0 && (
              <FloatBtnWrapper>
                <FloatingActionButtonComponent
                  href="/safes/create-safe"
                  color="secondary"
                  icon="addd"
                  tooltipTitle="Create New Safe"
                  tooltipPos="left"
                />
              </FloatBtnWrapper>
            )}
          </LeftColumnSection>

          <RightColumnSection clicked={safeClicked}>
            <Switch>
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
                    detailData={selectedSafeDetails}
                    params={routerProps}
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
                    detailData={selectedSafeDetails}
                    params={routerProps}
                    resetClicked={() => onResetClicked()}
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
