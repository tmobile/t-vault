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
  handlePopperClick,
}) => {
  // loop through the data
  return data.map((item) => {
    // if its a file render <File />
    if (item.type === 'file') {
      return <File name={item.labelText} />;
    }
    // if its a folder render <Folder />
    if (item.type === 'folder') {
      return (
        <Folder name={item.labelText} popperClickHandler={handlePopperClick}>
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
          <TreeRecursive
            data={item.children}
            saveSecretsToFolder={saveSecretsToFolder}
            handlePopperClick={handlePopperClick}
            setCreateSecretBox={setCreateSecretBox}
            handleCancelClick={handleCancelClick}
            saveFolder={saveFolder}
            isAddFolder={isAddFolder}
            inputType={inputType}
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

  const [updatedTreeData, setUpdatedTreeData] = useState([]);
  const [secretsFolder, setSecretsFolder] = useState([]);
  const [isAddFolder, setIsAddFolder] = useState(false);
  const [inputType, setInputType] = useState('');

  const setTreeData = (treeData) => {
    setUpdatedTreeData(treeData);
  };
  useEffect(() => {
    setTreeData(data);
  }, [data]);

  /**
   *Creates secrets folder array
   * @param {string} folderName
   */

  const saveSecretsToFolder = (obj, parentId) => {
    debugger;
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

  // prop handlers of tree map
  const handlePopperClick = () => {
    setIsAddFolder(true);
    setInputType('folder');
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
          data={updatedTreeData}
          saveSecretsToFolder={saveSecretsToFolder}
          handlePopperClick={handlePopperClick}
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
