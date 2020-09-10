/* eslint-disable consistent-return */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable array-callback-return */
/* eslint-disable import/no-unresolved */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

import { findElementAndUpdate } from 'services';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import CreateSecretButton from '../CreateSecretButton';
import AddForm from '../AddForm';
import CreateSecret from '../CreateSecrets';
import AddFolder from '../AddFolder';
import File from './components/file';
import Folder from './components/folder';

const TreeRecursive = ({
  data,
  saveSecretsToFolder,
  saveFolder,
  handleCancelClick,
  setCreateSecretBox,
  isAddFolder,
  inputType,
}) => {
  // loop through the data
  return data.map((item) => {
    // if its a file render <File />
    if (item.type.toLowerCase() === 'file') {
      return (
        <File
          secretKey={item.labelKey}
          secretValue={item.labelValue}
          type={item.type}
        />
      );
    }
    // if its a folder render <Folder />
    if (item.type === 'folder') {
      return (
        <Folder folderInfo={item}>
          {Array.isArray(item.children) && (
            <TreeRecursive
              data={item.children}
              saveSecretsToFolder={saveSecretsToFolder}
              setCreateSecretBox={setCreateSecretBox}
              handleCancelClick={handleCancelClick}
              saveFolder={saveFolder}
              isAddFolder={isAddFolder}
              inputType={inputType}
            />
          )}

          <AddForm
            inputNode={
              // eslint-disable-next-line react/jsx-wrap-multilines
              inputType.toLowerCase() === 'folder' ? (
                <AddFolder
                  handleCancelClick={handleCancelClick}
                  handleSaveClick={(secret) =>
                    saveFolder(secret, item.labelText)
                  }
                />
              ) : (
                <CreateSecret
                  handleSecretCancel={handleCancelClick}
                  handleSecretSave={(secret) =>
                    saveFolder(secret, item.labelText)
                  }
                />
              )
            }
            inputEnabled={isAddFolder}
            createButton={<CreateSecretButton onClick={setCreateSecretBox} />}
          />
        </Folder>
      );
    }
  });
};
const StyledTree = styled.div`
  line-height: 1.5;
  margin-top: 1.2rem;
`;
const Tree = (props) => {
  const { data } = props;
  const [secretsFolder, setSecretsFolder] = useState([]);
  const [isAddFolder, setIsAddFolder] = useState(false);
  const [inputType, setInputType] = useState('');

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
  const saveSecretsToFolder = (obj, parentId) => {
    const tempFolders = [...secretsFolder] || [];
    const folderObj = {};
    folderObj.labelText = obj.labelValue;
    folderObj.type = obj.type || 'file';
    folderObj.labelKey = obj.labelKey;
    folderObj.children = [];
    const updatedArray = findElementAndUpdate(tempFolders, parentId, obj);
    setSecretsFolder([...updatedArray]);
    setIsAddFolder(false);
  };

  const saveFolder = (secret, selectedNode) => {
    saveSecretsToFolder(secret, selectedNode);
  };
  const setCreateSecretBox = (e) => {
    setIsAddFolder(e);
  };
  const handleCancelClick = (val) => {
    setIsAddFolder(val);
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
          isAddFolder={isAddFolder}
          inputType={inputType}
        />
      </StyledTree>
    </ComponentError>
  );
};

Tree.File = File;
Tree.Folder = Folder;

// props validation
Tree.propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  data: PropTypes.array,
};

Tree.defaultProps = {
  data: [],
};
export default Tree;
