import React from 'react';

import { Input, InputAdornment } from '@material-ui/core';
import MuiButton from 'components/common/MuiButton';
import styled from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import Dropdown from 'components/common/SelectDropdown';
import SelectionTabs from 'components/common/Tabs';

import FolderIcon from '@material-ui/icons/Folder';
import SearchIcon from '@material-ui/icons/Search';
import AddIcon from '@material-ui/icons/Add';

//mock data
import data from 'mockData/safeSectionMock.json';

//styled components
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

const SafeIconWrap = styled('div')`
  width: 20%;
  height: 100%;
  background: #4a4a4a;
  border-radious: 50%;
  align-items: center;
  display: flex;
`;
const SafeDescription = styled.p`
  font-size: 1.4em;
  text-align: left;
`;
const SafeFolderWrap = styled.div`
  display: flex;
  align-items: center;
  padding: 0.8em;
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
const SafeSectionWrap = () => {
  const { safes } = data;
  return (
    <SectionPreview title="safe-section">
      <ColumnSection>
        <ColumnHeader>
          <Dropdown />
          <MuiButton label="Create" icon={<AddIcon />} />
        </ColumnHeader>
        <SearchInput
          startAdornment={
            <InputAdornment position="start">
              <SearchIcon />
            </InputAdornment>
          }
        />
        <SafeListContainer>
          {safes.map((safe, index) => (
            <SafeFolderWrap key={index}>
              <SafeAvatarWrap>
                <Avatar>
                  <FolderIcon />
                </Avatar>
              </SafeAvatarWrap>
              <SafeDetailBox>
                <div>
                  {safe.name}
                  <span>{safe.flagType}</span>
                </div>
                <div>{safe.date}</div>
              </SafeDetailBox>
            </SafeFolderWrap>
          ))}
        </SafeListContainer>
      </ColumnSection>
      <ColumnSection>
        <ColumnHeader>
          <SafeIconWrap />
          <SafeDescription>
            Hello yhis is the sample description of thesafety used here. it
            shows description about safety type and so on.
          </SafeDescription>
        </ColumnHeader>
        <SelectionTabs />
      </ColumnSection>
    </SectionPreview>
  );
};

export default SafeSectionWrap;
