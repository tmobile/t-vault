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
  path,
  toastMessage,
  setResponseType,
  childrenData,
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
        <File
          secretKey={item.key}
          secretValue={item.value}
          type={item.type}
          setIsAddInput={setIsAddInput}
          setInputType={setInputType}
          parentId={path}
        />
      );
    }
    // if its a folder render <Folder />
    if (item.type === 'folder') {
      return (
        <Folder
          folderInfo={item}
          setInputType={setInputType}
          setIsAddInput={setIsAddInput}
          onClick={getChildrenData}
          parentId={path}
        >
          {Array.isArray(childrenData.childrens) && (
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
              childrenData={childrenData}
              setResponseType={setResponseType}
            />
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
                  handleSecretSave={(secret) => saveFolder(secret, item.value)}
                />
              )
            }
            inputEnabled={inputType?.currentNode === item.value && isAddInput}
          />
          {item?.children?.length === 0 && (
            <CreateSecretButton
              onClick={(e) => setCreateSecretBox(e, item.value)}
            />
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
      );
    }
  });
};
const StyledTree = styled.div`
  line-height: 1.5;
  margin-top: 1.2rem;
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
  const [childData, setChildData] = useState({});

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
        console.log(error);
        if (!error.toString().toLowerCase().includes('network')) {
          setToastMessage(error.response.data.messages[0]);
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
        console.log(error);
        setResponseType(-1);
        if (!error.toString().toLowerCase().includes('network')) {
          setToastMessage(error.response.data.messages[0]);
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
