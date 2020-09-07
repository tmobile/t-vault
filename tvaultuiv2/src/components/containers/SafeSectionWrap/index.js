/* eslint-disable no-return-assign */
/* eslint-disable import/no-unresolved */
import React, { useState, useEffect } from 'react';
import InfiniteScroll from 'react-infinite-scroller';
import PropTypes from 'prop-types';
import { Link, Route, Switch } from 'react-router-dom';
import { Input, InputAdornment } from '@material-ui/core';
import PsudoPopper from 'components/common/PsudoPopper';
import SafeDetails from 'components/containers/SafeDetails';
import styled from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import Dropdown from 'components/common/SelectDropdown';
import FolderIcon from '@material-ui/icons/Folder';
import SearchIcon from '@material-ui/icons/Search';
import FloatingButtonComponent from 'components/FormFields/FloatingButton';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

// mock data
import { safes, safeDetail } from './safeSectionMock.json';

// styled components
const ColumnSection = styled('section')`
  width: 50%;
  padding: 2.5em;
  border-right: 2px solid #ddd;
  position: relative;
  &:last-child {
    border-right: none;
  }
`;
const SectionPreview = styled('main')`
  display: flex;
`;
const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  padding: 0.5em;
  justify-content: space-between;
`;
const SearchInput = styled(Input)`
  padding: 0.25em 0.5em;
  background-color: #f2f2f2;
  padding: 0.25em 0.5em;
  width: 100%;
  outline: none;
  margin: 0 auto;
  border: none;
  height: 3em;
  margin: 0 auto;
  margin-bottom: 1em;
  &.MuiInput-underline:before,
  .MuiInput-underline:hover:before {
    border-bottom: none;
  }
`;
const SafeListContainer = styled.div`
  height: 20rem;
  overflow: auto;
`;

const SafeFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  text-decoration: none;
  color: #4a4a4a;
  align-items: center;
  padding: 0.8em;
  cursor: pointer;
  background-color: ${(props) => (props.active ? '#4a4a4a' : '#fff')};
  background-image: ${(props) =>
    props.active ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active ? '#fff' : '#4a4a4a')};
  :hover {
    background-color: #4a4a4a;
    color: #fff;
  }
`;
const SafeDetailBox = styled('div')`
  padding-left: 1em;
`;
const SafeAvatarWrap = styled.div`
  width: 4em;
  height: 4em;
`;
const SafeName = styled.div`
  font-size: 1.25rem;
`;
const Flag = styled('span')`
  opacity: 0.7;
  margin-left: 0.5rem;
  font-size: ${(props) => props.fontSize};
  font-style: ${(props) => (props.fontStyle ? props.fontStyle : '')};
`;
const PopperWrap = styled.div`
  position: absolute;
  top: 50%;
  right: 0%;
  z-index: 2;
  transform: translate(-50%, -50%);
`;
const SafeSectionWrap = (props) => {
  const [safeList, setSafeList] = useState([]);
  const [moreData, setMoreData] = useState(false);

  const { routeProps } = props;
  const [activeSafeFolders, setActiveSafeFolders] = useState([]);
  // const [showPopper, setShowPopper] = useState(false);

  /**
   * safe detail page route change handling function
   * @param {string}
   * @param {object}
   */
  const showSafeDetails = (active) => {
    const activeSafes = [];
    activeSafes.push(active);
    setActiveSafeFolders([...activeSafes]);
  };

  /**
   * renders safe details page route
   * @param {string}
   * @param {object}
   */

  useEffect(() => {
    safes.map((item) => {
      return setSafeList((prev) => [...prev, item]);
    });
    setMoreData(true);
  }, []);

  const getSafesList = () => {
    return new Promise((resolve) =>
      setTimeout(() => {
        resolve({
          name: `safe-${Math.ceil(Math.random() * 100)}`,
          desc:
            'Hello yhis is the sample description of thesafety used here. it shows description about safety type and so on',
          date: '2 days ago , 9:20 pm',
          flagType: 'new',
        });
      }, 1000)
    );
  };

  const loadMoreData = () => {
    getSafesList().then((res) => {
      // setMoreData(false);
      setSafeList((prev) => [...prev, res]);
    });
  };

  let scrollParentRef = null;
  const renderSafes = () => {
    return safeList.map((safe) => (
      <SafeFolderWrap
        key={safe.name}
        to={`${routeProps.match.url}/${safe.name}`}
        active={activeSafeFolders.includes(safe.name)}
        onClick={() => showSafeDetails(safe.name)}
      >
        <SafeAvatarWrap>
          <Avatar>
            <FolderIcon />
          </Avatar>
        </SafeAvatarWrap>
        <SafeDetailBox>
          <SafeName>
            {safe.name}
            <Flag fontSize="0.85rem" fontStyle="italic">
              {safe.flagType}
            </Flag>
          </SafeName>
          <Flag fontSize="1rem">{safe.date}</Flag>
        </SafeDetailBox>
        {activeSafeFolders.includes(safe.name) ? (
          <PopperWrap>
            <PsudoPopper />
          </PopperWrap>
        ) : null}
      </SafeFolderWrap>
    ));
  };
  const onCreateSafeClicked = () => {
    routeProps.history.push('/safe/create-safe');
  };

  return (
    <ComponentError>
      <SectionPreview title="safe-section">
        <ColumnSection>
          <ColumnHeader>
            <Dropdown />
            <FloatingButtonComponent
              onClick={() => onCreateSafeClicked()}
              type="float"
            />
          </ColumnHeader>
          <SearchInput
            startAdornment={
              // eslint-disable-next-line react/jsx-wrap-multilines
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            }
          />
          <SafeListContainer ref={(ref) => (scrollParentRef = ref)}>
            <InfiniteScroll
              pageStart={0}
              loadMore={() => {
                console.log('Load more data called---');
                loadMoreData();
              }}
              hasMore={moreData}
              threshold={100}
              loader={<div key={0}>Loading...</div>}
              useWindow={false}
              getScrollParent={() => scrollParentRef}
            >
              {renderSafes()}
            </InfiniteScroll>
          </SafeListContainer>
        </ColumnSection>

        <ColumnSection>
          <Switch>
            {' '}
            <Route
              path="/:tab/:safeName"
              render={(routerProps) => (
                <SafeDetails detailData={safeDetail} params={routerProps} />
              )}
            />
          </Switch>
        </ColumnSection>
      </SectionPreview>
    </ComponentError>
  );
};

SafeSectionWrap.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  routeProps: PropTypes.object,
};
SafeSectionWrap.defaultProps = {
  routeProps: {},
};

export default SafeSectionWrap;
