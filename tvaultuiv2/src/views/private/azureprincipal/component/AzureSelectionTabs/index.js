/* eslint-disable no-inner-declarations */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable no-else-return */
/* eslint-disable react/jsx-props-no-spreading */
import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../../apiService';
import AzureSecrets from '../AzureSecrets';
import AzurePermission from '../AzurePermission';
import { getEachUsersDetails } from '../../../../../services/helper-function';

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
      id={`certs-tabpanel-${index}`}
      aria-labelledby={`cert-tab-${index}`}
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
    height: 'calc(100% - 17.1rem)',
    display: 'flex',
    flexDirection: 'column',
    background: 'linear-gradient(to bottom,#151820,#2c3040)',
  },
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    height: '4.8rem',
    alignItems: 'center',
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

const AzureSelectionTabs = (props) => {
  const { azureDetail, refresh } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [azureSecretData, setAzureSecretData] = useState({});
  const [hasPermission, setHasPermission] = useState(false);
  const [azureMetaData, setAzureMetaData] = useState({});
  const [userDetails, setUserDetails] = useState([]);
  const [secretResponse, setSecretResponse] = useState({
    status: '',
    message: '',
  });
  const [permissionResponse, setPermissionResponse] = useState({
    status: '',
    message: '',
  });

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  const getEachUser = async (data) => {
    const eachUsersDetails = await getEachUsersDetails(data);
    if (eachUsersDetails !== null) {
      setUserDetails([...eachUsersDetails]);
    }
  };

  const getAzureServiceAllDetails = useCallback(() => {
    setPermissionResponse({ status: 'loading' });
    return apiService
      .getAzureserviceDetails(`${azureDetail.name}`)
      .then((res) => {
        setAzureMetaData(res.data);
        if (
          res?.data?.isActivated &&
          res?.data?.owner_ntid?.toLowerCase() ===
            sessionStorage.getItem('username').toLowerCase()
        ) {
          setHasPermission(true);
          getEachUser(res.data.users);
          setPermissionResponse({ status: 'success' });
        } else {
          setHasPermission(false);
          setValue(0);
        }
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setPermissionResponse({
            status: 'error',
            message: err?.response?.data?.errors[0],
          });
        } else {
          setPermissionResponse({ status: 'error' });
        }
      });
  }, [azureDetail]);

  const getAzureDataSecrets = useCallback(() => {
    if (azureDetail?.name) {
      if (azureDetail.access !== 'N/A') {
        apiService
          .getAzureSecrets(azureDetail.name)
          .then((res) => {
            if (res?.data) {
              setAzureSecretData(res.data);
            }
          })
          .catch((err) => {
            if (azureDetail.access === 'deny') {
              setSecretResponse({
                status: 'error',
                message:
                  'Access denied: no permission to read the password details for the given service account',
              });
            } else if (
              err?.response?.data?.errors &&
              err?.response?.data?.errors[0]
            ) {
              setSecretResponse({
                status: 'error',
                message: err?.response?.data?.errors[0],
              });
            } else {
              setSecretResponse({ status: 'error' });
            }
          });
      } else {
        setAzureSecretData({});
        setSecretResponse({ status: 'success' });
      }
    }
  }, [azureDetail]);

  useEffect(() => {
    if (Object.keys(azureDetail).length > 0) {
      if (!azureDetail.isManagable) {
        setHasPermission(false);
        setValue(0);
      }
      async function fetchData() {
        setSecretResponse({ status: 'loading' });
        await getAzureServiceAllDetails();
        getAzureDataSecrets();
      }
      fetchData();
    }
  }, [getAzureDataSecrets, azureDetail, getAzureServiceAllDetails]);

  return (
    <ComponentError>
      <div className={classes.root}>
        <AppBar position="static" className={classes.appBar}>
          <Tabs
            value={value}
            onChange={handleChange}
            aria-label="cert tabs"
            indicatorColor="secondary"
            textColor="primary"
          >
            <Tab className={classes.tab} label="Secrets" {...a11yProps(0)} />
            {hasPermission && <Tab label="Permissions" {...a11yProps(1)} />}
          </Tabs>
        </AppBar>
        <TabContentsWrap>
          <TabPanel value={value} index={0}>
            <AzureSecrets
              azureSecretData={azureSecretData}
              secretResponse={secretResponse}
              azureMetaData={azureMetaData}
              value={value}
              azureDetail={azureDetail}
              refresh={refresh}
            />
          </TabPanel>
          <TabPanel value={value} index={1}>
            <AzurePermission
              azureMetaData={azureMetaData}
              userDetails={userDetails}
              refresh={() => getAzureServiceAllDetails()}
              permissionResponse={permissionResponse}
              azureDetail={azureDetail}
            />
          </TabPanel>
        </TabContentsWrap>
      </div>
    </ComponentError>
  );
};

AzureSelectionTabs.propTypes = {
  azureDetail: PropTypes.objectOf(PropTypes.any),
  refresh: PropTypes.func.isRequired,
};
AzureSelectionTabs.defaultProps = {
  azureDetail: {},
};

export default AzureSelectionTabs;
