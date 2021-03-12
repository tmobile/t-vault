/* eslint-disable react/no-array-index-key */
/* eslint-disable no-return-assign */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable no-param-reassign */
import React, { useState, useEffect, useCallback, lazy } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import {
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
import FloatingActionButtonComponent from '../../../../../components/FormFields/FloatingActionButton';
import mediaBreakpoints from '../../../../../breakpoints';
import SafeDetails from '../SafeDetails';
import SelectionTabs from '../Tabs';
import Error from '../../../../../components/Error';
import {
  constructSafeType,
  createSafeArray,
} from '../../../../../services/helper-function';
import SnackbarComponent from '../../../../../components/Snackbar';
import apiService from '../../apiService';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import SelectWithCountComponent from '../../../../../components/FormFields/SelectWithCount';
import { ListContainer } from '../../../../../styles/GlobalStyles/listingStyle';
import configData from '../../../../../config/config';
import SearchboxWithDropdown from '../../../../../components/FormFields/SearchboxWithDropdown';
import LeftColumn from './component/leftColumn';

const CreateSafe = lazy(() => import('../../CreateSafe'));

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

const NoSafeWrap = styled.div`
  width: 35%;
`;

const FloatBtnWrapper = styled('div')`
  position: absolute;
  bottom: 1rem;
  right: 2.5rem;
  z-index: 1;
`;

const SearchWrap = styled.div`
  width: 100%;
  border: 0.5px solid transparent;
  outline: none;
  :focus-within {
    border: 0.5px solid #e20074;
  }
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

const customStyle = css`
  position: absolute;
  right: 1.2rem;
  top: 1.6rem;
  color: red;
`;

const useStyles = makeStyles(() => ({
  select: {
    backgroundColor: 'transparent',
    fontSize: '1.6rem',
    textTransform: 'uppercase',
    color: '#fff',
    fontWeight: 'bold',
    maxWidth: '15rem',
    marginRight: '2.5rem',
    '& .Mui-selected': {
      color: 'red',
    },
  },
}));

const SafeDashboard = () => {
  const classes = useStyles();
  const [safeList, setSafeList] = useState([]);
  const [allSafeList, setAllSafeList] = useState([]);
  const [response, setResponse] = useState({});
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [menu] = useState([
    { name: 'User', type: 'users' },
    { name: 'Shared', type: 'shared' },
    { name: 'Application', type: 'apps' },
  ]);
  const [safeType, setSafeType] = useState('User');
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [deletionPath, setDeletionPath] = useState('');
  const [toast, setToast] = useState(null);
  const [safeClicked, setSafeClicked] = useState(false);
  const location = useLocation();
  const [selectedSafeDetails, setSelectedSafeDetails] = useState({});
  const handleClose = () => {
    setOpenConfirmationModal(false);
  };
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const history = useHistory();
  const [isInfiniteScrollLoading, setIsInfiniteScrollLoading] = useState(false);
  const [hasMoreData, setHasMoreData] = useState(true);
  const [safeOffset, setSafeOffset] = useState(0);
  const [clearedData, setClearedData] = useState(false);
  const [searchLoader, setSearchLoader] = useState(false);
  const [limit] = useState(20);
  const isAdmin = JSON.parse(sessionStorage.getItem('isAdmin'));
  const [searchMenu, setSearchMenu] = useState([]);
  const [noResultFound, setNoResultFound] = useState('');
  const [ownerOfSafes, setOwnerOfSafes] = useState(false);
  const [searchSelectClicked, setSearchSelectClicked] = useState(false);
  const [dataNotAvailableToScroll, setDataNotAvailableToScroll] = useState(
    false
  );
  const [searchList, setSearchList] = useState([]);
  const constructSafesArray = (type) => {
    const data = JSON.parse(sessionStorage.getItem('safesData'));
    const array = [];
    Object.keys(data).map((ele) => {
      if (ele === type) {
        data[ele].map((item) => {
          return array.push({
            name: Object.keys(item)[0],
            access: Object.values(item)[0],
          });
        });
      }
      return null;
    });
    return array;
  };

  /**
   * @function compareSafesAndList
   * @description function compare safe and manage safes and remove duplication.
   */
  const compareSafesAndList = (listArray, type) => {
    const arrayVal = constructSafesArray(type);
    const value = createSafeArray(listArray, type, arrayVal);
    return value;
  };

  const clearData = () => {
    setSafeOffset(0);
    setSafeList([]);
    setAllSafeList([]);
    setHasMoreData(true);
    setClearedData(true);
    setSearchSelectClicked(false);
    setNoResultFound('');
    setDataNotAvailableToScroll(false);
    sessionStorage.removeItem('safesList');
    setSearchSelectClicked(false);
  };

  const safesLdapUserPassResponse = () => {
    const access = JSON.parse(sessionStorage.getItem('access'));
    sessionStorage.setItem('safesData', JSON.stringify(access));
  };

  const checkHasMoreData = (listArray) => {
    if (listArray?.data?.next === -1) {
      setHasMoreData(false);
      setDataNotAvailableToScroll(true);
    } else {
      setHasMoreData(true);
    }
  };

  useEffect(() => {
    if (dataNotAvailableToScroll && !isAdmin) {
      const selectedTypeObj = menu.find((item) => item.name === safeType);
      const array = constructSafesArray(selectedTypeObj?.type);
      const arr1 = [];
      const safesListArray = JSON.parse(sessionStorage.getItem('safesList'));
      array.map((ele) => {
        const notAvailableVal = safesListArray?.find(
          (item) => item.name === ele.name
        );
        if (!notAvailableVal) {
          const data = {
            name: ele.name,
            access: ele.access,
            path: `${selectedTypeObj.type}/${ele.name}`,
            safeType: constructSafeType(selectedTypeObj.type),
            manage: false,
          };
          arr1.push(data);
        }
        return null;
      });
      setSafeList((val) => val.concat(arr1));
      sessionStorage.setItem(
        'safesList',
        JSON.stringify(safesListArray?.concat(arr1))
      );
    }
    // eslint-disable-next-line
  }, [dataNotAvailableToScroll]);

  const onResponseVariableSet = (safeArr) => {
    setIsInfiniteScrollLoading(false);
    setResponse({ status: 'success', message: '' });
    setSafeList((val) => val.concat(safeArr));
    if (!isAdmin) {
      const data = JSON.parse(sessionStorage.getItem('safesList'));
      if (data === null) {
        sessionStorage.setItem('safesList', JSON.stringify(safeArr));
      } else {
        sessionStorage.setItem(
          'safesList',
          JSON.stringify(data.concat(safeArr))
        );
      }
    }
  };

  /**
   * @function fetchUserSafesData
   * @description function call all the manage and users safe api.
   */
  const fetchUserSafesData = useCallback(async () => {
    let safesApiResponse = [];
    if (
      configData.AUTH_TYPE === 'oidc' &&
      JSON.parse(sessionStorage.getItem('safesApiCount')) === 0
    ) {
      safesApiResponse = await apiService.getSafes();
    }
    const usersListApiResponse = await apiService.getManageUsersList(
      limit,
      safeOffset
    );
    const allApiResponse = Promise.all([
      safesApiResponse,
      usersListApiResponse,
    ]);
    allApiResponse
      .then(async (result) => {
        setSafeOffset(limit + safeOffset);
        let safesObject = [];
        if (configData.AUTH_TYPE === 'oidc') {
          if (result && result[0]?.data) {
            sessionStorage.setItem('safesData', JSON.stringify(result[0].data));
          }
        } else {
          safesLdapUserPassResponse();
        }
        if (result && result[1]?.data?.keys) {
          safesObject = compareSafesAndList(result[1].data.keys, 'users');
        }

        onResponseVariableSet(safesObject);
        checkHasMoreData(result[1]);
      })
      .catch(() => {
        setResponse({ status: 'failed', message: 'failed' });
      });
    // eslint-disable-next-line
  }, [safeOffset, safeList]);

  const callSearchApi = () => {
    apiService
      .searchSafes()
      .then((responses) => {
        const array = [];
        Object.keys(responses.data).map((item) => {
          responses.data[item].map((ele) => {
            array.push({
              name: ele,
              type: constructSafeType(item),
              path: item,
            });
            return null;
          });
          return null;
        });
        setSearchList([...array]);
      })
      .catch(() => {});
  };

  /**
   * @description On component load call fetchUserSafesData function.
   */
  useEffect(() => {
    sessionStorage.setItem('safesApiCount', 0);
    sessionStorage.removeItem('safesList');
    setResponse({ status: 'loading', message: 'Loading...' });
    setInputSearchValue('');
    setSafeType('User');
    fetchUserSafesData().catch(() => {
      setResponse({ status: 'failed', message: 'failed' });
    });
    callSearchApi();
    // eslint-disable-next-line
  }, []);

  /**
   * @function fetchSharedSafesData
   * @description function call all the manage and shared safe api.
   */
  const fetchSharedSafesData = useCallback(async () => {
    let safesApiResponse = [];
    if (
      configData.AUTH_TYPE === 'oidc' &&
      JSON.parse(sessionStorage.getItem('safesApiCount')) === 0
    ) {
      safesApiResponse = await apiService.getSafes();
    }
    const sharedListApiResponse = await apiService.getManageSharedList(
      limit,
      safeOffset
    );
    const allApiResponse = Promise.all([
      safesApiResponse,
      sharedListApiResponse,
    ]);
    allApiResponse
      .then((result) => {
        let safesObject = [];
        setSafeOffset(limit + safeOffset);
        if (configData.AUTH_TYPE === 'oidc') {
          if (result && result[0]?.data) {
            sessionStorage.setItem('safesData', JSON.stringify(result[0].data));
          }
        } else {
          safesLdapUserPassResponse();
        }
        if (result && result[1]?.data?.keys) {
          safesObject = compareSafesAndList(result[1].data.keys, 'shared');
        }

        onResponseVariableSet(safesObject);
        checkHasMoreData(result[1]);
      })
      .catch(() => {
        setResponse({ status: 'failed', message: 'failed' });
      });
    // eslint-disable-next-line
  }, [safeOffset, safeList]);

  /**
   * @function fetchAppSafesData
   * @description function call all the manage and apps safe api.
   */
  const fetchAppSafesData = useCallback(async () => {
    let safesApiResponse = [];
    if (
      configData.AUTH_TYPE === 'oidc' &&
      JSON.parse(sessionStorage.getItem('safesApiCount')) === 0
    ) {
      safesApiResponse = await apiService.getSafes();
    }
    const appsListApiResponse = await apiService.getManageAppsList(
      limit,
      safeOffset
    );
    const allApiResponse = Promise.all([safesApiResponse, appsListApiResponse]);
    allApiResponse
      .then((result) => {
        let safesObject = [];
        setSafeOffset(limit + safeOffset);
        if (configData.AUTH_TYPE === 'oidc') {
          if (result && result[0]?.data) {
            sessionStorage.setItem('safesData', JSON.stringify(result[0].data));
          }
        } else {
          safesLdapUserPassResponse();
        }
        if (result && result[1]?.data?.keys) {
          safesObject = compareSafesAndList(result[1].data.keys, 'apps');
        }
        onResponseVariableSet(safesObject);
        checkHasMoreData(result[1]);
      })
      .catch(() => {
        setResponse({ status: 'failed', message: 'failed' });
      });
    // eslint-disable-next-line
  }, [safeOffset]);

  useEffect(() => {
    if (safeList.length > 0) {
      const val = location.pathname.split('/');
      const safeName = val[val.length - 1];
      if (safeName !== 'create-safe' && safeName !== 'edit-safe') {
        const obj = safeList.find((safe) => safe.name === safeName);
        setOwnerOfSafes(false);
        if (obj) {
          setSelectedSafeDetails({ ...obj });
        } else {
          setSelectedSafeDetails(safeList[0]);
          history.push(`/safes/${safeList[0].name}`);
        }
      }
    } else {
      setSelectedSafeDetails({});
    }
    // eslint-disable-next-line
  }, [safeList, location, history]);

  const callApiBasedOnSafeType = async () => {
    setInputSearchValue('');
    if (safeType === 'User') {
      await fetchUserSafesData();
    } else if (safeType === 'Shared') {
      await fetchSharedSafesData();
    } else if (safeType === 'Application') {
      await fetchAppSafesData();
    }
  };

  const filterSearchValue = (value) => {
    const array = searchList.filter((item) => item.name.includes(value));
    if (array.length > 0) {
      setSearchMenu([...array]);
      setNoResultFound('');
    } else {
      setSearchMenu([]);
      setNoResultFound('No result found');
    }
  };

  const clearSearchData = () => {
    setSearchLoader(false);
    setSearchMenu([]);
    setNoResultFound('');
  };

  /**
   * @function onSearchChange
   * @description function to search safe.
   * @param {string} value searched input value.
   */
  const onSearchChange = (e) => {
    if (e?.target?.value?.length > 2) {
      setSearchMenu([]);
      filterSearchValue(e?.target?.value);
    } else if (e?.target?.value?.length === 0) {
      setResponse({ status: 'loading', message: 'Loading...' });
      clearData();
      clearSearchData();
    } else {
      clearSearchData();
    }
    setInputSearchValue(e?.target?.value);
  };

  /**
   * @function onSelectChange
   * @description function to filter safe.
   * @param {string} value selected filter value.
   */
  const onSelectChange = (value) => {
    if (response.status !== 'loading') {
      setSafeType(value);
      setInputSearchValue('');
      setResponse({ status: 'loading', message: 'Loading...' });
      clearData();
      setSearchSelectClicked(false);
    }
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
    setResponse({ status: 'loading', message: 'loading' });
    setSafeList([]);
    setOpenConfirmationModal(false);
    apiService
      .deleteSafe(deletionPath)
      .then(async () => {
        setDeletionPath('');
        setToast(1);
        clearData();
        await callSearchApi();
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
      sessionStorage.setItem('safesApiCount', 1);
      loadMoreData();
    }
  };

  useEffect(() => {
    if (clearedData) {
      callApiBasedOnSafeType();
      setClearedData(false);
      sessionStorage.setItem('safesApiCount', 0);
    }
    // eslint-disable-next-line
  }, [clearedData]);

  const fetchData = () => {
    setResponse({ status: 'loading', message: 'loading' });
    sessionStorage.setItem('safesApiCount', 0);
    callSearchApi();
    clearData();
  };

  const onSearchItemSelected = (value) => {
    setOwnerOfSafes(false);
    setSearchMenu([]);
    const data = JSON.parse(sessionStorage.getItem('safesData'));
    let dataObj = {};
    Object.keys(data).map((ele) => {
      if (ele === value.path) {
        data[ele].map((item) => {
          if (Object.keys(item)[0] === value.name) {
            dataObj = {
              name: value.name,
              access: Object.values(item)[0],
              path: `${value.path}/${value.name}`,
              safeType: value.type,
              manage: !!isAdmin,
            };
          }
          return null;
        });
      }
      return null;
    });
    if (Object.keys(dataObj)?.length === 0) {
      dataObj = {
        name: value.name,
        access: '',
        path: `${value.path}/${value.name}`,
        safeType: value.type,
        manage: true,
      };
    }
    setSafeList([dataObj]);
    setAllSafeList([dataObj]);
    setInputSearchValue(value.name);
    setSafeType(`${value.type}`);
    setSearchSelectClicked(true);
  };

  const renderSafes = () => {
    return (
      <LeftColumn
        onActionClicked={(e) => onActionClicked(e)}
        onLinkClicked={(safe) => onLinkClicked(safe)}
        onEditSafeClicked={(safe) => onEditSafeClicked(safe)}
        onDeleteSafeClicked={(e, safe) => onDeleteSafeClicked(e, safe)}
        history={history}
        safeList={!searchSelectClicked ? safeList : allSafeList}
        ownerOfSafes={ownerOfSafes}
        isAdmin={isAdmin}
        searchSelectClicked={searchSelectClicked}
      />
    );
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
              <SearchWrap tabIndex="0">
                <SearchboxWithDropdown
                  onSearchChange={(e) => onSearchChange(e)}
                  value={inputSearchValue || ''}
                  menu={searchMenu}
                  onChange={(value) => onSearchItemSelected(value)}
                  noResultFound={noResultFound}
                />
                {searchLoader && inputSearchValue?.length > 2 && (
                  <LoaderSpinner customStyle={customStyle} />
                )}
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
                      {isInfiniteScrollLoading && allSafeList.length < 1 && (
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
                        setOwnerOfSafes={setOwnerOfSafes}
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
                        setOwnerOfSafes={setOwnerOfSafes}
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
