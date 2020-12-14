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

const TabPanelWrap = styled.div`
  position: relative;
  height: 100%;
  margin: 0;
  padding-top: 1.3rem;
`;

const TabContentsWrap = styled('div')`
  height: calc(100% - 6rem);
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
    height: '100%',
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
  const { azureDetail } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [azureSecretData, setAzureSecretData] = useState({});
  const [hasPermission, setHasPermission] = useState(false);
  const [azureMetaData, setAzureMetaData] = useState({});
  const [secretResponse, setSecretResponse] = useState({
    status: '',
    message: '',
  });

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  const getAzureServiceAllDetails = useCallback(() => {
    setSecretResponse({ status: 'loading' });
    return apiService
      .getAzureserviceDetails(`${azureDetail.name}`)
      .then((res) => {
        setAzureMetaData(res.data);
        if (
          res?.data?.isActivated &&
          res?.data?.owner_ntid?.toLowerCase() ===
            localStorage.getItem('username').toLowerCase()
        ) {
          setHasPermission(true);
        } else {
          setHasPermission(false);
          setValue(0);
        }
      })
      .catch(() => {
        setSecretResponse({ status: 'error' });
      });
  }, [azureDetail]);

  const getAzureDataSecrets = useCallback(() => {
    if (azureDetail?.name) {
      setSecretResponse({ status: 'loading' });
      if (azureDetail.access !== 'N/A') {
        apiService
          .getAzureSecrets(azureDetail.name)
          .then((res) => {
            setSecretResponse({ status: 'success' });
            if (res?.data) {
              setAzureSecretData(res.data);
            }
          })
          .catch((err) => {
            if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
              setSecretResponse({
                status: 'error',
                message: err?.response?.data?.errors[0],
              });
            } else {
              setSecretResponse({ status: 'error' });
            }
          });
      } else {
        setSecretResponse({ status: 'inactive' });
      }
    }
  }, [azureDetail]);

  useEffect(() => {
    if (Object.keys(azureDetail).length > 0) {
      async function fetchData() {
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
            <Tab className={classes.tab} label="Secret" {...a11yProps(0)} />
            {hasPermission && <Tab label="Permissions" {...a11yProps(1)} />}
          </Tabs>
        </AppBar>
        <TabContentsWrap>
          <TabPanel value={value} index={0}>
            <AzureSecrets
              azureSecretData={azureSecretData}
              secretResponse={secretResponse}
              azureDetail={azureDetail}
            />
          </TabPanel>
          <TabPanel value={value} index={1}>
            <AzurePermission azureMetaData={azureMetaData} />
          </TabPanel>
        </TabContentsWrap>
      </div>
    </ComponentError>
  );
};

AzureSelectionTabs.propTypes = {
  azureDetail: PropTypes.objectOf(PropTypes.any),
};
AzureSelectionTabs.defaultProps = {
  azureDetail: {},
};

export default AzureSelectionTabs;
