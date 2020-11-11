/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable no-param-reassign */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import InfiniteScroll from 'react-infinite-scroller';
import { Link, Route, Switch, useHistory, Redirect } from 'react-router-dom';

import useMediaQuery from '@material-ui/core/useMediaQuery';
import { useStateValue } from '../../../../../contexts/globalState';
import sectionHeaderBg from '../../../../../assets/svc_banner_img.png';
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
import VisibilityIcon from '@material-ui/icons/Visibility';
import ViewIamServiceAccount from '../IamServiceAccountPreview';
import apiService from '../../apiService';
import Strings from '../../../../../resources';
import { TitleOne } from '../../../../../styles/GlobalStyles';
import AccountSelectionTabs from '../IamSvcAccountTabs';

// const OnBoardForm = lazy(() => import('../../OnBoardForm'));

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
  max-height: 61vh;
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
  const [moreData] = useState(false);
  const [isLoading] = useState(false);
  const [iamServiceAccountList, setIamServiceAccountList] = useState([]);
  const [status, setStatus] = useState({});
  const [getResponse, setGetResponse] = useState(null);
  //   const [allIamServiceAccountList, setAllIamServiceAccountList] = useState([]);
  const [
    selectedIamServiceAccountDetails,
    setSelectedIamServiceAccountDetails,
  ] = useState(null);
  const [viewDetails, setViewDetails] = useState(false);

  const [state] = useStateValue();
  let scrollParentRef = null;
  // const classes = useStyles();
  const listIconStyles = iconStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const history = useHistory();
  //   const location = useLocation();

  const introduction = Strings.Resources.iamServiceAccountDesc;

  /**
   * @function fetchData
   * @description function call all the manage and safe api.
   */
  const fetchData = useCallback(async () => {
    setStatus({ status: 'loading', message: 'Loading...' });
    setInputSearchValue('');
    const serviceList = await apiService.getIamServiceAccountList();
    const iamServiceAccounts = await apiService.getIamServiceAccounts();
    const allApiResponse = Promise.all([serviceList, iamServiceAccounts]);
    allApiResponse
      .then((response) => {
        const listArray = [];
        if (response[0] && response[0].data && response[0].data.iamsvcacc) {
          response[0].data.iamsvcacc.map((item) => {
            const svcName = Object.keys(item)[0].split('_');
            svcName.splice(0, 1);
            const data = {
              name: svcName.join('_'),
              iamAccountId: Object.keys(item)[0].split('_')[0],
              active: true,
            };
            return listArray.push(data);
          });
        }
        if (response[1] && response[1].data?.keys) {
          response[1].data.keys.map((svcitem) => {
            if (!listArray.some((list) => list.name === svcitem.userName)) {
              const data = {
                name: svcitem.userName,
                iamAccountId: svcitem.accountID,
                active: false,
              };
              return listArray.push(data);
            }
          });
        }
        setIamServiceAccountList([...listArray]);
        console.log('listArray', listArray);
        setStatus({});
        setGetResponse(1);
      })
      .catch((err) => {
        setStatus({ status: 'failed', message: 'failed' });
        setGetResponse(-1);
      });
  }, []);

  /**
   * @description On component load call fetchData function.
   */
  useEffect(() => {
    fetchData().catch(() => {
      setStatus({ status: 'failed', message: 'failed' });
    });
  }, [fetchData]);

  /**
   * @function onSearchChange
   * @description function to search input
   */
  const onSearchChange = (value) => {
    setInputSearchValue(value);
    if (value !== '') {
      const array = state?.iamServiceAccountList.filter((item) => {
        return String(item.name).startsWith(value);
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
  const onLinkClicked = (item) => {
    setListItemDetails(item);
    if (isMobileScreen) {
      setIamServiceAccountClicked(true);
    }
  };

  /**
   * @function onActionClicked
   * @description function to prevent default click.
   * @param {object} e event
   */
  const onActionClicked = (e, svcname) => {
    e.stopPropagation();
    e.preventDefault();
  };

  const onViewClicked = (e, svcname) => {
    setViewDetails(true);
    apiService
      .fetchIamServiceAccountDetails(svcname)
      .then((res) => {
        setGetResponse(1);
        setSelectedIamServiceAccountDetails(res?.data);
      })
      .catch((err) => {
        setGetResponse(-1);
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

  useEffect(() => {
    if (iamServiceAccountList?.length > 0) {
      iamServiceAccountList.map((item) => {
        if (
          history.location.pathname === `/iam-service-accounts/${item.name}`
        ) {
          return setListItemDetails(item);
        }
        return null;
      });
    }
  }, [iamServiceAccountList, listItemDetails, history]);

  // Infine scroll load more data
  const loadMoreData = () => {};

  // toast close handler
  const onToastClose = () => {
    setStatus({});
  };

  const renderList = () => {
    return iamServiceAccountList.map((account) => (
      <ListFolderWrap
        key={account.name}
        to={{
          pathname: `/iam-service-accounts/${account.name}`,
          state: { data: account },
        }}
        onClick={() => onLinkClicked(account)}
        active={
          history.location.pathname === `/iam-service-accounts/${account.name}`
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
        {account.name && !isMobileScreen ? (
          <PopperWrap onClick={(e) => onActionClicked(e)}>
            <ViewIcon
              onClick={(e) =>
                onViewClicked(e, `${account.iamAccountId}_${account.name}`)
              }
            >
              {' '}
              <VisibilityIcon />
            </ViewIcon>
          </PopperWrap>
        ) : null}
        {isMobileScreen && account.name && (
          <EditDeletePopperWrap onClick={(e) => onActionClicked(e)}>
            {' '}
            <ViewIcon
              onClick={(e) =>
                onViewClicked(e, `${account.iamAccountId}_${account.name}`)
              }
            >
              {' '}
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
            {status.status === 'loading' && (
              <ScaledLoader contentHeight="80%" contentWidth="100%" />
            )}
            {getResponse === -1 && !iamServiceAccountList?.length && (
              <EmptyContentBox>
                {' '}
                <Error description="Error while fetching service accounts!" />
              </EmptyContentBox>
            )}

            {getResponse === 1 && (
              <>
                {iamServiceAccountList && iamServiceAccountList.length > 0 ? (
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
                  iamServiceAccountList?.length === 0 &&
                  getResponse === 1 && (
                    <>
                      {' '}
                      {inputSearchValue ? (
                        <NoDataWrapper>
                          No Iam Service Account found with name:
                          <strong>{inputSearchValue}</strong>
                        </NoDataWrapper>
                      ) : (
                        <NoDataWrapper>
                          {' '}
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
                    ListDetailHeaderBg={sectionHeaderBg}
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
                path="/iam-service-accounts"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={backToIamServiceAccounts}
                    ListDetailHeaderBg={sectionHeaderBg}
                    description={introduction}
                  />
                )}
              />
            </Switch>
          </RightColumnSection>
          {(status.status === 'success') === 'failed' && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              severity="error"
              icon="error"
              message="Something went wrong!"
            />
          )}
          {status.status === 'success' && (
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
