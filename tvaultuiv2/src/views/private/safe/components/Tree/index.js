/* eslint-disable no-nested-ternary */
/* eslint-disable consistent-return */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable array-callback-return */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

import { findElementAndUpdate } from '../../../../../services/helper-function';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import Loader from '../../../../../components/Loader';
import CreateSecretButton from '../CreateSecretButton';
import AddForm from '../AddForm';
import CreateSecret from '../CreateSecrets';
import AddFolder from '../AddFolder';
import File from './components/file';
import Folder from './components/folder';
import apiService from '../../apiService';
import SnackbarComponent from '../../../../../components/Snackbar';

const TreeRecursive = ({
  data,
  saveSecretsToFolder,
  saveFolder,
  handleCancelClick,
  setCreateSecretBox,
  setIsAddInput,
  isAddInput,
  setInputType,
  inputType,
  responseType,
  toastMessage,
  setResponseType,
  getChildrenData,
}) => {
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };
  // loop through the data
  return data.map((item) => {
    // if its a file render <File />

    if (item.type.toLowerCase() === 'secret') {
      return (
        <div key={item.id}>
          {' '}
          <File
            secretKey={item.key}
            secretValue={item.value}
            type={item.type}
            setIsAddInput={setIsAddInput}
            setInputType={setInputType}
            id={item.id}
          />
        </div>
      );
    }
    // if its a folder render <Folder />
    if (item.type === 'folder') {
      return (
        <div key={item.id}>
          <Folder
            folderInfo={item}
            setInputType={setInputType}
            setIsAddInput={setIsAddInput}
            getChildNodes={getChildrenData}
            id={item.id}
          >
            {Array.isArray(item.children) ? (
              <TreeRecursive
                data={item.children}
                saveSecretsToFolder={saveSecretsToFolder}
                setCreateSecretBox={setCreateSecretBox}
                handleCancelClick={handleCancelClick}
                saveFolder={saveFolder}
                isAddInput={isAddInput}
                setIsAddInput={setIsAddInput}
                setInputType={setInputType}
                inputType={inputType}
                path={`${item.id}/${item.value}`}
                toastMessage={toastMessage}
                setResponseType={setResponseType}
                getChildrenData={getChildrenData}
              />
            ) : (
              <></>
            )}
            <AddForm
              inputNode={
                // eslint-disable-next-line react/jsx-wrap-multilines
                inputType?.type?.toLowerCase() === 'folder' ? (
                  <AddFolder
                    parentId={item.id}
                    handleCancelClick={handleCancelClick}
                    handleSaveClick={(secret) => saveFolder(secret, item.value)}
                  />
                ) : (
                  <CreateSecret
                    parentId={item.id}
                    handleSecretCancel={handleCancelClick}
                    handleSecretSave={(secret) =>
                      saveFolder(secret, item.value)
                    }
                  />
                )
              }
              inputEnabled={inputType?.currentNode === item.value && isAddInput}
            />
            {item?.children?.length === 0 && responseType === 0 ? (
              <div>Loading</div>
            ) : (
              item?.children?.length === 0 && (
                <CreateSecretButton
                  onClick={(e) => setCreateSecretBox(e, item.value)}
                />
              )
            )}
            {responseType === 0 ? (
              <Loader />
            ) : responseType === -1 && !isAddInput ? (
              <SnackbarComponent
                open
                onClose={() => onToastClose()}
                severity="error"
                icon="error"
                message={toastMessage || 'Something went wrong!'}
              />
            ) : (
              responseType === 1 &&
              !isAddInput && (
                <SnackbarComponent
                  open
                  onClose={() => onToastClose()}
                  severity="success"
                  message={toastMessage || 'Folder/Secret added successfully'}
                />
              )
            )}
          </Folder>
        </div>
      );
    }
  });
};
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
    // const idClone = id.split('/');
    // idClone.splice(idClone.length - 1, 1);
    // const parentId = idClone.join('/');

    apiService
      .getSecret(id)
      .then((res) => {
        console.log('child------', res);
        const updatedArray = findElementAndUpdate(
          tempFolders,
          id,
          res.data.children
        );
        setSecretsFolder([...updatedArray]);
        console.log('treeeeData', secretsFolder);
      })
      .catch((error) => {
        setResponseType(-1);
        if (!error.toString().toLowerCase().includes('network')) {
          if (error.response) {
            setToastMessage(error.response?.data.errors[0]);
            return;
          }
        }
        setToastMessage('Network Error');
      });
  };

  /**
   *Creates secrets folder array
   * @param {object} obj
   * @param {node} node
   */
  const saveSecretsToFolder = (obj, node) => {
    const tempFolders = [...secretsFolder] || [];
    const folderObj = {};
    folderObj.id = `${obj.parentId}`;
    folderObj.parentId = obj.parentId;
    folderObj.value = obj.value;
    folderObj.type = obj.type || 'secret';
    folderObj.key = obj.key;
    folderObj.children = [];

    apiService
      .addSecret(folderObj.id)
      // eslint-disable-next-line no-unused-vars
      .then((res) => {
        setResponseType(1);
        const updatedArray = findElementAndUpdate(tempFolders, node, obj);
        setSecretsFolder([...updatedArray]);
        setToastMessage(res.data.messages[0]);
      })
      .catch((error) => {
        setResponseType(-1);
        console.log('error', error);
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
  return (
    <ComponentError>
      <StyledTree>
        <TreeRecursive
          data={secretsFolder}
          saveSecretsToFolder={saveSecretsToFolder}
          setCreateSecretBox={setCreateSecretBox}
          handleCancelClick={handleCancelClick}
          saveFolder={saveFolder}
          isAddInput={isAddInput}
          setInputType={setInputType}
          inputType={inputType}
          setIsAddInput={setIsAddInput}
          responseType={responseType}
          onToastClose={onToastClose}
          toastMessage={toastMessage}
          setResponseType={setResponseType}
          getChildrenData={getChildrenData}
        />
      </StyledTree>
    </ComponentError>
  );
};

Tree.File = File;
Tree.Folder = Folder;

// props validation
Tree.propTypes = {
  data: PropTypes.arrayOf(PropTypes.any),
};

Tree.defaultProps = {
  data: [],
};

// Tree recursive props validation
export default Tree;
