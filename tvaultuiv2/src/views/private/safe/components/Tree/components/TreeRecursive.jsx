import React from 'react';
// import PropTypes from 'prop-types';
// import styled from 'styled-components';
import LoaderSpinner from '../../../../../../components/LoaderSpinner';
import CreateSecretButton from '../../CreateSecretButton';
import AddForm from '../../AddForm';
// import CreateSecret from '../../CreateSecrets';
// import AddFolder from '../../AddFolder';
import File from './file';
import Folder from './folder';
import AddFolderModal from '../../AddFolderModal';
import CreateSecretModal from '../../CreateSecretsModal';
// import { BackgroundColor } from '../../../../../styles/GlobalStyles';

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
}) => {
  // loop through the data
  return data.map((item) => {
    // if its a file render <File />

    if (item.type.toLowerCase() === 'secret') {
      return (
        <File
          key={item.id}
          secret={item.value}
          type={item.type}
          setIsAddInput={setIsAddInput}
          setInputType={setInputType}
          id={item.id}
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
          getChildNodes={getChildrenData}
          id={item.id}
          key={item.id}
        >
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
            />
          ) : (
            <></>
          )}
          {responseType === 0 ? (
            <LoaderSpinner />
          ) : (
            item?.children?.length === 0 &&
            responseType !== 0 &&
            responseType ===
              1(
                <CreateSecretButton
                  onClick={(e) => setCreateSecretBox(e, item.value)}
                />
              )
          )}
        </Folder>
      );
    }
  });
};

export default TreeRecursive;
