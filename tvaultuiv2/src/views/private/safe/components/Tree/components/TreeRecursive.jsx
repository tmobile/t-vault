/* eslint-disable consistent-return */
import React, { useState } from 'react';
// import PropTypes from 'prop-types';
import { css } from 'styled-components';
import LoaderSpinner from '../../../../../../components/LoaderSpinner';
import CreateSecretButton from '../../CreateSecretButton';
import { convertObjectToArray } from '../../../../../../services/helper-function';
// import AddForm from '../../AddForm';
// import CreateSecret from '../../CreateSecrets';
// import AddFolder from '../../AddFolder';
import File from './file';
import Folder from './folder';
import AddFolderModal from '../../AddFolderModal';
import CreateSecretModal from '../../CreateSecretsModal';
// import { BackgroundColor } from '../../../../../styles/GlobalStyles';

const loaderStyle = css`
  margin-top: 0.5rem;
`;
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
  setResponseType,
  getChildrenData,
  deleteTreeItem,
  secretprefilledData,
  setSecretprefilledData,
}) => {
  const [currentNode, setCurrentNode] = useState('');
  // loop through the data
  // eslint-disable-next-line array-callback-return
  return data.map((item) => {
    // if its a file render <File />

    if (item.type.toLowerCase() === 'secret') {
      const secretArray =
        item.value && convertObjectToArray(JSON.parse(item.value));
      return secretArray.map((secret) =>
        !secret.default ? (
          <File
            key={item.id}
            secret={secret}
            parentId={item.parentId}
            setSecretprefilledData={setSecretprefilledData}
            type={item.type}
            setIsAddInput={setIsAddInput}
            setInputType={setInputType}
            deleteTreeItem={deleteTreeItem}
            id={item.id}
          />
        ) : (
          <></>
        )
      );
    }
    // if its a folder render <Folder />
    if (item.type === 'folder') {
      return (
        <Folder
          folderInfo={item}
          setInputType={setInputType}
          setIsAddInput={setIsAddInput}
          getChildNodes={getChildrenData}
          setCurrentNode={setCurrentNode}
          deleteTreeItem={deleteTreeItem}
          id={item.id}
          key={item.id}
        >
          {responseType === 0 && currentNode === item.id && (
            <LoaderSpinner size="small" customStyle={loaderStyle} />
          )}

          {inputType?.type?.toLowerCase() === 'folder' &&
            inputType?.currentNode === item.value && (
              <AddFolderModal
                openModal={isAddInput}
                setOpenModal={setIsAddInput}
                childrens={item?.children.length ? item.children : data}
                parentId={item.id}
                handleCancelClick={handleCancelClick}
                handleSaveClick={(secret) => saveFolder(secret, item.value)}
              />
            )}
          {inputType?.type?.toLowerCase() === 'secret' &&
            inputType?.currentNode === item.value && (
              <CreateSecretModal
                openModal={isAddInput}
                secretprefilledData={secretprefilledData}
                setOpenModal={setIsAddInput}
                parentId={item.id}
                handleSecretCancel={handleCancelClick}
                handleSecretSave={(secret) => saveFolder(secret, item.value)}
              />
            )}
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
              setResponseType={setResponseType}
              getChildrenData={getChildrenData}
              deleteTreeItem={deleteTreeItem}
              secretprefilledData={secretprefilledData}
              setSecretprefilledData={setSecretprefilledData}
            />
          ) : (
            <></>
          )}
          {item?.children?.length < 2 && responseType !== 0 && (
            <CreateSecretButton
              onClick={(e) => setCreateSecretBox(e, item.value)}
            />
          )}
        </Folder>
      );
    }
  });
};

export default TreeRecursive;
