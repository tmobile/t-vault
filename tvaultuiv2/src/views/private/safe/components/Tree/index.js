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
import Error from '../../../../../components/Error';

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
}) => {
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
          parentId={path}
        >
          {Array.isArray(item.children) && (
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
              path={`${path}/${item.value}`}
            />
          )}
          <AddForm
            inputNode={
              // eslint-disable-next-line react/jsx-wrap-multilines
              inputType?.type?.toLowerCase() === 'folder' ? (
                <AddFolder
                  parentId={`${path}/${item.value}`}
                  handleCancelClick={handleCancelClick}
                  handleSaveClick={(secret) => saveFolder(secret, item.value)}
                />
              ) : (
                <CreateSecret
                  parentId={path}
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
            <Error description="error in creating folder" />
          ) : (
            <></>
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
  const [path, setPath] = useState('');

  // set inital tree data structure
  const setTreeData = (treeData) => {
    setSecretsFolder(treeData);
  };

  useEffect(() => {
    setTreeData(data);
  }, [data]);

  /**
   *Creates secrets folder array
   * @param {string} folderName
   */
  const saveSecretsToFolder = (obj, node) => {
    const tempFolders = [...secretsFolder] || [];
    const folderObj = {};
    folderObj.id = `${obj.parentId}/${obj.value}`;
    folderObj.parentId = obj.parentId;
    folderObj.value = obj.value;
    folderObj.type = obj.type || 'secret';
    folderObj.key = obj.key;
    folderObj.children = [];
    const updatedArray = findElementAndUpdate(tempFolders, node, obj);
    apiService
      .postApiCall(`/write?path=${folderObj.id}`, null)
      .then((res) => {
        console.log('res....', res);
        setSecretsFolder([...updatedArray]);
        setResponseType(1);
      })
      .catch((error) => {
        setResponseType(-1);
        console.log(error);
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
    const updatedArray = findElementAndUpdate(tempFolders, parentId, folderObj);
    // api call
    apiService
      .postApiCall(`/sdb/createfolder?path=${folderObj.id}`, null)
      .then((res) => {
        console.log('res....', res);
        setSecretsFolder([...updatedArray]);
        setResponseType(1);
      })
      .catch((error) => {
        console.log(error);
        setResponseType(-1);
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
          setPath={setPath}
          path={path}
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
export default Tree;
