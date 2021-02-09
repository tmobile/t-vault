/* eslint-disable react/jsx-props-no-spreading */
import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import ServiceAccountSecrets from '../ServiceAccountSecrets';
import ServiceAccountPermission from '../ServiceAccountPermission';
import { useStateValue } from '../../../../../contexts/globalState';
import apiService from '../../apiService';
import { getEachUsersDetails } from '../../../../../services/helper-function';
// styled components goes here

const TabPanelWrap = styled.div`
  position: relative;
  height: 100%;
  margin: 0;
  padding-top: 1.3rem;
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

const useStyles = makeStyles(() => ({
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
  const [userDetails, setUserDetails] = useState([]);
  const [secretResStatus, setSecretResStatus] = useState({ status: 'loading' });
  const [accountMetaData, setAccountMetaData] = useState({
    response: {},
    error: '',
  });
  const [state] = useStateValue();

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  const accountName = accountDetail?.name;
  useEffect(() => {
    setValue(0);
  }, [accountName]);

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

  const getServiceListMetaData = () => {
    return apiService
      .fetchServiceAccountDetails(accountDetail.name)
      .then((res) => {
        if (res.data.data.values && res.data.data.values[0]) {
          return res.data.data.values[0].owner;
        }
        return '';
      })
      .catch(() => {
        setResponse({ status: 'error' });
        setAccountMetaData({ response: {}, error: 'Something went wrong' });
      });
  };

  // Function to get the metadata of the given service account
  const fetchPermission = useCallback(() => {
    setResponse({ status: 'loading' });
    return apiService
      .updateMetaPath(accountDetail.name)
      .then(async (res) => {
        if (res.data && res.data.data) {
          const owner = await getServiceListMetaData();
          if (owner?.toLowerCase() === state?.username?.toLowerCase()) {
            setDisabledPermission(false);
            if (res.data.data.initialPasswordReset) {
              setHasSvcAccountAcitve(true);
              setAccountMetaData({ response: { ...res.data.data }, error: '' });
              const eachUsersDetails = await getEachUsersDetails(
                res.data.data.users
              );
              if (eachUsersDetails !== null) {
                setUserDetails([...eachUsersDetails]);
              }
            } else {
              setHasSvcAccountAcitve(false);
            }
          } else {
            setValue(0);
            setDisabledPermission(true);
          }
          setResponse({ status: 'success' });
        }
      })
      .catch(() => {
        setResponse({ status: 'error' });
        setAccountMetaData({ response: {}, error: 'Something went wrong' });
      });
    // eslint-disable-next-line
  }, [accountDetail, state]);

  useEffect(() => {
    setResponse({ status: 'loading' });
    setSecretResStatus({ status: 'loading' });
    setHasSvcAccountAcitve(false);
    setValue(0);
    if (accountDetail?.name) {
      fetchPermission();
      getSecrets();
    } else {
      setDisabledPermission(true);
      setSecretResStatus({ status: 'no-data' });
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
            {!disabledPermission && (
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
            <ServiceAccountSecrets
              accountDetail={accountDetail}
              accountMetaData={accountMetaData}
              value={value}
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
              fetchPermission={fetchPermission}
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
};
AccountSelectionTabs.defaultProps = {
  accountDetail: {},
};

export default AccountSelectionTabs;
