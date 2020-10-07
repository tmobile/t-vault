/* eslint-disable no-param-reassign */
import React, { useState, useEffect, useCallback, useContext } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import InfiniteScroll from 'react-infinite-scroller';
import MoreVertOutlinedIcon from '@material-ui/icons/MoreVertOutlined';
import { Link, Route, Switch, useHistory, Redirect } from 'react-router-dom';

import useMediaQuery from '@material-ui/core/useMediaQuery';
import sectionHeaderBg from '../../../../../assets/Banner_img.png';
// import { values } from 'lodash';
import mediaBreakpoints from '../../../../../breakpoints';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import NoSafesIcon from '../../../../../assets/no-data-safes.svg';
import safeIcon from '../../../../../assets/icon_safes.svg';
import FloatingActionButtonComponent from '../../../../../components/FormFields/FloatingActionButton';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ListItemDetail from '../../../../../components/ListItemDetail';
import EditDeletePopper from '../EditDeletePopper';
import ListItem from '../../../../../components/ListItem';
import EditAndDeletePopup from '../../../../../components/EditAndDeletePopup';
import Error from '../../../../../components/Error';
// import OnBoardServiceAccount from '../OnBoardServiceAccounts';
import SnackbarComponent from '../../../../../components/Snackbar';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
// import apiService from '../../apiService';

// import ConfirmationModal from '../../../../../components/ConfirmationModal';
// import OnBoardForm from '../OnBoardForm';
import apiService from '../../apiService';
import Strings from '../../../../../resources';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import { IconEdit, IconDeleteActive } from '../../../../../assets/SvgIcons';
import { TitleOne } from '../../../../../styles/GlobalStyles';
import AccountSelectionTabs from '../Tabs';
import { UserContext } from '../../../../../contexts';

const ColumnSection = styled('section')`
  position: relative;
  background: ${(props) => props.backgroundColor || '#151820'};
`;

const RightColumnSection = styled(ColumnSection)`
  width: 59.23%;
  padding: 0;
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
  justify-content: space-between;
  border-bottom: 0.1rem solid #1d212c;
`;
const StyledInfiniteScroll = styled(InfiniteScroll)`
  width: 100%;
  max-height: 57vh;
  ${mediaBreakpoints.small} {
    max-height: 78vh;
  }
`;

const ListContainer = styled.div`
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

const PopperWrap = styled.div`
  position: absolute;
  top: 50%;
  right: 0%;
  z-index: 1;
  width: 5.5rem;
  transform: translate(-50%, -50%);
  display: none;
`;
const ListFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  text-decoration: none;
  align-items: center;
  padding: 1.2rem 1.8rem 1.2rem 3.4rem;
  cursor: pointer;
  background-image: ${(props) =>
    props.active ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active ? '#fff' : '#4a4a4a')};
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
  bottom: 2.8rem;
  right: 2.5rem;
`;

const SearchWrap = styled.div`
  width: 100%;
`;

const MobileViewForListDetailPage = css`
  position: fixed;
  display: flex;
  right: 0;
  left: 0;
  bottom: 0;
  top: 0;
  z-index: 1;
  overflow-y: auto;
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

const ColumnTitleWrap = styled('div')`
  display: flex;
  flex-flow: wrap;
  .button-wrap {
    display: flex;
    width: 100%;
    align-items: center;
    padding: 1.5rem 0;
    justify-content: space-between;
  }
  margin-bottom: 0.75rem;
