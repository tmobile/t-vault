/* eslint-disable no-unused-vars */
/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable no-param-reassign */
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
// import EditDeletePopper from '../EditDeletePopper';
import ListItem from '../../../../../components/ListItem';
import EditAndDeletePopup from '../../../../../components/EditAndDeletePopup';
import Error from '../../../../../components/Error';
import SnackbarComponent from '../../../../../components/Snackbar';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import apiService from '../../apiService';
import Strings from '../../../../../resources';
import { TitleOne } from '../../../../../styles/GlobalStyles';

import {
  ListContainer,
  ListContent,
} from '../../../../../styles/GlobalStyles/listingStyle';
import configData from '../../../../../config/config';

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

const EditDeletePopperWrap = styled.div``;

const iconStyles = makeStyles(() => ({
  root: {
    width: '100%',
  },
}));

const AzureDashboard = () => {
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [serviceAccountClicked, setServiceAccountClicked] = useState(false);
  const [listItemDetails, setListItemDetails] = useState({});
  const [azureList, setAzureList] = useState([]);
  const [toastResponse, setToastResponse] = useState(null);
  const [response, setResponse] = useState({ status: 'loading' });
  const [allAzureList, setAllAzureList] = useState([]);
  //   const listIconStyles = iconStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const isTabScreen = useMediaQuery(mediaBreakpoints.medium);
  const history = useHistory();
  //   const location = useLocation();
  const introduction = Strings.Resources.azurePrincipal;
  /**
   * @function fetchData
   * @description function call all the manage and azure principal api.
   */
  const fetchData = useCallback(async () => {
    setResponse({ status: 'loading' });
    setInputSearchValue('');
    setListItemDetails({});
    const manageAzureService = await apiService.getManageAzureService();
    const azureServiceList = await apiService.getAzureServiceList();
    const allApiResponse = Promise.all([manageAzureService, azureServiceList]);
    allApiResponse
      .then((result) => {
        setAzureList([]);
        setAllAzureList([]);
        setResponse({ status: 'success' });
      })
      .catch(() => {
        setResponse({ status: 'failed' });
      });
  }, []);

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
   * @description function to search azure principal.
   * @param {string} value searched input value.
   */
  const onSearchChange = (value) => {
    setInputSearchValue(value);
    if (value !== '') {
      const array = allAzureList?.filter((item) => {
        return String(item?.name?.toLowerCase()).startsWith(
          value?.toLowerCase().trim()
        );
      });
      setAzureList([...array]);
    } else {
      setAzureList([...allAzureList]);
    }
  };

  const renderList = () => {
    return azureList.map((account) => (
      <ListFolderWrap
        key={account.name}
        to={{
          pathname: `/azure-principal/${account.name}`,
          state: { data: account },
        }}
        active={
          history.location.pathname === `/azure-principal/${account.name}`
        }
      >
        {/* <ListItem
          title={account.name}
          subTitle={account.date}
          flag={account.type}
          icon={svcIcon}
          showActions={false}
          listIconStyles={listIconStyles}
        />
        <BorderLine /> */}
      </ListFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        <SectionPreview title="service-account-section">
          <LeftColumnSection isAccountDetailsOpen={serviceAccountClicked}>
            <ColumnHeader>
              <div style={{ margin: '0 1rem' }}>
                <TitleOne extraCss={ListHeader}>
                  {`Azure Principal (${azureList?.length})`}
                </TitleOne>
              </div>
              <SearchWrap>
                <TextFieldComponent
                  placeholder="Search"
                  icon="search"
                  fullWidth
                  value={inputSearchValue || ''}
                  color="secondary"
                  onChange={(e) => onSearchChange(e?.target?.value)}
                />
              </SearchWrap>
            </ColumnHeader>
            {response.status === 'loading' && (
              <ScaledLoader contentHeight="80%" contentWidth="100%" />
            )}
            {response.status === 'failed' && (
              <EmptyContentBox>
                <Error description="Error while fetching azure accounts!" />
              </EmptyContentBox>
            )}
            {response.status === 'success' && (
              <>
                {azureList.length > 0 ? (
                  <ListContainer>
                    <ListContent>{renderList()}</ListContent>
                  </ListContainer>
                ) : (
                  <>
                    {inputSearchValue ? (
                      <NoDataWrapper>
                        No azure account found with name:
                        <strong>{inputSearchValue}</strong>
                      </NoDataWrapper>
                    ) : (
                      <NoDataWrapper>
                        <NoListWrap>
                          <NoData
                            imageSrc={NoSafesIcon}
                            description="No azure principal are associated with you yet."
                          />
                        </NoListWrap>
                      </NoDataWrapper>
                    )}
                  </>
                )}
              </>
            )}
          </LeftColumnSection>
          <RightColumnSection
            mobileViewStyles={isMobileScreen ? MobileViewForListDetailPage : ''}
            isAccountDetailsOpen={serviceAccountClicked}
          >
            <Switch>
              {azureList[0]?.name && (
                <Redirect
                  exact
                  from="/azure-principal"
                  to={{
                    pathname: `/azure-principal/${azureList[0]?.name}`,
                    state: { data: azureList[0] },
                  }}
                />
              )}
              <Route
                path="/azure-principal/:azureName"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={() => {}}
                    ListDetailHeaderBg={
                      isTabScreen
                        ? tabSvcIcon
                        : isMobileScreen
                        ? mobSvcIcon
                        : sectionHeaderBg
                    }
                    description={introduction}
                    renderContent={<div />}
                  />
                )}
              />
              <Route
                path="/azure-principal"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={() => {}}
                    ListDetailHeaderBg={
                      isTabScreen
                        ? tabSvcIcon
                        : isMobileScreen
                        ? mobSvcIcon
                        : sectionHeaderBg
                    }
                    description={introduction}
                    renderContent={<div />}
                  />
                )}
              />
            </Switch>
          </RightColumnSection>
          {toastResponse === -1 && (
            <SnackbarComponent
              open
              onClose={() => {}}
              severity="error"
              icon="error"
              message="Something went wrong!"
            />
          )}
          {toastResponse === 1 && (
            <SnackbarComponent
              open
              onClose={() => {}}
              message="Service account off-boarded successfully!"
            />
          )}
        </SectionPreview>
      </>
    </ComponentError>
  );
};

export default AzureDashboard;
