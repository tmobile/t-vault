/* eslint-disable no-param-reassign */
/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect, useCallback, lazy } from 'react';
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
import sectionHeaderBg from '../../../../../assets/svc_banner_img.png';
import mediaBreakpoints from '../../../../../breakpoints';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import NoSafesIcon from '../../../../../assets/no-data-safes.svg';
import svcIcon from '../../../../../assets/icon-service-account.svg';
import mobSvcIcon from '../../../../../assets/mob-svcbg.png';
import tabSvcIcon from '../../../../../assets/tab-svcbg.png';
import FloatingActionButtonComponent from '../../../../../components/FormFields/FloatingActionButton';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ListItemDetail from '../../../../../components/ListItemDetail';
import EditDeletePopper from '../EditDeletePopper';
import ListItem from '../../../../../components/ListItem';
import EditAndDeletePopup from '../../../../../components/EditAndDeletePopup';
import Error from '../../../../../components/Error';
import SnackbarComponent from '../../../../../components/Snackbar';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import apiService from '../../apiService';
import Strings from '../../../../../resources';
import { TitleOne } from '../../../../../styles/GlobalStyles';
import AccountSelectionTabs from '../Tabs';
import DeletionConfirmationModal from './components/DeletionConfirmationModal';
import TransferConfirmationModal from './components/TransferConfirmationModal';
import {
  ListContainer,
  ListContent,
} from '../../../../../styles/GlobalStyles/listingStyle';
import configData from '../../../../../config/config';

const OnBoardForm = lazy(() => import('../../OnBoardForm'));

const ColumnSection = styled('section')`
  position: relative;
  background: ${(props) => props.backgroundColor || '#151820'};
`;

const RightColumnSection = styled(ColumnSection)`
  width: 59.23%;
  padding: 0;
  // background: linear-gradient(to top, #151820, #2c3040);
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

const ListHeader = css`
  width: 22rem;
  text-transform: capitalize;
  font-weight: 600;
  ${mediaBreakpoints.smallAndMedium} {
    width: 18rem;
  }
`;

const customStyle = css`
  justify-content: center;
