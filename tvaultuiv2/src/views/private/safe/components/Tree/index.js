/* eslint-disable no-nested-ternary */
/* eslint-disable consistent-return */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable array-callback-return */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';

import {
  findElementAndUpdate,
  findElementAndDelete,
  findElementAndReturnSecrets,
} from '../../../../../services/helper-function';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import TreeRecursive from './components/TreeRecursive';
import SnackbarComponent from '../../../../../components/Snackbar';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import mediaBreakpoints from '../../../../../breakpoints';
import apiService from '../../apiService';

const StyledTree = styled.div`
  line-height: 1.5;
  margin-top: 1.2rem;
  overflow-y: auto;
  & > div {
    padding-left: 0;
  }
`;
const Tree = (props) => {
  const { data } = props;
  const [secretsFolder, setSecretsFolder] = useState([]);
  const [isAddInput, setIsAddInput] = useState(false);
  const [inputType, setInputType] = useState({});
  const [status, setStatus] = useState({});
  const [secretprefilledData, setSecretprefilledData] = useState({});
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const [deletePath, setDeleteItem] = useState({});
  // set inital tree data structure
  const setTreeData = (treeData) => {
    setSecretsFolder(treeData);
  };
  useEffect(() => {
    setTreeData(data);
  }, [data]);
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }

    setStatus({});
  };

  const getChildrenData = (id) => {
    const tempFolders = [...secretsFolder] || [];
    setStatus({ status: 'loading', message: 'loading...' });
    if (id) {
      apiService
        .getSecret(id)
        .then((res) => {
          setStatus({});
          const updatedArray = findElementAndUpdate(
            tempFolders,
            id,
            res.data.children
          );
          setSecretsFolder([...updatedArray]);
        })
        .catch((error) => {
          setStatus({ status: 'failed', message: '' });
          if (!error.toString().toLowerCase().includes('network')) {
            if (error.response) {
              setStatus({
                status: 'failed',
                message: '',
              });
              return;
            }
          }
          setStatus({
            status: 'failed',
            message: 'Network Error',
          });
        });
    }
  };

  /**
   *Creates secrets folder array
   * @param {object} obj
   * @param {node} node
   */
  const saveSecretsToFolder = (obj, node) => {
    const tempFolders = [...secretsFolder] || [];
    const currentSecrets = findElementAndReturnSecrets(tempFolders, node);
    const folderObj = {};
    folderObj.id = `${obj.parentId}`;
    folderObj.parentId = obj.parentId;
    folderObj.type = obj.type || 'secret';
    folderObj.children = [];
    folderObj.value =
      currentSecrets &&
      JSON.stringify({
        data: { ...currentSecrets.data, [obj.key]: obj.value },
      });

    apiService
      .modifySecret(folderObj.id, {
        path: folderObj.id,
        data: { ...currentSecrets?.data, [obj.key]: obj.value },
      })
      // eslint-disable-next-line no-unused-vars
      .then((res) => {
        getChildrenData(node);
        setStatus({
          status: 'success',
          message: res.data.messages[0],
        });
      })
      .catch((error) => {
        setStatus({
          status: 'failed',
          message: '',
        });
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
            message: 'Secret already exists!',
          });
          return;
        }
        setStatus({
          status: 'failed',
          message: 'Network Error',
        });
      });
    setIsAddInput(false);
  };

  const saveFolderToCurrentFolder = (secretFolder, parentId) => {
    const tempFolders = [...secretsFolder] || [];
    const folderObj = {};
    folderObj.id = `${secretFolder.parentId}/${secretFolder.value}`;
    folderObj.parentId = secretFolder.parentId;
    folderObj.value = secretFolder.value;
    folderObj.type = secretFolder.type || 'folder';
    folderObj.children = [];
    // api call
    apiService
      .addFolder(folderObj.id)
      // eslint-disable-next-line no-unused-vars
      .then((res) => {
        const updatedArray = findElementAndUpdate(
          tempFolders,
          parentId,
          folderObj
        );
        setSecretsFolder([...updatedArray]);
        setStatus({
          status: 'success',
          message: res.data.messages[0],
        });
      })
      .catch((error) => {
        setStatus({
          status: 'failed',
          message: error.response?.data.errors[0],
        });
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
            message: 'Folder already exists!',
          });
          return;
        }
        setStatus({
          status: 'failed',
          message: 'Network Error',
        });
      });
    setIsAddInput(false);
  };

  const saveFolder = (secret, selectedNode) => {
    setStatus({
      status: 'loading',
      message: 'loading...',
    });

    if (secret?.type?.toLowerCase() === 'secret') {
      saveSecretsToFolder(secret, selectedNode);
      return;
    }

    saveFolderToCurrentFolder(secret, selectedNode);
  };
  const setCreateSecretBox = (e, node) => {
    setIsAddInput(e);
    setInputType({ type: 'secret', currentNode: node });
  };
  const handleCancelClick = (val) => {
    setIsAddInput(val);
  };

  const handleDeleteModalClose = () => {
    setDeleteModalOpen(false);
  };

  // delete item in the tree
  const deleteTreeItem = (node) => {
    setStatus({
      status: 'loading',
      message: 'loading...',
    });
    setDeleteModalOpen(false);
    if (node.type.toLowerCase() === 'secret') {
      const tempFolders = [...secretsFolder] || [];
      const updatedObject = findElementAndDelete(
        tempFolders,
        node.parentId,
        node.key
      );
      const payload = { path: node.parentId, data: updatedObject.data };
      apiService
        .modifySecret(node.parentId, payload)
        .then((res) => {
          getChildrenData(node.parentId);
          setStatus({
            status: 'success',
            message: 'Secret deleted Successfully',
          });
        })
        .catch((error) => {
          setStatus({ status: 'failed', message: 'Secret deletion failed!' });
        });
      return;
    }
    apiService
      .deleteFolder(node.id)
      .then((res) => {
        getChildrenData(node.parentId);
        setStatus({
          status: 'success',
          message: res.data.messages[0].toLowerCase().includes('sdb deleted')
            ? 'Folder Deleted Successfully'
            : res.data.messages[0],
        });
      })
      .catch((error) => {
        setStatus({ status: 'failed', message: 'Folder deletion failed!' });
      });
  };

  // delete tree item handler
  const onDeleteTreeItem = (node) => {
    setDeleteModalOpen(true);
    setDeleteItem(node);
  };

  return (
    <ComponentError>
      <StyledTree>
        <TreeRecursive
          data={(secretsFolder?.length && secretsFolder[0].children) || []}
          saveSecretsToFolder={saveSecretsToFolder}
          setCreateSecretBox={setCreateSecretBox}
          handleCancelClick={handleCancelClick}
          saveFolder={saveFolder}
          isAddInput={isAddInput}
          setInputType={setInputType}
          inputType={inputType}
          setIsAddInput={setIsAddInput}
          status={status}
          setStatus={setStatus}
          getChildrenData={getChildrenData}
          onDeleteTreeItem={onDeleteTreeItem}
          secretprefilledData={secretprefilledData}
          setSecretprefilledData={setSecretprefilledData}
        />
        {status.status === 'failed' ? (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message={status.message || 'Something went wrong!'}
          />
        ) : (
          status.status === 'success' && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              severity="success"
              message={status.message || 'Folder/Secret added successfully'}
            />
          )
        )}
        <ConfirmationModal
          open={deleteModalOpen}
          title={`Are you sure you want to delete this secret? `}
          cancelButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleDeleteModalClose()}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
          confirmButton={
            // eslint-disable-next-line react/jsx-wrap-multilines
            <ButtonComponent
              label="Confirm"
              color="secondary"
              onClick={() => deleteTreeItem(deletePath)}
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
      </StyledTree>
    </ComponentError>
  );
};

// props validation
Tree.propTypes = {
  data: PropTypes.arrayOf(PropTypes.any),
};

Tree.defaultProps = {
  data: [],
};

// Tree recursive props validation
export default Tree;
