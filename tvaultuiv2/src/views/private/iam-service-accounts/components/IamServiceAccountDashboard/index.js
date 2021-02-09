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
import VisibilityIcon from '@material-ui/icons/Visibility';
import { useStateValue } from '../../../../../contexts/globalState';
import sectionHeaderBg from '../../../../../assets/svc_banner_img.png';
import mobSvcIcon from '../../../../../assets/mob-svcbg.png';
import tabSvcIcon from '../../../../../assets/tab-svcbg.png';
import mediaBreakpoints from '../../../../../breakpoints';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import NoSafesIcon from '../../../../../assets/no-data-safes.svg';
import svcIcon from '../../../../../assets/icon-service-account.svg';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ListItemDetail from '../../../../../components/ListItemDetail';
import ListItem from '../../../../../components/ListItem';
import Error from '../../../../../components/Error';
import SnackbarComponent from '../../../../../components/Snackbar';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import ViewIamServiceAccount from '../IamServiceAccountPreview';
import apiService from '../../apiService';
import Strings from '../../../../../resources';
import { TitleOne } from '../../../../../styles/GlobalStyles';
import AccountSelectionTabs from '../IamSvcAccountTabs';
import { ListContent } from '../../../../../styles/GlobalStyles/listingStyle';
import { getEachUsersDetails } from '../../../../../services/helper-function';

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

const ListContainer = styled.div`
  overflow: auto;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  ::-webkit-scrollbar-track {
    -webkit-box-shadow: none !important;
    background-color: transparent;
  }
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
      display: flex;
      width: 3rem;
      height: 3rem;
      align-items: center;
      justify-content: center;
      margin-left: 0.75rem;
      padding: 0.9rem 0.5rem 0.4rem 0.6rem;
      border-radius: 50%;
      :hover {
        background-color: rgb(90, 99, 122);
      }
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

const SearchWrap = styled.div`
  width: 100%;
