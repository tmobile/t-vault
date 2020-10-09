/* eslint-disable react/jsx-props-no-spreading */
import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import ServiceAccountSecrets from '../ServiceAccountSecrets';
import ServiceAccountPermission from '../ServiceAccountPermission';
import { useStateValue } from '../../../../../contexts/globalState';
import apiService from '../../apiService';
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
  const { accountDetail, refresh } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [response, setResponse] = useState({ status: 'loading' });
  const [accountSecretData, setAccountSecretData] = useState({});
  const [accountSecretError, setAccountSecretError] = useState('');
  const [hasSvcAccountAcitve, setHasSvcAccountAcitve] = useState(false);
  const [disabledPermission, setDisabledPermission] = useState(false);
  const [secretResStatus, setSecretResStatus] = useState({ status: 'loading' });
  const [accountMetaData, setAccountMetaData] = useState({
    response: {},
    error: '',
  });
  const [state] = useStateValue();

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  // Function to get the secret of the given service account.
  const getSecrets = useCallback(() => {
    setSecretResStatus({ status: 'loading' });
    if (accountDetail.access !== '') {
      apiService
        .getServiceAccountPassword(accountDetail?.name)
        .then((res) => {
          setSecretResStatus({ status: 'success' });
          if (res?.data) {
            setAccountSecretData(res.data);
          }
        })
        .catch((err) => {
          if (
            err?.response &&
            err.response.data?.errors &&
            err.response.data.errors[0]
          ) {
            setAccountSecretError(err.response.data.errors[0]);
          }
          setSecretResStatus({ status: 'error' });
        });
    } else {
      setSecretResStatus({ status: 'no-permission' });
    }
  }, [accountDetail]);

  // Function to get the metadata of the given service account
  const fetchPermission = useCallback(() => {
    setResponse({ status: 'loading' });
    apiService
      .updateMetaPath(accountDetail.name)
      .then((res) => {
        if (res.data && res.data.data) {
          setResponse({ status: 'success' });
          if (res.data.data.managedBy === state.username) {
            setDisabledPermission(false);
            if (res.data.data.initialPasswordReset) {
              setHasSvcAccountAcitve(true);
              setAccountMetaData({ response: { ...res.data.data }, error: '' });
            } else {
              setHasSvcAccountAcitve(false);
            }
          } else {
            setValue(0);
            setDisabledPermission(true);
          }
        }
      })
      .catch(() => {
        setResponse({ status: 'error' });
        setAccountMetaData({ response: {}, error: 'Something went wrong' });
      });
  }, [accountDetail, state]);

  useEffect(() => {
    setResponse({ status: 'loading' });
    setHasSvcAccountAcitve(false);
    if (accountDetail?.name) {
      fetchPermission();
      getSecrets();
    }
  }, [accountDetail, fetchPermission, getSecrets]);

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
            <Tab
              label="Permissions"
              {...a11yProps(1)}
              disabled={!accountDetail.admin || disabledPermission}
            />
          </Tabs>
        </AppBar>
        <TabContentsWrap>
          <TabPanel value={value} index={0}>
            <ServiceAccountSecrets
              accountDetail={accountDetail}
              accountMetaData={accountMetaData}
              accountSecretData={accountSecretData}
              accountSecretError={accountSecretError}
              secretStatus={secretResStatus.status}
            />
          </TabPanel>
          <TabPanel value={value} index={1}>
            <ServiceAccountPermission
              accountDetail={accountDetail}
              accountMetaData={accountMetaData}
              hasSvcAccountAcitve={hasSvcAccountAcitve}
              parentStatus={response.status}
              refresh={refresh}
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
};
AccountSelectionTabs.defaultProps = {
  accountDetail: {},
};

export default AccountSelectionTabs;
