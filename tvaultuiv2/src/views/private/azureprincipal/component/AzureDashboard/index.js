/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable no-nested-ternary */
/* eslint-disable prefer-destructuring */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import VisibilityIcon from '@material-ui/icons/Visibility';
import {
  Link,
  Route,
  Switch,
  useHistory,
  Redirect,
  useLocation,
} from 'react-router-dom';

import useMediaQuery from '@material-ui/core/useMediaQuery';
import sectionHeaderBg from '../../../../../assets/azure-banner.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import NoSafesIcon from '../../../../../assets/no-data-safes.svg';
import azureIcon from '../../../../../assets/azure.svg';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ListItemDetail from '../../../../../components/ListItemDetail';
import Error from '../../../../../components/Error';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import apiService from '../../apiService';
import Strings from '../../../../../resources';
import { TitleOne } from '../../../../../styles/GlobalStyles';

import {
  ListContainer,
  ListContent,
} from '../../../../../styles/GlobalStyles/listingStyle';
import AzureListItem from '../AzureListItem';
import ViewAzure from '../ViewAzure';
import AzureSelectionTabs from '../AzureSelectionTabs';

const ColumnSection = styled('section')`
  position: relative;
  background: ${(props) => props.backgroundColor || '#151820'};
`;

const RightColumnSection = styled(ColumnSection)`
  width: 59.23%;
  padding: 0;
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

const ViewIcon = styled.div``;

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

const AzureDashboard = () => {
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [azureAccountClicked, setAzureAccountClicked] = useState(false);
  const [listItemDetails, setListItemDetails] = useState({});
  const [azureList, setAzureList] = useState([]);
  const [response, setResponse] = useState({ status: 'loading' });
  const [allAzureList, setAllAzureList] = useState([]);
  const [openViewAzureModal, setOpenViewAzureModal] = useState(false);
  const [viewAzureData, setViewAzureData] = useState({});
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const isTabScreen = useMediaQuery(mediaBreakpoints.medium);
  const history = useHistory();
  const location = useLocation();
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
        const manageArray = [];
        if (result[0] && result[0].data?.keys?.length > 0) {
          result[0].data.keys.map((item) => {
            return manageArray.push({
              access: 'N/A',
              name: item,
              isManagable: true,
            });
          });
        }
        if (result[1] && result[1].data?.azuresvcacc?.length > 0) {
          result[1].data.azuresvcacc.map((item) => {
            const obj = manageArray.find(
              (ele) =>
                ele.name.toLowerCase() === Object.keys(item)[0].toLowerCase()
            );
            if (obj) {
              obj.access = Object.values(item)[0];
            } else {
              manageArray.push({
                access: Object.values(item)[0],
                name: Object.keys(item)[0],
                isManagable: false,
              });
            }
            return null;
          });
        }
        setAzureList([...manageArray]);
        setAllAzureList([...manageArray]);
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

  useEffect(() => {
    if (allAzureList.length > 0) {
      const val = location.pathname.split('/');
      const azureName = val[val.length - 1];
      const obj = allAzureList.find((azure) => azure.name === azureName);
      if (obj) {
        setListItemDetails({ ...obj });
      } else {
        setListItemDetails(allAzureList[0]);
        history.push(`/azure-principal/${allAzureList[0].name}`);
      }
    }
  }, [allAzureList, location, history]);

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

  /**
   * @function onActionClicked
   * @description function to prevent default click.
   * @param {object} e event
   */
  const onActionClicked = (e) => {
    e.stopPropagation();
    e.preventDefault();
  };

  const onViewClicked = (azure) => {
    setOpenViewAzureModal(true);
    setViewAzureData(azure);
  };

  /**
   * @function onLinkClicked
   * @description function to check if mobile screen the make azureClicked true
   * based on that value display left and right side.
   */
  const onLinkClicked = () => {
    if (isMobileScreen) {
      setAzureAccountClicked(true);
    }
  };

  const onCloseViewAzureModal = (action) => {
    if (action) {
      fetchData();
    }
    setOpenViewAzureModal(false);
    setViewAzureData({});
  };

  const renderList = () => {
    return azureList.map((azure) => (
      <ListFolderWrap
        key={azure.name}
        to={`/azure-principal/${azure.name}`}
        onClick={() => onLinkClicked()}
        active={
          history.location.pathname === `/azure-principal/${azure.name}`
            ? 'true'
            : 'false'
        }
      >
        <AzureListItem title={azure.name} icon={azureIcon} />
        <BorderLine />
        {(azure.isManagable || azure.access === 'write') && !isMobileScreen && (
          <PopperWrap onClick={(e) => onActionClicked(e)}>
            <ViewIcon onClick={() => onViewClicked(azure)}>
              <VisibilityIcon />
            </ViewIcon>
          </PopperWrap>
        )}
        {(azure.isManagable || azure.access === 'write') && isMobileScreen && (
          <EditDeletePopperWrap onClick={(e) => onActionClicked(e)}>
            <ViewIcon onClick={() => onViewClicked(azure)}>
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
        <SectionPreview title="azure-account-section">
          {openViewAzureModal && (
            <ViewAzure
              open={openViewAzureModal}
              viewAzureData={viewAzureData}
              onCloseViewAzureModal={(action) => onCloseViewAzureModal(action)}
            />
          )}
          <LeftColumnSection isAccountDetailsOpen={azureAccountClicked}>
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
                            customStyle={customStyle}
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
            isAccountDetailsOpen={azureAccountClicked}
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
                    backToLists={() => setAzureAccountClicked(false)}
                    ListDetailHeaderBg={
                      isTabScreen
                        ? sectionHeaderBg
                        : isMobileScreen
                        ? sectionHeaderBg
                        : sectionHeaderBg
                    }
                    description={introduction}
                    renderContent={
                      <AzureSelectionTabs
                        azureDetail={listItemDetails}
                        refresh={() => fetchData()}
                      />
                    }
                  />
                )}
              />
              <Route
                path="/azure-principal"
                render={(routerProps) => (
                  <ListItemDetail
                    listItemDetails={listItemDetails}
                    params={routerProps}
                    backToLists={() => setAzureAccountClicked(false)}
                    ListDetailHeaderBg={
                      isTabScreen
                        ? sectionHeaderBg
                        : isMobileScreen
                        ? sectionHeaderBg
                        : sectionHeaderBg
                    }
                    description={introduction}
                    renderContent={
                      <AzureSelectionTabs
                        azureDetail={listItemDetails}
                        refresh={() => fetchData()}
                      />
                    }
                  />
                )}
              />
            </Switch>
          </RightColumnSection>
        </SectionPreview>
      </>
    </ComponentError>
  );
};

export default AzureDashboard;
