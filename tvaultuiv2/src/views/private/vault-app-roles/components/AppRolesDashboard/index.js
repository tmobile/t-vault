/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable no-param-reassign */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';

import {
  Link,
  Route,
  Switch,
  useHistory,
  Redirect,
  useLocation,
} from 'react-router-dom';

import useMediaQuery from '@material-ui/core/useMediaQuery';
import { useStateValue } from '../../../../../contexts/globalState';
import sectionHeaderBg from '../../../../../assets/approle_banner_img.png';
import sectionTabHeaderBg from '../../../../../assets/tab-vaultbg.png';
import sectionMobHeaderBg from '../../../../../assets/mob-vaultbg.png';
import mediaBreakpoints from '../../../../../breakpoints';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import NoSafesIcon from '../../../../../assets/no-data-safes.svg';
import appRoleIcon from '../../../../../assets/icon-approle.svg';
import FloatingActionButtonComponent from '../../../../../components/FormFields/FloatingActionButton';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ListItemDetail from '../../../../../components/ListItemDetail';
import AppRoleDetails from '../ApproleDetails';
import EditDeletePopper from '../EditDeletePopper';
import ListItem from '../../../../../components/ListItem';
import EditAndDeletePopup from '../../../../../components/EditAndDeletePopup';
import Error from '../../../../../components/Error';
import SnackbarComponent from '../../../../../components/Snackbar';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import apiService from '../../apiService';
import Strings from '../../../../../resources';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import CreateAppRole from '../../CreateAppRole';
import { TitleOne } from '../../../../../styles/GlobalStyles';
import {
  ListContainer,
  NoResultFound,
  StyledInfiniteScroll,
} from '../../../../../styles/GlobalStyles/listingStyle';

const ColumnSection = styled('section')`
  position: relative;
  background: ${(props) => props.backgroundColor || '#151820'};
`;

const RightColumnSection = styled.div`
  width: 59.23%;
  padding: 0;
  background: none;
  background: linear-gradient(to top, #151820, #2c3040);
  ${mediaBreakpoints.small} {
    width: 100%;
    ${(props) => props.mobileViewStyles}
    display: ${(props) => (props.isAccountDetailsOpen ? 'block' : 'none')};
  }
`;
const LeftColumnSection = styled(ColumnSection)`
  width: 40.77%;
  ${mediaBreakpoints.small} {
    display: ${(props) => (props.isAccountDetailsOpen ? 'none' : 'block')};
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
  height: 6.5rem;
  justify-content: space-between;
  border-bottom: 0.1rem solid #1d212c;
`;

const NoDataWrapper = styled.div`
  height: 61vh;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const PopperWrap = styled.div`
  position: absolute;
  right: 4%;
  z-index: 1;
  display: none;
`;
const ListFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  text-decoration: none;
  align-items: center;
  padding: 1.2rem 1.8rem 1.2rem 3.8rem;
  cursor: pointer;
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

const NoListWrap = styled.div`
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
  width: 100%;
`;

const ListHeader = css`
  width: 22rem;
  text-transform: capitalize;
  font-weight: 600;
  ${mediaBreakpoints.small} {
    width: 19rem;
  }
`;

const MobileViewForListDetailPage = css`
  position: fixed;
  display: flex;
  right: 0;
  left: 0;
  bottom: 0;
  top: 0;
  overflow-y: auto;
  max-height: 100%;
  z-index: 20;
`;
const EmptyContentBox = styled('div')`
  width: 100%;
  position: absolute;
  display: flex;
  justify-content: center;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
