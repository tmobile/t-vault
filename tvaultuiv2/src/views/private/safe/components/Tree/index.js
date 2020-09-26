/* eslint-disable no-nested-ternary */
/* eslint-disable consistent-return */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable array-callback-return */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

import {
  findElementAndUpdate,
  findElementAndDelete,
  findElementAndReturnSecrets,
} from '../../../../../services/helper-function';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import TreeRecursive from './components/TreeRecursive';
import SnackbarComponent from '../../../../../components/Snackbar';
import apiService from '../../apiService';

const StyledTree = styled.div`
  line-height: 1.5;
  margin-top: 1.2rem;
  height: 43.5vh;
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
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [secretprefilledData, setSecretprefilledData] = useState({});
  const [status, setStatus] = useState({});

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
    setResponseType(null);
  };

  const getChildrenData = (id) => {
    const tempFolders = [...secretsFolder] || [];
    setResponseType(0);
    if (id) {
      apiService
        .getSecret(id)
        .then((res) => {
          setResponseType(null);
          const updatedArray = findElementAndUpdate(
            tempFolders,
            id,
            res.data.children
          );
          setSecretsFolder([...updatedArray]);
        })
        .catch((error) => {
          if (!error.toString().toLowerCase().includes('network')) {
            if (error.response) {
              setToastMessage(error.response?.data.errors[0]);
              return;
            }
          }
          setToastMessage('Network Error');
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
        setResponseType(1);
        getChildrenData(node);
        setToastMessage(res.data.messages[0]);
      })
      .catch((error) => {
        setResponseType(-1);
        if (!error.toString().toLowerCase().includes('network')) {
          if (error.response) {
            setToastMessage(error.response?.data.errors[0]);
            return;
          }
        }
        if (error.toString().toLowerCase().includes('422')) {
          setToastMessage('Secret already exists');
          return;
        }
        setToastMessage('Network Error');
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
        setResponseType(1);
        const updatedArray = findElementAndUpdate(
          tempFolders,
          parentId,
          folderObj
        );
        setSecretsFolder([...updatedArray]);
        setToastMessage(res.data.messages[0]);
      })
      .catch((error) => {
        setResponseType(-1);
        if (!error.toString().toLowerCase().includes('network')) {
          if (error.response) {
            setToastMessage(error.response?.data.errors[0]);
            return;
          }
        }
        if (error.toString().toLowerCase().includes('422')) {
          setToastMessage('folder already exists');
          return;
        }
        setToastMessage('Network Error');
      });
    setIsAddInput(false);
  };

  const saveFolder = (secret, selectedNode) => {
    setResponseType(0);
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

  // delete item in the tree
  const deleteTreeItem = (node, parent) => {
    setResponseType(0);
    if (node.type.toLowerCase() === 'secret') {
      const tempFolders = [...secretsFolder] || [];
      const updatedObject = findElementAndDelete(tempFolders, parent, node.key);
      const payload = { path: parent, data: updatedObject.data };
      apiService
        .modifySecret(parent, payload)
        .then((res) => {
          setResponseType(1);
          getChildrenData(parent);
          setToastMessage(res.data.messages[0]);
          setStatus({
            status: 'success',
            message: 'Secret deleted Successfully',
          });
        })
        .catch((error) => {
          setResponseType(-1);
          setStatus({ status: 'failed', message: 'Secret deletion failed!' });
        });
      return;
    }
    apiService
      .deleteFolder(node.id)
      .then((res) => {
        setResponseType(1);
        getChildrenData(parent);
        setToastMessage(
          res.data.messages[0].toLowerCase().includes('sdb deleted')
            ? 'Folder Deleted Successfully'
            : res.data.messages[0]
        );
      })
      .catch((error) => {
        setResponseType(-1);
        setStatus({ status: 'failed', message: 'Folder deletion failed!' });
      });
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
          responseType={responseType}
          setResponseType={setResponseType}
          getChildrenData={getChildrenData}
          deleteTreeItem={deleteTreeItem}
          secretprefilledData={secretprefilledData}
          setSecretprefilledData={setSecretprefilledData}
        />
        {(responseType === -1 || status.status === 'failed') && !isAddInput ? (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message={toastMessage || 'Something went wrong!'}
          />
        ) : (
          (responseType === 1 || status.status === 'success') &&
          !isAddInput && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              severity="success"
              message={toastMessage || 'Folder/Secret added successfully'}
            />
          )
        )}
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
