/* eslint-disable consistent-return */
import React, { useState } from 'react';
import styled from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';

import CreateSecretButton from '../../CreateSecretButton';
import { convertObjectToArray } from '../../../../../../services/helper-function';

import File from './file';
import Folder from './folder';
import AddFolderModal from '../../AddFolderModal';
import CreateSecretModal from '../../CreateSecretsModal';
import BackdropLoader from '../../../../../../components/Loaders/BackdropLoader';
// import { BackgroundColor } from '../../../../../styles/GlobalStyles';

const useStyles = makeStyles(() => ({
  backdrop: {
    position: 'absolute',
  },
}));
const SecretsError = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0.5rem;
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
  status,
  setStatus,
  getChildrenData,
  onDeleteTreeItem,
  secretprefilledData,
  setSecretprefilledData,
}) => {
  const [currentNode, setCurrentNode] = useState('');
  const classes = useStyles();
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
            onDeleteTreeItem={onDeleteTreeItem}
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
          onDeleteTreeItem={onDeleteTreeItem}
          id={item.id}
          key={item.id}
        >
          {status.status === 'loading' && (
            <BackdropLoader classes={classes} color="secondary" />
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
              setStatus={setStatus}
              status={status}
              getChildrenData={getChildrenData}
              onDeleteTreeItem={onDeleteTreeItem}
              secretprefilledData={secretprefilledData}
              setSecretprefilledData={setSecretprefilledData}
            />
          ) : (
            <></>
          )}
          {currentNode === item.value && status.status === 'failed' && (
            <SecretsError>Error in loading secrets!</SecretsError>
          )}
          {item?.children?.length < 2 && currentNode === item.id && (
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