`;

const EditDeletePopperWrap = styled.div``;
const iconStyles = makeStyles(() => ({
  root: {
    width: '100%',
  },
}));

const AppRolesDashboard = () => {
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [appRoleClicked, setAppRoleClicked] = useState(false);
  const [listItemDetails, setListItemDetails] = useState({});
  const [moreData] = useState(false);
  const [isLoading] = useState(false);
  const [appRoleList, setAppRoleList] = useState([]);
  const [response, setResponse] = useState({});
  const [responseType, setResponseType] = useState(null);
  const [deleteAppRoleName, setDeleteAppRoleName] = useState('');
  const [deleteAppRoleConfirmation, setDeleteAppRoleConfirmation] = useState(
    false
  );
  const [toastMessage, setToastMessage] = useState('');
  const [state, dispatch] = useStateValue();
  let scrollParentRef = null;
  const listIconStyles = iconStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const isTabScreen = useMediaQuery(mediaBreakpoints.medium);
  const history = useHistory();
  const location = useLocation();
  const introduction = Strings.Resources.appRoles;

  const admin = Boolean(state.isAdmin);
  /**
   * @function fetchData
   * @description function call all the manage and safe api.
   */
  const fetchData = useCallback(async () => {
    setListItemDetails({});
    setInputSearchValue('');
    setResponse({ status: 'loading' });
    apiService
      .getAppRole()
      .then((res) => {
        setResponse({ status: 'success' });
        const appRolesArr = [];
        if (res?.data?.keys) {
          res.data.keys.map((item) => {
            const appObj = {
              name: item,
              admin,
            };
            return appRolesArr.push(appObj);
          });
        }
        setAppRoleList([...appRolesArr]);
        dispatch({ type: 'UPDATE_APP_ROLE_LIST', payload: [...appRolesArr] });
      })
      .catch(() => {
        setResponse({ status: 'failed' });
      });
  }, [admin, dispatch]);

  /**
   * @description On component load call fetchData function.
   */
  useEffect(() => {
    fetchData().catch(() => {
      setResponse({ status: 'failed', message: 'failed' });
    });
  }, [fetchData]);

  /**
   * @function onSearchChange
   * @description function to search input
   */
  const onSearchChange = (value) => {
    setInputSearchValue(value);
    if (value !== '') {
      const array = state?.appRoleList?.filter((item) => {
        return item?.name?.toLowerCase().includes(value?.toLowerCase().trim());
      });
      setAppRoleList([...array]);
    } else {
      setAppRoleList([...state?.appRoleList]);
    }
  };

  /**
   * @function onLinkClicked
   * @description function to check if mobile screen the make safeClicked true
   * based on that value display left and right side.
   */
  const onLinkClicked = (item) => {
    setListItemDetails(item);
    if (isMobileScreen) {
      setAppRoleClicked(true);
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
   * @function backToAppRoles
   * @description To get back to left side lists in case of mobile view
   * @param {bool} isMobileScreen boolian
   */
  const backToAppRoles = () => {
    if (isMobileScreen) {
      setAppRoleClicked(false);
    }
  };

  useEffect(() => {
    if (state?.appRoleList?.length > 0) {
      const val = location.pathname.split('/');
      const roleName = val[val.length - 1];
      if (
        roleName !== 'create-vault-app-role' &&
        roleName !== 'edit-vault-app-role'
      ) {
        const obj = state?.appRoleList.find((role) => role.name === roleName);
        if (obj) {
          if (listItemDetails.name !== obj.name) {
            setListItemDetails({ ...obj });
          }
        } else {
          setListItemDetails(state?.appRoleList[0]);
          history.push(`/vault-app-roles/${state?.appRoleList[0].name}`);
        }
      }
    } else {
      setListItemDetails({});
    }
    // eslint-disable-next-line
  }, [state, location, history]);

  // Infine scroll load more data
  const loadMoreData = () => {};

  // toast close handler
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  /**
   * @function onDeleteClicked
   * @description function is called when delete is clicked opening
   * the confirmation modal and setting the path.
   * @param {string} name app role  name to be deleted.
   */
  const onDeleteClicked = (name) => {
    setDeleteAppRoleConfirmation(true);
    setDeleteAppRoleName(name);
  };

  const onApproleEdit = (name) => {
    history.push({
      pathname: '/vault-app-roles/edit-vault-app-role',
      state: {
        appRoleDetails: {
          name,
          isAdmin: admin,
          isEdit: true,
          allAppRoles: appRoleList,
        },
      },
    });
  };

  /**
   * @function onAppRoleDelete
   * @description delete app role
   */
  const onAppRoleDelete = () => {
    setDeleteAppRoleConfirmation(false);
    setResponse({ status: 'loading' });
    apiService
      .deleteAppRole(deleteAppRoleName)
      .then(async (res) => {
        setResponseType(1);
        if (res?.data?.messages && res?.data?.messages[0]) {
          setToastMessage(res?.data?.messages[0]);
        }
        await fetchData();
      })
      .catch((err) => {
        setResponseType(-1);
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setToastMessage(err?.response?.data?.errors[0]);
        }
      });
  };

  /**
   * @function handleConfirmationModalClose
   * @description function to handle the close of deletion modal.
   */
  const handleConfirmationModalClose = () => {
    setDeleteAppRoleConfirmation(false);
  };

  const renderList = () => {
    return appRoleList.map((appRole) => (
      <ListFolderWrap
        key={appRole.name}
        to={{
          pathname: `/vault-app-roles/${appRole.name}`,
          state: { data: appRole },
        }}
        onClick={() => onLinkClicked(appRole)}
        active={
          history.location.pathname === `/vault-app-roles/${appRole.name}`
            ? 'true'
            : 'false'
        }
      >
        <ListItem
          title={appRole.name}
          subTitle={appRole.date}
          flag={appRole.type}
          icon={appRoleIcon}
          showActions={false}
          listIconStyles={listIconStyles}
        />
        <BorderLine />
        {appRole.name && !isMobileScreen ? (
          <PopperWrap onClick={(e) => onActionClicked(e)}>
            <EditAndDeletePopup
              onDeletListItemClicked={() => onDeleteClicked(appRole.name)}
              onEditListItemClicked={() => onApproleEdit(appRole.name)}
            />
          </PopperWrap>
        ) : null}
        {isMobileScreen && appRole.name && (
          <EditDeletePopperWrap onClick={(e) => onActionClicked(e)}>
            <EditDeletePopper
              onDeleteClicked={() => onDeleteClicked(appRole.name)}
              onEditClicked={() => onApproleEdit(appRole.name)}
            />
          </EditDeletePopperWrap>
        )}
      </ListFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={deleteAppRoleConfirmation}
          handleClose={handleConfirmationModalClose}
          title="Confirmation"
          description={`<p>Are you sure you want to delete this appRole : <strong>${deleteAppRoleName}</strong></p>`}
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleConfirmationModalClose()}
              width={isMobileScreen ? '44%' : ''}
            />
          }
          confirmButton={
            <ButtonComponent
              label="Delete"
              color="secondary"
              onClick={() => onAppRoleDelete()}
              width={isMobileScreen ? '44%' : ''}
            />
          }
        />
        <SectionPreview title="vault-app-roles-section">
          <LeftColumnSection isAccountDetailsOpen={appRoleClicked}>
            <ColumnHeader>
              <div style={{ margin: '0 1rem' }}>
                <TitleOne extraCss={ListHeader}>
                  {`All Vault AppRoles (${appRoleList?.length})`}
                </TitleOne>
              </div>
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
            {response.status === 'loading' && (
              <ScaledLoader contentHeight="80%" contentWidth="100%" />
            )}
            {response.status === 'failed' && !appRoleList?.length && (
              <EmptyContentBox>
                <Error description="Error while fetching app roles!" />
              </EmptyContentBox>
            )}
            {response.status === 'success' && (
              <>
                {appRoleList?.length > 0 ? (
                  <ListContainer
                    // eslint-disable-next-line no-return-assign
                    ref={(ref) => (scrollParentRef = ref)}
                  >
                    <StyledInfiniteScroll
                      pageStart={0}
                      loadMore={() => {
                        loadMoreData();
                      }}
                      hasMore={moreData}
                      threshold={100}
                      loader={
                        !isLoading ? <div key={0}>Loading...</div> : <></>
                      }
                      useWindow={false}
                      getScrollParent={() => scrollParentRef}
                    >
                      {renderList()}
                    </StyledInfiniteScroll>
                  </ListContainer>
                ) : (
                  appRoleList?.length === 0 && (
                    <>
                      {inputSearchValue ? (
                        <NoResultFound>
                          No app role found with name:
                          <div>{inputSearchValue}</div>
                        </NoResultFound>
                      ) : (
                        <NoDataWrapper>
                          <NoListWrap>
                            <NoData
                              imageSrc={NoSafesIcon}
                              description="No approles are created yet!"
                              actionButton={
                                // eslint-disable-next-line react/jsx-wrap-multilines

                                <FloatingActionButtonComponent
                                  href="/vault-app-roles/create-vault-app-role"
                                  color="secondary"
                                  icon="add"
                                  tooltipTitle="Create New Approle"
                                  tooltipPos="bottom"
                                />
                              }
                            />
                          </NoListWrap>
                        </NoDataWrapper>
                      )}
                    </>
                  )
                )}
              </>
            )}

            {appRoleList?.length ? (
              <FloatBtnWrapper>
                <FloatingActionButtonComponent
                  href="/vault-app-roles/create-vault-app-role"
                  color="secondary"
                  icon="add"
                  tooltipTitle="Create New Approle"
                  tooltipPos="left"
                />
              </FloatBtnWrapper>
            ) : (
              <></>
            )}
          </LeftColumnSection>
          <RightColumnSection
            mobileViewStyles={isMobileScreen ? MobileViewForListDetailPage : ''}
            isAccountDetailsOpen={appRoleClicked}
          >
            <Switch>
              {appRoleList[0]?.name && (
                <Redirect
                  exact
                  from="/vault-app-roles"
                  to={{
                    pathname: `/vault-app-roles/${appRoleList[0]?.name}`,
                    state: { data: appRoleList[0] },
                  }}
                />
              )}
              <Route
                path="/vault-app-roles/:appRoleName"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={backToAppRoles}
                    ListDetailHeaderBg={
                      isTabScreen
                        ? sectionTabHeaderBg
                        : isMobileScreen
                        ? sectionMobHeaderBg
                        : sectionHeaderBg
                    }
                    description={introduction}
                    renderContent={
                      <AppRoleDetails appRoleDetail={listItemDetails} />
                    }
                  />
                )}
              />
              <Route
                path="/vault-app-roles"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={backToAppRoles}
                    ListDetailHeaderBg={
                      isTabScreen
                        ? sectionTabHeaderBg
                        : isMobileScreen
                        ? sectionMobHeaderBg
                        : sectionHeaderBg
                    }
                    description={introduction}
                    renderContent={
                      <AppRoleDetails appRoleDetail={listItemDetails} />
                    }
                  />
                )}
              />
            </Switch>
          </RightColumnSection>
          {responseType === -1 && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              severity="error"
              icon="error"
              message={toastMessage || 'Something went wrong!'}
            />
          )}
          {responseType === 1 && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              message={toastMessage || 'Successful!'}
            />
          )}
        </SectionPreview>
        <Switch>
          <Route
            exact
            path="/vault-app-roles/create-vault-app-role"
            render={(routeProps) => (
              <CreateAppRole routeProps={routeProps} refresh={fetchData} />
            )}
          />
          <Route
            exact
            path="/vault-app-roles/edit-vault-app-role"
            render={(routeProps) => (
              <CreateAppRole routeProps={routeProps} refresh={fetchData} />
            )}
          />
        </Switch>
      </>
    </ComponentError>
  );
};
AppRolesDashboard.propTypes = {};
AppRolesDashboard.defaultProps = {};

export default AppRolesDashboard;
