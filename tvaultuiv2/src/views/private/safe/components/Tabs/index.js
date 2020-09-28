/* eslint-disable react/jsx-props-no-spreading */
/* eslint-disable no-nested-ternary */
/* eslint-disable import/no-unresolved */
/* eslint-disable react/forbid-prop-types */
/* eslint-disable react/require-default-props */
// eslint-disable-next-line react/forbid-prop-types
// eslint-disable-next-line react/require-default-props
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import Error from '../../../../../components/Error';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import addFolderPlus from '../../../../../assets/folder-plus.svg';
import NoSecretsIcon from '../../../../../assets/no-data-secrets.svg';
import NamedButton from '../../../../../components/NamedButton';
import NoData from '../../../../../components/NoData';
import mediaBreakpoints from '../../../../../breakpoints';
// import AddFolder from '../AddFolder';
import Tree from '../Tree';
import Permissions from '../Permissions';
import apiService from '../../apiService';
import disableAddFolder from '../../../../../assets/addfolder_inactive.svg';
import AddFolderModal from '../AddFolderModal';
import SnackbarComponent from '../../../../../components/Snackbar';
// styled components goes here

const EmptySecretBox = styled('div')`
  width: 100%;
  position: absolute;
  display: flex;
  justify-content: center;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
`;
const customBtnStyles = css`
  padding: 0.2rem 1rem;
  border-radius: 0.5rem;
`;

const TabPanelWrap = styled.div`
  position: relative;
  height: 100%;
  margin: 0;
  padding-top: 1.3rem;
  overflow: auto;
  ${mediaBreakpoints.small} {
    height: 77vh;
  }
`;
const bgIconStyle = {
  width: '16rem',
  height: '16rem',
};
const CountSpan = styled.span`
  margin-top: 1.5rem;
  color: #5e627c;
  font-size: 1.3rem;
`;

const TabContentsWrap = styled('div')`
  height: calc(100% - 4.8rem);
`;

const TabPanel = (props) => {
  const { children = '', value, index } = props;

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
  },
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    height: '4.8rem',
  },
  tab: {
    minWidth: '9.5rem',
  },
}));