`;

const EditDeletePopperWrap = styled.div``;

const iconStyles = makeStyles(() => ({
  root: {
    width: '100%',
  },
}));

const ServiceAccountDashboard = () => {
  const [
    transferSvcAccountConfirmation,
    setTransferSvcAccountConfirmation,
  ] = useState(false);
  const [transferResponse, setTransferResponse] = useState(false);
  const [transferResponseDesc, setTransferResponseDesc] = useState('');
  const [transferName, setTransferName] = useState('');
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [serviceAccountClicked, setServiceAccountClicked] = useState(false);
  const [listItemDetails, setListItemDetails] = useState({});
  const [serviceAccountList, setServiceAccountList] = useState([]);
  const [toastResponse, setToastResponse] = useState(null);
  const [response, setResponse] = useState({});
  const [deleteAccName, setDeleteAccName] = useState('');
  const [offBoardSuccessfull, setOffBoardSuccessfull] = useState(false);
  const [allServiceAccountList, setAllServiceAccountList] = useState([]);
  const [
    offBoardSvcAccountConfirmation,
    setOffBoardSvcAccountConfirmation,
  ] = useState(false);
  const [state, dispatch] = useStateValue();
  const listIconStyles = iconStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const isTabScreen = useMediaQuery(mediaBreakpoints.medium);
  const history = useHistory();
  const location = useLocation();
  const admin = Boolean(state.isAdmin);

  const introduction = Strings.Resources.serviceAccount;

  /**
   * @function fetchData
   * @description function call all the manage and safe api.
   */
  const fetchData = useCallback(async () => {
    setResponse({ status: 'loading', message: 'Loading...' });
    setInputSearchValue('');
    setListItemDetails({});
    let serviceList = [];
    if (configData.AUTH_TYPE === 'oidc') {
      serviceList = await apiService.getServiceAccountList();
    }
    const serviceAccounts = await apiService.getServiceAccounts();
    const allApiResponse = Promise.all([serviceList, serviceAccounts]);
    allApiResponse
      .then((result) => {
        const listArray = [];
        if (configData.AUTH_TYPE === 'oidc') {
          if (result[0] && result[0].data && result[0].data.svcacct) {
            result[0].data.svcacct.map((item) => {
              const data = {
                name: Object.keys(item)[0],
                access: Object.values(item)[0],
                admin,
                manage: true,
              };
              return listArray.push(data);
            });
          }
        } else {
          const access = JSON.parse(sessionStorage.getItem('access'));
          if (Object.keys(access).length > 0) {
            Object.keys(access).forEach((item) => {
              if (item === 'svcacct') {
                access[item].map((ele) => {
                  const data = {
                    name: Object.keys(ele)[0],
                    access: Object.values(ele)[0],
                    admin,
                    manage: true,
                  };
                  return listArray.push(data);
                });
              }
            });
          }
        }
        if (result[1] && result[1]?.data?.keys) {
          listArray.map((item) => {
            if (!result[1].data.keys.includes(item.name)) {
              item.manage = false;
            }
            return null;
          });
          result[1].data.keys.map((item) => {
            if (!listArray.some((list) => list.name === item)) {
              const data = {
                name: item,
                access: '',
                admin,
                manage: true,
              };
              return listArray.push(data);
            }
            return null;
          });
          setServiceAccountList([...listArray]);
          setAllServiceAccountList([...listArray]);
          dispatch({
            type: 'GET_ALL_SERVICE_ACCOUNT_LIST',
            payload: [...listArray],
          });
        }
        setResponse({ status: 'success', message: '' });
      })
      .catch(() => {
        setResponse({ status: 'failed', message: 'failed' });
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
      const array = state?.serviceAccountList?.filter((item) => {
        return item?.name?.toLowerCase().includes(value?.toLowerCase().trim());
      });
      setServiceAccountList([...array]);
    } else {
      setServiceAccountList([...state?.serviceAccountList]);
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
    if (allServiceAccountList.length > 0) {
      const val = location.pathname.split('/');
      const svcName = val[val.length - 1];
      if (
        svcName !== 'onboard-service-accounts' &&
        svcName !== 'edit-service-accounts'
      ) {
        const obj = allServiceAccountList.find((svc) => svc.name === svcName);
        if (obj) {
          if (listItemDetails.name !== obj.name) {
            setListItemDetails({ ...obj });
          }
        } else {
          setListItemDetails(allServiceAccountList[0]);
          history.push(`/service-accounts/${allServiceAccountList[0].name}`);
        }
      }
    }
    // eslint-disable-next-line
  }, [allServiceAccountList, location, history]);

  // toast close handler
  const onToastClose = () => {
    setToastResponse(null);
  };

  /**
   * @function onDeleteClicked
   * @description function is called when delete is clicked opening
   * the confirmation modal and setting the path.
   * @param {string} name service acc name to be deleted.
   */
  const onDeleteClicked = (name) => {
    setOffBoardSvcAccountConfirmation(true);
    setDeleteAccName(name);
  };

  const onServiceAccountEdit = (name) => {
    history.push({
      pathname: '/service-accounts/edit-service-accounts',
      state: {
        serviceAccountDetails: {
          name,
          isAdmin: admin,
          isEdit: true,
        },
      },
    });
  };
  /**
   * @function deleteServiceAccount
   * @description function is called when delete is clicked opening
   * the confirmation modal and setting the path.
   * @param {string} name service acc name to be deleted.
   */
  const deleteServiceAccount = (owner) => {
    const payload = {
      name: deleteAccName,
      owner,
    };
    apiService
      .offBoardServiceAccount(payload)
      .then(() => {
        fetchData();
        setOffBoardSuccessfull(true);
      })
      .catch(() => {
        setToastResponse(-1);
      });
  };

  useEffect(() => {
    if (offBoardSuccessfull) {
      setOffBoardSvcAccountConfirmation(true);
    }
  }, [offBoardSuccessfull]);

  /**
   * @function onServiceAccountOffBoard
   * @description function is to fetch the service account details and check username
   */
  const onServiceAccountOffBoard = () => {
    setOffBoardSvcAccountConfirmation(false);
    setResponse({ status: 'loading' });
    apiService
      .fetchServiceAccountDetails(deleteAccName)
      .then((res) => {
        let details = {};
        if (res?.data?.data?.values && res.data.data.values[0]) {
          details = { ...res.data.data.values[0] };
          if (details?.managedBy?.userName) {
            deleteServiceAccount(details.managedBy.userName);
          }
        }
      })
      .catch(() => {
        setToastResponse(-1);
      });
  };

  /**
   * @function handleSuccessfullConfirmation
   * @description function to handle the deletion successful modal.
   */
  const handleSuccessfullConfirmation = () => {
    setOffBoardSvcAccountConfirmation(false);
    setOffBoardSuccessfull(false);
  };

  /**
   * @function handleConfirmationModalClose
   * @description function to handle the close of deletion modal.
   */
  const handleConfirmationModalClose = () => {
    setOffBoardSvcAccountConfirmation(false);
  };

  /**
   * @function onTransferOwnerClicked
   * @description function open transfer owner modal.
   */
  const onTransferOwnerClicked = (name) => {
    setTransferSvcAccountConfirmation(true);
    setTransferName(name);
  };

  /**
   * @function onTransferOwnerCancelClicked
   * @description function to handle the close of transfer owner modal.
   */
  const onTransferOwnerCancelClicked = () => {
    setTransferSvcAccountConfirmation(false);
    setTransferResponse(false);
  };

  const onTranferConfirmationClicked = () => {
    setResponse({ status: 'loading' });
    setTransferSvcAccountConfirmation(false);
    apiService
      .transferOwner(transferName)
      .then(async (res) => {
        setTransferResponse(true);
        setTransferSvcAccountConfirmation(true);
        if (res?.data?.messages && res.data.messages[0]) {
          setTransferResponseDesc(res.data.messages[0]);
        }
        await fetchData();
      })
      .catch(() => {
        setToastResponse(-1);
      });
  };

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
            ? 'true'
            : 'false'
        }
      >
        <ListItem
          title={account.name}
          subTitle={account.date}
          flag={account.type}
          icon={svcIcon}
          showActions={false}
          listIconStyles={listIconStyles}
        />
        <BorderLine />
        {account.name && !isMobileScreen ? (
          <PopperWrap onClick={(e) => onActionClicked(e)}>
            <EditAndDeletePopup
              onDeletListItemClicked={() => onDeleteClicked(account.name)}
              onEditListItemClicked={() => onServiceAccountEdit(account.name)}
              admin={admin}
              manage={account?.manage}
              isSvcAcct
              onTransferOwnerClicked={() =>
                onTransferOwnerClicked(account.name)
              }
            />
          </PopperWrap>
        ) : null}
        {isMobileScreen && (account?.admin || account?.manage) && (
          <EditDeletePopperWrap onClick={(e) => onActionClicked(e)}>
            <EditDeletePopper
              onDeleteClicked={() => onDeleteClicked(account.name)}
              onEditClicked={() => onServiceAccountEdit(account.name)}
              admin={admin}
              manage={account?.manage}
              isSvcAcct
              onTransferOwnerClicked={() =>
                onTransferOwnerClicked(account.name)
              }
            />
          </EditDeletePopperWrap>
        )}
      </ListFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        <DeletionConfirmationModal
          offBoardSvcAccountConfirmation={offBoardSvcAccountConfirmation}
          offBoardSuccessfull={offBoardSuccessfull}
          handleSuccessfullConfirmation={handleSuccessfullConfirmation}
          handleConfirmationModalClose={handleConfirmationModalClose}
          onServiceAccountOffBoard={onServiceAccountOffBoard}
        />
        <TransferConfirmationModal
          transferSvcAccountConfirmation={transferSvcAccountConfirmation}
          onTransferOwnerCancelClicked={onTransferOwnerCancelClicked}
          transferResponse={transferResponse}
          transferResponseDesc={transferResponseDesc}
          onTranferConfirmationClicked={onTranferConfirmationClicked}
        />
        <SectionPreview title="service-account-section">
          <LeftColumnSection isAccountDetailsOpen={serviceAccountClicked}>
            <ColumnHeader>
              <div style={{ margin: '0 1rem' }}>
                <TitleOne extraCss={ListHeader}>
                  {`Service Accounts (${serviceAccountList?.length})`}
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
            {response.status === 'failed' && !serviceAccountList?.length && (
              <EmptyContentBox>
                <Error description="Error while fetching service accounts!" />
              </EmptyContentBox>
            )}
            {response.status === 'success' && (
              <>
                {serviceAccountList && serviceAccountList.length > 0 ? (
                  <ListContainer>
                    <ListContent>{renderList()}</ListContent>
                  </ListContainer>
                ) : (
                  serviceAccountList?.length === 0 && (
                    <>
                      {inputSearchValue ? (
                        <NoDataWrapper>
                          No service account found with name:
                          <strong>{inputSearchValue}</strong>
                        </NoDataWrapper>
                      ) : (
                        <NoDataWrapper>
                          <NoListWrap>
                            <NoData
                              imageSrc={NoSafesIcon}
                              description="No service accounts are associated with you yet!, If you are a admin please onboard a service account to get started!"
                              actionButton={
                                admin ? (
                                  <FloatingActionButtonComponent
                                    href="/service-accounts/onboard-service-accounts"
                                    color="secondary"
                                    icon="add"
                                    tooltipTitle="Onboard New Service Account"
                                    tooltipPos="left"
                                  />
                                ) : (
                                  <></>
                                )
                              }
                              customStyle={customStyle}
                            />
                          </NoListWrap>
                        </NoDataWrapper>
                      )}
                    </>
                  )
                )}
              </>
            )}

            {serviceAccountList?.length && admin ? (
              <FloatBtnWrapper>
                <FloatingActionButtonComponent
                  href="/service-accounts/onboard-service-accounts"
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
                    ListDetailHeaderBg={
                      isTabScreen
                        ? tabSvcIcon
                        : isMobileScreen
                        ? mobSvcIcon
                        : sectionHeaderBg
                    }
                    description={introduction}
                    renderContent={
                      <AccountSelectionTabs
                        accountDetail={listItemDetails}
                        refresh={() => fetchData()}
                      />
                    }
                  />
                )}
              />
              <Route
                path="/service-accounts"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={backToServiceAccounts}
                    ListDetailHeaderBg={
                      isTabScreen
                        ? tabSvcIcon
                        : isMobileScreen
                        ? mobSvcIcon
                        : sectionHeaderBg
                    }
                    description={introduction}
                    renderContent={
                      <AccountSelectionTabs
                        accountDetail={listItemDetails}
                        refresh={() => fetchData()}
                      />
                    }
                  />
                )}
              />
            </Switch>
          </RightColumnSection>
          {toastResponse === -1 && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              severity="error"
              icon="error"
              message="Something went wrong!"
            />
          )}
          {toastResponse === 1 && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              message="Service account off-boarded successfully!"
            />
          )}
        </SectionPreview>
        <Switch>
          <Route
            exact
            path="/service-accounts/onboard-service-accounts"
            render={(routeProps) => (
              <OnBoardForm routeProps={{ ...routeProps }} refresh={fetchData} />
            )}
          />
          <Route
            exact
            path="/service-accounts/edit-service-accounts"
            render={(routeProps) => (
              <OnBoardForm routeProps={{ ...routeProps }} refresh={fetchData} />
            )}
          />
        </Switch>
      </>
    </ComponentError>
  );
};

export default ServiceAccountDashboard;
