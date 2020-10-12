/* eslint-disable react/jsx-props-no-spreading */
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import Tab from '@material-ui/core/Tab';
import PropTypes from 'prop-types';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import NamedButton from '../NamedButton';
import permissionPlusIcon from '../../assets/permission-plus.svg';
import mediaBreakpoints from '../../breakpoints';

const { belowLarge } = mediaBreakpoints;

function a11yProps(index) {
  return {
    id: `scrollable-prevent-tab-${index}`,
    'aria-controls': `scrollable-prevent-tabpanel-${index}`,
  };
}

const CountPlusWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 2rem;
  height: 3.8rem;
`;
const CountSpan = styled.div`
  color: #5e627c;
  font-size: 1.3rem;
`;

const customStyles = css`
  display: flex;
  ${belowLarge} {
    display: none;
  }
`;

const customMobileStyles = css`
  display: none;
  ${belowLarge} {
    display: flex;
  }
`;

const useStyles = makeStyles(() => ({
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    height: '3.7rem',
  },
}));

const PermissionsTabs = (props) => {
  const { onAddLabelBtnClicked, count, onTabChange, value } = props;
  const classes = useStyles();
  const [tabData] = useState({
    0: 'User',
    1: 'Group',
    2: 'AWS Application',
    3: 'App Role',
  });
  const [selectedTab, setSelectedTab] = useState('User');

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const handleChange = (event, newValue) => {
    onTabChange(newValue);
    Object.entries(tabData).map(([key, data]) => {
      if (key === newValue.toString()) {
        return setSelectedTab(data);
      }
      return null;
    });
  };

  useEffect(() => {
    handleChange('', value);
  }, [value, handleChange]);

  return (
    <ComponentError>
      <>
        <CountPlusWrapper>
          <CountSpan color="#5e627c">{`${count} ${selectedTab}s`}</CountSpan>
          <NamedButton
            customStyle={customMobileStyles}
            label={`Add ${selectedTab}`}
            iconSrc={permissionPlusIcon}
            onClick={onAddLabelBtnClicked}
          />
        </CountPlusWrapper>
        <AppBar position="static" className={classes.appBar}>
          <Tabs
            value={value}
            onChange={handleChange}
            variant="scrollable"
            scrollButtons="off"
            aria-label="scrollable prevent tabs example"
          >
            <Tab label="Users" {...a11yProps(0)} />
            <Tab label="Groups" {...a11yProps(1)} />
            <Tab label="AWS Applications" {...a11yProps(2)} />
            <Tab label="App Roles" {...a11yProps(3)} />
          </Tabs>
          <NamedButton
            customStyle={customStyles}
            label={`Add ${selectedTab}`}
            iconSrc={permissionPlusIcon}
            onClick={onAddLabelBtnClicked}
          />
        </AppBar>
      </>
    </ComponentError>
  );
};

PermissionsTabs.propTypes = {
  onAddLabelBtnClicked: PropTypes.func,
  onTabChange: PropTypes.func,
  value: PropTypes.number,
  count: PropTypes.number,
};

PermissionsTabs.defaultProps = {
  value: 0,
  count: 0,
  onTabChange: () => {},
  onAddLabelBtnClicked: () => {},
};

export default PermissionsTabs;
