/* eslint-disable react/jsx-props-no-spreading */
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import IamServiceAccountSecrets from '../IamServiceAccountSecrets';
import IamServiceAccountPermission from '../IamServiceAccountPermission';
// styled components goes here

const TabPanelWrap = styled.div`
  position: relative;
  height: 100%;
  margin: 0;
  padding-top: 1.3rem;
  ${mediaBreakpoints.small} {
    height: 77vh;
  }
`;

const TabContentsWrap = styled('div')`
  height: calc(100% - 4.8rem);
`;

const TabPanel = (props) => {
  const { children, value, index } = props;

  return (
    <TabPanelWrap
      role="tabpanel"
      hidden={value !== index}
      id={`safes-tabpanel-${index}`}
      aria-labelledby={`safe-tab-${index}`}
    >
      {children}
    </TabPanelWrap>
  );
};

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
};

TabPanel.defaultProps = {
  children: <div />,
};

function a11yProps(index) {
  return {
    id: `safety-tab-${index}`,
    'aria-controls': `safety-tabpanel-${index}`,
  };
}

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
    padding: '0 2.1rem',
    height: 'calc( 100% - 19.1rem )',
    display: 'flex',
    flexDirection: 'column',
    background: 'linear-gradient(to bottom,#151820,#2c3040)',
  },
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    height: '4.8rem',
    boxShadow: 'none',
    borderBottom: '0.3rem solid #222632',
    [theme.breakpoints.down('md')]: {
      height: 'auto',
    },
  },
  tab: {
    minWidth: '9.5rem',
  },
}));

const AccountSelectionTabs = (props) => {
  const {
    accountDetail,
    refresh,
    getSecrets,
    fetchPermission,
    isIamSvcAccountActive,
    accountSecretData,
    accountSecretError,
    disabledPermission,
    accountMetaData,
    status,
    userDetails,
  } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  useEffect(() => {
    if (accountDetail?.name) {
      fetchPermission();
      if (isIamSvcAccountActive && accountDetail?.permission !== 'deny') {
        getSecrets();
      }
    }
  }, [accountDetail, fetchPermission, getSecrets, isIamSvcAccountActive]);

  const accountName = accountDetail?.name;
  useEffect(() => {
    setValue(0);
  }, [accountName]);

  return (
    <ComponentError>
      <div className={classes.root}>
        <AppBar position="static" className={classes.appBar}>
          <Tabs
            value={value}
            onChange={handleChange}
            aria-label="safe tabs"
            indicatorColor="secondary"
            textColor="primary"
          >
            <Tab className={classes.tab} label="Secrets" {...a11yProps(0)} />
            {accountDetail?.name && !disabledPermission && (
              <Tab
                label="Permissions"
                {...a11yProps(1)}
                disabled={disabledPermission}
              />
            )}
          </Tabs>
        </AppBar>
        <TabContentsWrap>
          <TabPanel value={value} index={0}>
            <IamServiceAccountSecrets
              accountDetail={accountDetail}
              accountMetaData={accountMetaData}
              accountSecretData={accountSecretData}
              accountSecretError={accountSecretError}
              getSecrets={getSecrets}
              isIamSvcAccountActive={isIamSvcAccountActive}
              status={status}
            />
          </TabPanel>
          <TabPanel value={value} index={1}>
            <IamServiceAccountPermission
              accountDetail={accountDetail}
              accountMetaData={accountMetaData}
              parentStatus={status?.status}
              refresh={refresh}
              fetchPermission={fetchPermission}
              isIamSvcAccountActive={isIamSvcAccountActive}
              userDetails={userDetails}
            />
          </TabPanel>
        </TabContentsWrap>
      </div>
    </ComponentError>
  );
};
AccountSelectionTabs.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any),
  refresh: PropTypes.func.isRequired,
  fetchPermission: PropTypes.func.isRequired,
  getSecrets: PropTypes.func.isRequired,
  isIamSvcAccountActive: PropTypes.bool,
  accountMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  accountSecretData: PropTypes.objectOf(PropTypes.any),
  accountSecretError: PropTypes.string.isRequired,
  disabledPermission: PropTypes.bool.isRequired,
  status: PropTypes.objectOf(PropTypes.any).isRequired,
  userDetails: PropTypes.arrayOf(PropTypes.any).isRequired,
};
AccountSelectionTabs.defaultProps = {
  accountDetail: {},
  accountSecretData: {},
  isIamSvcAccountActive: false,
};

export default AccountSelectionTabs;