`;
const MoreWrap = styled.div``;
const useStyles = makeStyles(() => ({
  contained: { borderRadius: '0.4rem' },
}));

const ServiceAccountDashboard = () => {
  // const [, setEnableOnBoardForm] = useState(false);
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [anchorEl, setAnchorEl] = useState(null);
  const [serviceAccountClicked, setServiceAccountClicked] = useState(false);
  const [listItemDetails, setListItemDetails] = useState({});
  const [moreData] = useState(false);
  const [isLoading] = useState(false);
  const [serviceAccountList, setServiceAccountList] = useState([]);
  const [toast] = useState(null);
  const [status, setStatus] = useState({});
  let scrollParentRef = null;
  const classes = useStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const history = useHistory();

  const introduction = Strings.Resources.serviceAccount;

  const contextObj = useContext(UserContext);

  /**
   * @function fetchData
   * @description function call all the manage and safe api.
   */
  const fetchData = useCallback(async () => {
    setStatus({ status: 'loading', message: 'Loading...' });
    const serviceList = await apiService.getServiceAccountList();
    const serviceAccounts = await apiService.getServiceAccounts();
    const allApiResponse = Promise.all([serviceList, serviceAccounts]);
    allApiResponse
      .then((response) => {
        const listArray = [];
        if (response[0] && response[0].data && response[0].data.svcacct) {
          response[0].data.svcacct.map((item) => {
            const data = {
              name: Object.keys(item)[0],
              access: Object.values(item)[0],
              admin: contextObj.isAdmin,
              manage: true,
            };
            return listArray.push(data);
          });
        }
        if (response[1] && response[1]?.data?.keys) {
          listArray.map((item) => {
            if (!response[1].data.keys.includes(item.name)) {
              item.manage = false;
            }
            return null;
          });
          response[1].data.keys.map((item) => {
            if (!listArray.some((list) => list.name === item)) {
              const data = {
                name: item,
                access: '',
                admin: contextObj.isAdmin,
                manage: true,
              };
              return listArray.push(data);
            }
            return null;
          });
          setServiceAccountList([...listArray]);
        }
        setStatus({ status: 'success', message: '' });
      })
      .catch(() => {
        setStatus({ status: 'failed', message: 'failed' });
      });
  }, [contextObj]);

  /**
   * @description On component load call fetchData function.
   */
  useEffect(() => {
    fetchData().catch(() => {
      setStatus({ status: 'failed', message: 'failed' });
    });
  }, [fetchData]);

  const showOnBoardForm = () => {
    // setEnableOnBoardForm(true);
    setServiceAccountClicked(true);
  };
  /**
   * @function onSearchChange
   * @description function to search input
   */
  const onSearchChange = (value) => {
    setInputSearchValue(value);
  };
  /**
   * @function handlePopperClick
   * @description handle edit and delete popper when we click on more
   */
  const handlePopperClick = (e) => {
    e.preventDefault();
    setAnchorEl(e.currentTarget);
  };
  /**
   * @function onLinkClicked
   * @description function to check if mobile screen the make safeClicked true
   * based on that value display left and right side.
   */
  const onLinkClicked = (item) => {
    setListItemDetails(item);
    if (isMobileScreen) {
      setServiceAccountClicked(true);
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
   * @function backToServiceAccounts
   * @description To get back to left side lists in case of mobile view
   * @param {bool} isMobileScreen boolian
   */
  const backToServiceAccounts = () => {
    if (isMobileScreen) {
      setServiceAccountClicked(false);
    }
  };

  useEffect(() => {
    if (serviceAccountList?.length > 0) {
      serviceAccountList.map((item) => {
        if (history.location.pathname === `/service-accounts/${item.name}`) {
          return setListItemDetails(item);
        }
        return null;
      });
    }
  }, [serviceAccountList, listItemDetails, history]);

  // Infine scroll load more data
  const loadMoreData = () => {};

  // toast close handler
  const onToastClose = () => {
    setStatus({});
  };
  const onServiceAccountOffBoard = () => {};
  const onServiceAccountEdit = () => {};

  // const handleConfirmationModalClose = () => {};
  const renderList = () => {
    return serviceAccountList.map((account) => (
      <ListFolderWrap
        key={account.name}
        to={{
          pathname: `/service-accounts/${account.name}`,
          state: { data: account },
        }}
        onClick={() => onLinkClicked(account)}
        active={
          history.location.pathname === `/service-accounts/${account.name}`
        }
      >
        <ListItem
          title={account.name}
          subTitle={account.date}
          flag={account.type}
          icon={safeIcon}
          showActions={false}
          popperListItems={[
            { title: 'Edit', icon: <IconEdit /> },
            { title: 'delete', icon: <IconDeleteActive /> },
          ]}
        />
        <BorderLine />
        {account.name && !isMobileScreen ? (
          <PopperWrap onClick={(e) => onActionClicked(e)}>
            <EditAndDeletePopup
              //   onDeleteListItemClicked={() => onDeleteSafeClicked(safe.path)}
              item={account}
              path="/service-accounts/edit-service-account"
            />
          </PopperWrap>
        ) : null}
        {isMobileScreen && account.name ? (
          <div>
            <MoreWrap
              role="button"
              onClick={(e) => handlePopperClick(e)}
              tab-index={-1}
            >
              <MoreVertOutlinedIcon />
            </MoreWrap>
            <EditDeletePopper
              onDeleteClicked={onServiceAccountOffBoard}
              onEditClicked={onServiceAccountEdit}
              setAnchorEl={setAnchorEl}
              anchorEl={anchorEl}
            />
          </div>
        ) : null}
      </ListFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        {/* <ConfirmationModal
          open={onBoardConfirmationModal}
          handleClose={handleConfirmationModalClose}
          title="Onboarding Service Account"
          description="The password for this service account will expire in 365 days and will not be enabled for auto rotation by T-Vault. You need to makes sure the passwod for this service account is getting roated appropriately."
          confirmButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label="Confirm"
              color="secondary"
              onClick={() => setOnBoardConfirmationModal(false)}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        /> */}
        <SectionPreview title="service-account-section">
          <LeftColumnSection isAccountDetailsOpen={serviceAccountClicked}>
            <ColumnHeader>
              <ColumnTitleWrap>
                <div className="button-wrap">
                  <TitleOne extraCss="font-weight:600">
                    SERVICE ACCOUNTS
                  </TitleOne>
                  <ButtonComponent
                    color="secondary"
                    icon="add"
                    label="Onboard Account"
                    onClick={() => showOnBoardForm()}
                    classes={classes}
                    href="/service-accounts/change-service-accounts"
                  />
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
              </ColumnTitleWrap>
            </ColumnHeader>
            {status.status === 'loading' && (
              <ScaledLoader contentHeight="80%" contentWidth="100%" />
            )}
            {status.status === 'failed' && !serviceAccountList?.length && (
              <EmptyContentBox>
                {' '}
                <Error description="Error while fetching service accounts!" />
              </EmptyContentBox>
            )}
            {status.status === 'success' && (
              <>
                {serviceAccountList && serviceAccountList.length > 0 ? (
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
                  serviceAccountList?.length === 0 && (
                    <NoDataWrapper>
                      {' '}
                      <NoListWrap>
                        <NoData
                          imageSrc={NoSafesIcon}
                          description="Onbaord a service account to get started!"
                          actionButton={
                            // eslint-disable-next-line react/jsx-wrap-multilines
                            <FloatingActionButtonComponent
                              href="/service-accounts/onboard-service-account"
                              color="secondary"
                              icon="add"
                              tooltipTitle="Onboard New Service Account"
                              tooltipPos="bottom"
                            />
                          }
                        />
                      </NoListWrap>
                    </NoDataWrapper>
                  )
                )}
              </>
            )}

            {serviceAccountList?.length ? (
              <FloatBtnWrapper>
                <FloatingActionButtonComponent
                  href="/service-accounts/onboard-service-account"
                  color="secondary"
                  icon="add"
                  tooltipTitle="Onboard New Service Account"
                  tooltipPos="left"
                />
              </FloatBtnWrapper>
            ) : (
              <></>
            )}
          </LeftColumnSection>
          <RightColumnSection
            mobileViewStyles={isMobileScreen ? MobileViewForListDetailPage : ''}
            isAccountDetailsOpen={serviceAccountClicked}
          >
            <Switch>
              {serviceAccountList[0]?.name && (
                <Redirect
                  exact
                  from="/service-accounts"
                  to={{
                    pathname: `/service-accounts/${serviceAccountList[0]?.name}`,
                    state: { data: serviceAccountList[0] },
                  }}
                />
              )}
              <Route
                path="/service-accounts/:serviceAccountName"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={backToServiceAccounts}
                    ListDetailHeaderBg={sectionHeaderBg}
                    description={introduction}
                    renderContent={
                      <AccountSelectionTabs accountDetail={listItemDetails} />
                    }
                  />
                )}
              />
              <Route
                path="/service-accounts"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={serviceAccountList}
                    params={routerProps}
                    backToLists={backToServiceAccounts}
                    ListDetailHeaderBg={sectionHeaderBg}
                    description={introduction}
                    renderContent={
                      <AccountSelectionTabs accountDetail={listItemDetails} />
                    }
                  />
                )}
              />
            </Switch>
          </RightColumnSection>
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
              message="Service account off-boarded successfully!"
            />
          )}
        </SectionPreview>
      </>
    </ComponentError>
  );
};
ServiceAccountDashboard.propTypes = {};
ServiceAccountDashboard.defaultProps = {};

export default ServiceAccountDashboard;
