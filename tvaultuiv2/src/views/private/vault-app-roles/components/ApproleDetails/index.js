/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-props-no-spreading */
import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import AppRoleSecrets from '../AppRoleSecrets';
import apiService from '../../apiService';
import NoData from '../../../../../components/NoData';
import Error from '../../../../../components/Error';
import NoSafesIcon from '../../../../../assets/no-data-safes.svg';
import SnackbarComponent from '../../../../../components/Snackbar';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
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

const NoDataWrapper = styled.div`
  height: 61vh;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #5e627c;
  span {
    margin: 0 0.4rem;
    color: #fff;
    font-weight: bold;
    text-transform: uppercase;
  }
`;
const noDataStyle = css`
  width: 100%;
`;

const NoSecretIdWrap = styled.div`
  width: 40%;
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

const AppRoleDetails = (props) => {
  const { appRoleDetail } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [status, setStatus] = useState({});
  const [secretIdsData, setSecretIdsData] = useState(null);
  const [getResponseType, setGetResponseType] = useState(null);

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  // Function to get the secretIDs  of the given approle.
  const getSecrets = useCallback(() => {
    setStatus({ status: 'loading' });
    apiService
      .getAccessors(appRoleDetail?.name)
      .then((res) => {
        setStatus({});
        if (res?.data) {
          setSecretIdsData(res.data.keys);
        }
        setGetResponseType(1);
      })
      .catch((err) => {
        if (
          err?.response &&
          err.response.data?.errors &&
          err.response.data.errors[0]
        ) {
          setStatus({ message: err.response.data.errors[0] });
        }
        setGetResponseType(1);
      });
  }, [appRoleDetail]);

  useEffect(() => {
    getSecrets();
  }, [getSecrets]);

  /**
   * @function OnDeleteSecretIds
   * @param secretId Secret id/s to be deleted
   * @description To delete the secretIds
   */

  const OnDeleteSecretIds = () => {
    setStatus({ status: 'loading' });
    const payload = {};
    apiService
      .deleteSecretIds(payload)
      .then((res) => {
        setStatus({ status: 'success', message: '' });
      })
      .catch();
  };
  const onToastClose = () => {
    setStatus({});
    setGetResponseType(null);
  };
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
          </Tabs>
        </AppBar>
        <TabContentsWrap>
          <TabPanel value={value} index={0}>
            {status?.status === 'loading' && <LoaderSpinner size="medium" />}
            <span>{`${secretIdsData?.length} secretIds`}</span>
            {getResponseType === 1 && secretIdsData ? (
              <AppRoleSecrets
                secretIds={secretIdsData}
                deleteSecretIds={OnDeleteSecretIds}
              />
            ) : getResponseType === 1 && secretIdsData?.length === 0 ? (
              <NoDataWrapper>
                {' '}
                <NoSecretIdWrap>
                  <NoData
                    imageSrc={NoSafesIcon}
                    description="There are no secretIds to view here.!"
                    actionButton={<></>}
                    customStyle={noDataStyle}
                  />
                </NoSecretIdWrap>
              </NoDataWrapper>
            ) : getResponseType === -1 ? (
              <Error description="Error while fetching secretId's" />
            ) : null}
          </TabPanel>
        </TabContentsWrap>
        {status.status === 'success' && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message={status.message}
          />
        )}
        {status.status === 'failed' && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message="Something went wrong!"
          />
        )}
      </div>
    </ComponentError>
  );
};
AppRoleDetails.propTypes = {
  appRoleDetail: PropTypes.objectOf(PropTypes.object).isRequired,
};
AppRoleDetails.defaultProps = {};

export default AppRoleDetails;
