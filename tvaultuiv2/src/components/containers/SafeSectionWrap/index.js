/* eslint-disable import/no-unresolved */
import React, { Suspense, useState } from 'react';
import PropTypes from 'prop-types';
import { Link, Route, Switch } from 'react-router-dom';
import { Input, InputAdornment } from '@material-ui/core';
import MuiButton from 'components/common/MuiButton';
import PsudoPopper from 'components/common/PsudoPopper';
import SafeDetails from 'components/containers/SafeDetails';
import styled from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import Dropdown from 'components/common/SelectDropdown';

import FolderIcon from '@material-ui/icons/Folder';
import SearchIcon from '@material-ui/icons/Search';
import AddIcon from '@material-ui/icons/Add';

// mock data
import { safes, safeDetail } from 'mockData/safeSectionMock.json';

// styled components
const ColumnSection = styled('section')`
  width: 50%;
  padding: 2.5em;
  border-right: 2px solid #ddd;
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
const SafeListContainer = styled.div``;

const SafeFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  text-decoration: none;
  color: #4a4a4a;
  align-items: center;
  padding: 0.8em;
  cursor: pointer;
  background-color: ${(props) => (props.active ? '#4a4a4a' : '#fff')};
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
  // const renderLayout = () => {
  //   return (
  //     <Route
  //       exact
  //       path={`${routeProps.match.url}`}
  //       render={() => <SafeDetails />}
  //     />
  //   );
  // };
  return (
    <SectionPreview title="safe-section">
      <ColumnSection>
        <ColumnHeader>
          <Dropdown />
          <MuiButton label="Create" icon={<AddIcon />} />
        </ColumnHeader>
        <SearchInput
          startAdornment={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <InputAdornment position="start">
              <SearchIcon />
            </InputAdornment>
          }
        />
        <SafeListContainer>
          {safes.map((safe) => (
            // eslint-disable-next-line react/no-array-index-key
            <SafeFolderWrap
              to={`${routeProps.match.url}/${safe.name}`}
              active={activeSafeFolders.includes(safe.name)}
              key={safe.name}
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
          ))}
        </SafeListContainer>
      </ColumnSection>

      <ColumnSection>
        <Suspense fallback={<div>Loading...</div>}>
          <Switch>
            {' '}
            <Route
              path="/:tab/:safename"
              render={(routerProps) => (
                <SafeDetails detailData={safeDetail} params={routerProps} />
              )}
            />
          </Switch>
        </Suspense>
      </ColumnSection>
    </SectionPreview>
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