`;

const MobileViewForListDetailPage = css`
  position: fixed;
  display: flex;
  right: 0;
  left: 0;
  bottom: 0;
  top: 7rem;
  z-index: 1;
  overflow-y: auto;
  ::-webkit-scrollbar-track {
    -webkit-box-shadow: none !important;
    background-color: transparent;
  }
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
`;

const EditDeletePopperWrap = styled.div``;
const ViewIcon = styled.div``;

const iconStyles = makeStyles(() => ({
  root: {
    width: '100%',
  },
}));

const IamServiceAccountDashboard = () => {
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [iamServiceAccountClicked, setIamServiceAccountClicked] = useState(
    false
  );
  const [listItemDetails, setListItemDetails] = useState({});
  const [iamServiceAccountList, setIamServiceAccountList] = useState([]);
  const [response, setResponse] = useState({});
  const [
    selectedIamServiceAccountDetails,
    setSelectedIamServiceAccountDetails,
  ] = useState(null);
  const [viewAccountData, setViewAccountData] = useState({});
  const [viewDetails, setViewDetails] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [accountSecretData, setAccountSecretData] = useState({});
  const [permissionResponse, setPermissionResponse] = useState({
    status: 'loading',
  });
  const [secretResponse, setSecretResponse] = useState({
    status: '',
  });
  const [accountSecretError, setAccountSecretError] = useState('');
  const [disabledPermission, setDisabledPermission] = useState(true);
  const [accountMetaData, setAccountMetaData] = useState({
    response: {},
    error: '',
  });
  const [userDetails, setUserDetails] = useState([]);

  const [state, dispatch] = useStateValue();

  const listIconStyles = iconStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const history = useHistory();
  const location = useLocation();

  const introduction = Strings.Resources.iamServiceAccountDesc;

  const isTabScreen = useMediaQuery(mediaBreakpoints.medium);

  /**
   * @function fetchData
   * @description function call all the manage and safe api.
   */
  const fetchData = useCallback(async () => {
    setResponse({ status: 'loading' });
    setListItemDetails({});
    setInputSearchValue('');
    const serviceList = await apiService.getIamServiceAccountList();
    const iamServiceAccounts = await apiService.getIamServiceAccounts();
    const allApiResponse = Promise.all([serviceList, iamServiceAccounts]);
    allApiResponse
      .then((result) => {
        const listArray = [];
        if (result[0] && result[0]?.data?.iamsvcacc) {
          result[0].data.iamsvcacc.map((item) => {
            const svcName = Object.keys(item)[0].split('_');
            svcName.splice(0, 1);
            const data = {
              name: svcName.join('_'),
              iamAccountId: Object.keys(item)[0].split('_')[0],
              active: true,
              permission: Object.values(item)[0],
            };
            return listArray.push(data);
          });
        }
        if (result[1] && result[1].data?.keys) {
          result[1].data.keys.map((svcitem) => {
            if (!listArray.some((list) => list.name === svcitem.userName)) {
              const data = {
                name: svcitem.userName,
                iamAccountId: svcitem.accountID,
                active: false,
              };
              return listArray.push(data);
            }
            return null;
          });
        }
        setIamServiceAccountList([...listArray]);
        dispatch({
          type: 'GET_ALL_IAM_SERVICE_ACCOUNT_LIST',
          payload: [...listArray],
        });
        setResponse({ status: 'success' });
      })
      .catch(() => {
        setResponse({ status: 'failed' });
      });
  }, [dispatch]);

  /**
   * @description On component load call fetchData function.
   */
  useEffect(() => {
    fetchData().catch(() => {
      setResponse({ status: 'failed' });
    });
  }, [fetchData]);

  /**
   * @function onSearchChange
   * @description function to search input
   */
  const onSearchChange = (value) => {
    setInputSearchValue(value);
    if (value !== '') {
      const array = state?.iamServiceAccountList?.filter((item) => {
        return item?.name?.toLowerCase().includes(value?.toLowerCase().trim());
      });
      setIamServiceAccountList([...array]);
    } else {
      setIamServiceAccountList([...state?.iamServiceAccountList]);
    }
  };

  /**
   * @function onLinkClicked
   * @description function to check if mobile screen the make safeClicked true
   * based on that value display left and right side.
   */
  const onLinkClicked = () => {
    if (isMobileScreen) {
      setIamServiceAccountClicked(true);
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

  const onViewClicked = (svcname, account) => {
    setViewDetails(true);
    setViewAccountData(account);
    apiService
      .fetchIamServiceAccountDetails(svcname)
      .then((res) => {
        setSelectedIamServiceAccountDetails(res?.data);
      })
      .catch(() => {
        setResponseType(-1);
        setViewDetails(false);
      });
  };

  /**
   * @function backToIamServiceAccounts
   * @description To get back to left side lists in case of mobile view
   * @param {bool} isMobileScreen boolian
   */
  const backToIamServiceAccounts = () => {
    if (isMobileScreen) {
      setIamServiceAccountClicked(false);
    }
  };

  // Function to get the metadata of the given service account
  const fetchPermission = useCallback(async () => {
    setAccountMetaData({ response: {}, error: '' });
    setPermissionResponse({ status: 'loading' });
    if (listItemDetails?.active) {
      try {
        const res = await apiService.fetchIamServiceAccountDetails(
          `${listItemDetails?.iamAccountId}_${listItemDetails?.name}`
        );
        if (res?.data) {
          setPermissionResponse({ status: 'success' });
          setAccountMetaData({ response: res.data, error: '' });
          if (
            res.data.owner_ntid.toLowerCase() === state.username.toLowerCase()
          ) {
            setDisabledPermission(false);
            const eachUsersDetails = await getEachUsersDetails(res.data.users);
            if (eachUsersDetails !== null) {
              setUserDetails([...eachUsersDetails]);
            }
          } else {
            setDisabledPermission(true);
          }
        }
      } catch (err) {
        setPermissionResponse({ status: 'error' });
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setAccountSecretError(err?.response?.data?.errors[0]);
          setAccountMetaData({ response: {}, error: 'Something went wrong' });
        }
      }
    } else {
      setDisabledPermission(true);
    }
  }, [listItemDetails, state]);

  // Function to get the secret of the given service account.
  const getSecrets = useCallback(() => {
    setAccountSecretError('');
    if (listItemDetails.active) {
      setSecretResponse({ status: 'loading' });
      apiService
        .getIamSvcAccountSecrets(
          `${listItemDetails?.iamAccountId}_${listItemDetails?.name}`
        )
        .then(async (res) => {
          setSecretResponse({ status: 'success' });
          if (res?.data) {
            setAccountSecretData(res.data);
          }
          setAccountSecretError('');
        })
        .catch((err) => {
          if (err?.response?.data?.errors && err.response.data.errors[0]) {
            setAccountSecretError(err.response.data.errors[0]);
          }
          setSecretResponse({ status: 'error' });
        });
    }
  }, [listItemDetails]);

  useEffect(() => {
    if (iamServiceAccountList?.length > 0) {
      const val = location.pathname.split('/');
      const svcName = val[val.length - 1];
      const obj = iamServiceAccountList.find((svc) => svc.name === svcName);
      if (obj) {
        if (listItemDetails.name !== obj.name) {
          setListItemDetails({ ...obj });
          setAccountSecretData({});
        }
      } else {
        setListItemDetails(iamServiceAccountList[0]);
        history.push(`/iam-service-accounts/${iamServiceAccountList[0].name}`);
      }
    }
    // eslint-disable-next-line
  }, [iamServiceAccountList, location, history]);

  // toast close handler
  const onToastClose = () => {
    setResponseType(null);
  };

  const renderList = () => {
    return iamServiceAccountList.map((account) => (
      <ListFolderWrap
        key={account.name}
        to={{
          pathname: `/iam-service-accounts/${account.name}`,
          state: { data: account },
        }}
        onClick={() => onLinkClicked()}
        active={
          history.location.pathname === `/iam-service-accounts/${account.name}`
            ? 'true'
            : 'false'
        }
      >
        <ListItem
          title={account.name}
          subTitle={`IAM Account ID: ${account?.iamAccountId}`}
          icon={svcIcon}
          showActions={false}
          listIconStyles={listIconStyles}
        />
        <BorderLine />
        {(account.permission === 'write' || account.active === false) &&
        !isMobileScreen ? (
          <PopperWrap onClick={(e) => onActionClicked(e)}>
            <ViewIcon
              onClick={() =>
                onViewClicked(
                  `${account.iamAccountId}_${account.name}`,
                  account
                )
              }
            >
              <VisibilityIcon />
            </ViewIcon>
          </PopperWrap>
        ) : null}
        {isMobileScreen &&
          (account.permission === 'write' || account.active === false) && (
            <EditDeletePopperWrap onClick={(e) => onActionClicked(e)}>
              <ViewIcon
                onClick={() =>
                  onViewClicked(
                    `${account.iamAccountId}_${account.name}`,
                    account
                  )
                }
              >
                <VisibilityIcon />
              </ViewIcon>
            </EditDeletePopperWrap>
          )}
      </ListFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        <SectionPreview title="iam-service-account-section">
          <LeftColumnSection isAccountDetailsOpen={iamServiceAccountClicked}>
            <ColumnHeader>
              <div style={{ margin: '0 1rem' }}>
                <TitleOne extraCss={ListHeader}>
                  {`IAM Service Accounts (${iamServiceAccountList?.length})`}
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
            {response.status === 'failed' && (
              <EmptyContentBox>
                <Error description="Error while fetching service accounts!" />
              </EmptyContentBox>
            )}
            {response.status === 'success' && (
              <>
                {iamServiceAccountList && iamServiceAccountList.length > 0 ? (
                  <ListContainer>
                    <ListContent>{renderList()}</ListContent>
                  </ListContainer>
                ) : (
                  iamServiceAccountList?.length === 0 && (
                    <>
                      {inputSearchValue ? (
                        <NoDataWrapper>
                          No Iam Service Account found with name:
                          <strong>{inputSearchValue}</strong>
                        </NoDataWrapper>
                      ) : (
                        <NoDataWrapper>
                          <NoListWrap>
                            <NoData
                              imageSrc={NoSafesIcon}
                              description="No IAM Service Accounts enabled!"
                              actionButton={<></>}
                            />
                          </NoListWrap>
                        </NoDataWrapper>
                      )}
                    </>
                  )
                )}
              </>
            )}
          </LeftColumnSection>
          <RightColumnSection
            mobileViewStyles={isMobileScreen ? MobileViewForListDetailPage : ''}
            isAccountDetailsOpen={iamServiceAccountClicked}
          >
            <Switch>
              {iamServiceAccountList[0]?.name && (
                <Redirect
                  exact
                  from="/iam-service-accounts"
                  to={{
                    pathname: `/iam-service-accounts/${iamServiceAccountList[0]?.name}`,
                    state: { data: iamServiceAccountList[0] },
                  }}
                />
              )}
              <Route
                path="/iam-service-accounts/:iamServiceAccountName"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={backToIamServiceAccounts}
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
                        fetchPermission={fetchPermission}
                        getSecrets={getSecrets}
                        secretResponse={secretResponse}
                        permissionResponse={permissionResponse}
                        accountMetaData={accountMetaData}
                        accountSecretData={accountSecretData}
                        accountSecretError={accountSecretError}
                        disabledPermission={disabledPermission}
                        isIamSvcAccountActive={listItemDetails.active}
                        userDetails={userDetails}
                      />
                    }
                  />
                )}
              />

              <Route
                path="/iam-service-accounts"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={backToIamServiceAccounts}
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
                        fetchPermission={fetchPermission}
                        permissionResponse={permissionResponse}
                        getSecrets={getSecrets}
                        secretResponse={secretResponse}
                        accountMetaData={accountMetaData}
                        accountSecretData={accountSecretData}
                        accountSecretError={accountSecretError}
                        disabledPermission={disabledPermission}
                        isIamSvcAccountActive={listItemDetails.active}
                        userDetails={userDetails}
                      />
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
              message="Something went wrong!"
            />
          )}
          {responseType === 1 && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              message="Service account off-boarded successfully!"
            />
          )}
          {viewDetails ? (
            <ViewIamServiceAccount
              iamServiceAccountDetails={selectedIamServiceAccountDetails}
              open={viewDetails}
              viewAccountData={viewAccountData}
              setViewDetails={setViewDetails}
              refresh={fetchData}
            />
          ) : (
            <></>
          )}
        </SectionPreview>
      </>
    </ComponentError>
  );
};
IamServiceAccountDashboard.propTypes = {};
IamServiceAccountDashboard.defaultProps = {};

export default IamServiceAccountDashboard;