export default function SelectionTabs(props) {
  const { safeDetail } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [enabledAddFolder, setEnableAddFolder] = useState(false);
  const [secretsFolder, setSecretsFolder] = useState([]);
  const [getResponse, setGetResponse] = useState(null);
  const [status, setStatus] = useState({});

  // resolution handlers
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  // const isDeskTopView = useMediaQuery(mediaBreakpoints.desktop);

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };
  const addSecretsFolder = () => {
    setEnableAddFolder(true);
  };
  // toast close handling
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setStatus({});
  };

  const addSecretsFolderList = (secretFolder) => {
    const tempFolders = [...secretsFolder] || [];
    const folderObj = {};
    folderObj.id = `${secretFolder.parentId}/${secretFolder.value}`;
    folderObj.parentId = secretFolder.parentId;
    folderObj.value = secretFolder.value;
    folderObj.type = secretFolder.type || 'folder';
    folderObj.children = [];
    setStatus({ status: 'loading', message: 'loading...' });
    apiService
      .addFolder(folderObj.id)
      .then((res) => {
        setStatus({ status: 'success', message: res.data.messages[0] });
        tempFolders[0].children.push(folderObj);
        setSecretsFolder([...tempFolders]);
      })
      .catch((error) => {
        setStatus({ status: 'failed', message: 'Secret addition failed' });
        if (!error.toString().toLowerCase().includes('network')) {
          if (error.response) {
            setStatus({
              status: 'failed',
              message: error.response?.data.errors[0],
            });
            return;
          }
        }
        if (error.toString().toLowerCase().includes('422')) {
          setStatus({
            status: 'failed',
            message: 'Folder already exists',
          });
          return;
        }
        setStatus({
          status: 'failed',
          message: 'Network Error!',
        });
      });
    setEnableAddFolder(false);
  };

  useEffect(() => {
    if (safeDetail?.path) {
      setStatus({ status: 'loading', message: 'loading...' });
      if (!safeDetail.manage) {
        setValue(0);
      }
      apiService
        .getSecret(safeDetail.path)
        .then((res) => {
          setStatus({});
          setGetResponse(1);
          setSecretsFolder([res.data]);
        })
        .catch((error) => {
          setGetResponse(-1);
          if (error.toString().toLowerCase().includes('403')) {
            return;
          }
          if (!error.toString().toLowerCase().includes('network')) {
            if (error.response) {
              return;
            }
          }
          setStatus({
            status: 'failed',
            message: 'Network Error',
          });
        });
    }
  }, [safeDetail]);

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
            {safeDetail.manage && <Tab label="Permissions" {...a11yProps(1)} />}
          </Tabs>
          {value === 0 && (
            <NamedButton
              label="Add Folder"
              onClick={addSecretsFolder}
              customStyle={customBtnStyles}
              iconSrc={
                safeDetail?.access?.toLowerCase() === 'read' ||
                Object.keys(safeDetail).length === 0
                  ? disableAddFolder
                  : addFolderPlus
              }
              disable={
                safeDetail?.access?.toLowerCase() === 'read' ||
                Object.keys(safeDetail).length === 0
              }
            />
          )}
        </AppBar>
        <TabContentsWrap>
          <TabPanel value={value} index={0}>
            {
              <CountSpan color="#5e627c">
                {`${secretsFolder && secretsFolder.length} Secrets`}
              </CountSpan>
            }
            {enabledAddFolder ? (
              <AddFolderModal
                openModal={enabledAddFolder}
                setOpenModal={setEnableAddFolder}
                childrens={secretsFolder}
                handleSaveClick={addSecretsFolderList}
                parentId={safeDetail.path}
                handleCancelClick={() => setEnableAddFolder(false)}
              />
            ) : (
              <></>
            )}
            {!secretsFolder?.length && status.status === 'loading' && (
              <ScaledLoader width="100%" height="60%" />
            )}
            {getResponse === -1 && (
              <EmptySecretBox>
                {' '}
                <Error description="Error while fetching safes folders" />
              </EmptySecretBox>
            )}

            {secretsFolder && secretsFolder.length ? (
              <Tree data={secretsFolder} />
            ) : secretsFolder?.length === 0 && getResponse === 1 ? (
              // eslint-disable-next-line react/jsx-indent
              <EmptySecretBox>
                <NoData
                  imageSrc={NoSecretsIcon}
                  description="add a <strong>Folder</strong> and then you will be able to add <strong>secrets</strong> to view them all here"
                  actionButton={
                    // eslint-disable-next-line react/jsx-wrap-multilines
                    <ButtonComponent
                      label="add"
                      icon="add"
                      color="secondary"
                      disabled={safeDetail?.access?.toLowerCase() === 'read'}
                      width={isMobileScreen ? '100%' : ''}
                      onClick={() => setEnableAddFolder(true)}
                    />
                  }
                  bgIconStyle={bgIconStyle}
                  width={isMobileScreen ? '100%' : '30%'}
                />
              </EmptySecretBox>
            ) : (
              <></>
            )}
          </TabPanel>

          <TabPanel value={value} index={1}>
            <Permissions safeDetail={safeDetail} />
          </TabPanel>

          <SnackbarComponent
            open={status.status === 'failed'}
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message={status.message || 'Something went wrong!'}
          />
          <SnackbarComponent
            open={status.status === 'success'}
            onClose={() => onToastClose()}
            severity="success"
            icon="checked"
            message={status.message || 'Folder added successfully'}
          />
        </TabContentsWrap>
      </div>
    </ComponentError>
  );
}
SelectionTabs.propTypes = {
  safeDetail: PropTypes.object,
};
SelectionTabs.defaultProps = {
  safeDetail: {},
};
