import React, { useState } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles, withStyles } from '@material-ui/core/styles';
import InfiniteScroll from 'react-infinite-scroller';
import PropTypes from 'prop-types';
import { Link, Route, Switch, Redirect } from 'react-router-dom';

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
import ListItem from '../../../../../components/ListItem';
import PsudoPopper from '../../../../../components/PsudoPopper';
import Error from '../../../../../components/Error';

import apiService from '../../apiService';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';

import ConfirmationModal from '../../../../../components/ConfirmationModal';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import { IconEdit, IconDeleteActive } from '../../../../../assets/SvgIcons';
import { TitleOne } from '../../../../../styles/GlobalStyles';

const ColumnSection = styled('section')`
  position: relative;
  background: ${(props) => props.backgroundColor || '#151820'};
`;

const RightColumnSection = styled(ColumnSection)`
  width: 59.23%;
  padding: 0;
  background: linear-gradient(to bottom, #151820, #2c3040);
  ${mediaBreakpoints.small} {
    width: 100%;
    display: none;
    ${(props) => props.mobileViewStyles}
  }
`;
const LeftColumnSection = styled(ColumnSection)`
  width: 40.77%;
  ${mediaBreakpoints.small} {
    display: ${(props) => (props.isRightActive ? 'none' : 'block')};
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
const ListFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  text-decoration: none;
  align-items: center;
  flex-direction: column;
  padding: 1.2rem 1.8rem 1.2rem 3.4rem;
  cursor: pointer;
  background-image: ${(props) =>
    props.active ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active ? '#fff' : '#4a4a4a')};
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
    color: #fff;
  }
`;
const PopperWrap = styled.div`
  position: absolute;
  top: 50%;
  right: 0%;
  z-index: 1;
  width: 5.5rem;
  transform: translate(-50%, -50%);
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
const useStyles = makeStyles(() => ({
  containedSecondary: { borderRadius: '0.4rem' },
}));
const ServiceAccountDashboard = (props) => {
  const {
    leftColumLists,
    moreData,
    status,
    activeFolders,
    loadMoreData,
    setActiveFolders,
    isLoading,
  } = props;
  const [onBoardForm, setOnBoardForm] = useState(false);
  const [inputSearchValue, setInputSearchValue] = useState('');
  let scrollParentRef = null;
  const classes = useStyles();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  const showOnBoardForm = () => {
    setOnBoardForm(true);
  };
  const onSearchChange = (value) => {
    setInputSearchValue(value);
  };
  const renderList = () => {
    return leftColumLists.map((account) => (
      <ListFolderWrap
        key={account.name}
        // to={{
        //   pathname: `${routeProps.match.url}/${account.name}`,
        //   state: { account },
        // }}
        // active={
        //   activeFolders.includes(account.name) ||
        //   routeProps.location.pathname.includes(account.name)
        // }
        // onMouseLeave={() => setactiveFolders([])}
        // onClick={() => showSafeDetails(account.name, account)}
        // onMouseEnter={() => showSafeDetails(account.name, account)}
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
        {activeFolders.includes(account.name) && account.manage ? (
          <PopperWrap>
            <PsudoPopper
              //   onDeleteListItemClicked={() => onDeleteSafeClicked(safe.path)}
              item={account}
            />
          </PopperWrap>
        ) : null}
      </ListFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        {/* <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleClose}
          title="Are you sure you want to delete this service account?"
          cancelButton={
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleClose()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
          confirmButton={
            <ButtonComponent
              label="Confirm"
              color="secondary"
              onClick={() => onDeleteSafeConfirmClicked()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        /> */}
        <SectionPreview title="service-account-section">
          <LeftColumnSection>
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
            {status.status === 'failed' && !leftColumLists?.length && (
              <EmptyContentBox>
                {' '}
                <Error description="Error while fetching service accounts!" />
              </EmptyContentBox>
            )}
            {leftColumLists && leftColumLists.length > 0 ? (
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
                  loader={!isLoading ? <div key={0}>Loading...</div> : <></>}
                  useWindow={false}
                  getScrollParent={() => scrollParentRef}
                >
                  {renderList()}
                </StyledInfiniteScroll>
              </ListContainer>
            ) : (
              leftColumLists?.length === 0 && (
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
            {leftColumLists?.length ? (
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
            mobileViewStyles={
              activeFolders?.length && isMobileScreen
                ? MobileViewForListDetailPage
                : ''
            }
          >
            <Switch>
              {' '}
              {leftColumLists[0]?.name && (
                <Redirect
                  exact
                  from="/service-account"
                  to={{
                    pathname: `/service-account/${leftColumLists[0]?.name}`,
                    state: { serviceAccount: leftColumLists[0] },
                  }}
                />
              )}
              <Route
                path="/:tab/:serviceAccountName"
                render={(routerProps) => (
                  <ListItemDetail
                    detailData={leftColumLists}
                    params={routerProps}
                    setActiveFolders={() => setActiveFolders([])}
                    ListDetailHeaderBg={sectionHeaderBg}
                  />
                )}
              />
              <Route
                path="/"
                render={(routerProps) => (
                  <ListItemDetail
                    detailData={leftColumLists}
                    params={routerProps}
                    ListDetailHeaderBg={sectionHeaderBg}
                    setActiveFolders={() => setActiveFolders([])}
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
ServiceAccountDashboard.propTypes = {
  leftColumLists: PropTypes.arrayOf(PropTypes.array),
  moreData: PropTypes.bool,
  status: PropTypes.objectOf(PropTypes.object),
  activeFolders: PropTypes.arrayOf(PropTypes.array),
  loadMoreData: PropTypes.func,
  setActiveFolders: PropTypes.func,
  isLoading: PropTypes.bool,
};
ServiceAccountDashboard.defaultProps = {
  leftColumLists: [],
  moreData: false,
  status: {},
  activeFolders: [],
  loadMoreData: PropTypes.func,
  setActiveFolders: () => {},
  isLoading: false,
};

export default ServiceAccountDashboard;
