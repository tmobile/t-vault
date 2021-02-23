/* eslint-disable array-callback-return */
/* eslint-disable react/no-array-index-key */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';

import CreateSecretButton from '../../CreateSecretButton';
import { convertObjectToArray } from '../../../../../../services/helper-function';

import File from './file';
import Folder from './folder';
import AddFolderModal from '../../AddFolderModal';
import CreateSecretModal from '../../CreateSecretsModal';
import BackdropLoader from '../../../../../../components/Loaders/BackdropLoader';
import { BackgroundColor } from '../../../../../../styles/GlobalStyles';

const SecretsError = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0.5rem;
`;

const NoSecretWrap = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${BackgroundColor.secretBg};
  padding: 0.5em;
  cursor: pointer;
`;

const TreeRecursive = (props) => {
  const {
    data,
    value,
    saveSecretsToFolder,
    saveFolder,
    onFolderIsClosed,
    handleCancelClick,
    setCreateSecretBox,
    setIsAddInput,
    isAddInput,
    setInputType,
    inputType,
    status,
    setStatus,
    versionInfo,
    getChildrenData,
    onDeleteTreeItem,
    secretprefilledData,
    setSecretprefilledData,
    userHavePermission,
  } = props;
  const [currentNode, setCurrentNode] = useState('');
  const [secretEditData, setsecretEditData] = useState({});
  const [onFolderClosed, setOnFolderClosed] = useState(false);
  // loop through the data
  useEffect(() => {
    setsecretEditData(secretprefilledData);
  }, [secretprefilledData]);

 const getDaysDifference = (end) => {
  if(end){
    const date1 = new Date();
    const date2 = new Date(end);
    const diffInTime = Math.abs(date2.getTime() - date1.getTime());
    const diffInTimeDays = diffInTime / (1000);
    let time = Math.ceil(diffInTimeDays);

      return time < 60 ? "a few Seconds Ago" :
           ((time/60) < 60 ? `${Math.floor(time/60)} minutes ago` :
               ((time/3600) < 24 ? `${Math.floor(time/3600)} hours ago` :
                   `${Math.floor(time/(3600 * 24))} days ago`
                )
            )
    }else{
      return ' -- ';
    }
  };

  let arr = [];
  // eslint-disable-next-line consistent-return
  return data.map((item) => {
    let itemVersionInfo = versionInfo.filter(i=>i.folderPath === item.id)[0];
    if (
      item?.children[0]?.type.toLowerCase() === 'secret' &&
      item?.children[0]?.value
    ) {
      arr = convertObjectToArray(JSON.parse(item?.children[0]?.value));
    }

    // if its a file render <File />
    if (item.type.toLowerCase() === 'secret') {
      let secretVersionInfo = versionInfo.filter(i=>i.folderPath === item.id)[0]?.secretVersions
    
      const secretArray =
        item.value && convertObjectToArray(JSON.parse(item.value));
      return secretArray.map((secret, index) => {
        let modifiedAt = secretVersionInfo && secretVersionInfo[Object.keys(secret)[0]];
        if(Object.keys(secret)[0] !== 'default' ){
          return (<File
            key={index}
            secret={secret} 
            parentId={item.parentId}
            versionInfo={getDaysDifference(modifiedAt && modifiedAt[0]?.modifiedAt)}
            modifiedBy={(modifiedAt ? modifiedAt[0]?.modifiedBy : '')}
            setSecretprefilledData={setSecretprefilledData}
            type={item.type}
            setIsAddInput={setIsAddInput}
            setInputType={setInputType}
            onDeleteTreeItem={onDeleteTreeItem}
            id={item.id}
            userHavePermission={userHavePermission}
        />)
      } else {
          return;
      }
      })  ;
    }
    // if its a folder render <Folder />
    if (item.type === 'folder') {
      return (
        <Folder
          folderInfo={item}
          setInputType={setInputType}
          value={value}
          status={status}
          versionInfo={getDaysDifference(itemVersionInfo?.folderModifiedAt)}
          modifiedBy={itemVersionInfo?.folderModifiedBy}
          onFolderClosed={onFolderIsClosed}
          setOnFolderClosed={setOnFolderClosed}
          setIsAddInput={setIsAddInput}
          getChildNodes={getChildrenData}
          setCurrentNode={setCurrentNode}
          onDeleteTreeItem={onDeleteTreeItem}
          id={item.id}
          key={item.id}
          userHavePermission={userHavePermission}
        >
          {status.status === 'loading' && <BackdropLoader color="secondary" />}

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
            (inputType?.currentNode === item.value ||
              inputType?.currentNode === item.id) && (
              <CreateSecretModal
                existingSecrets={arr}
                openModal={isAddInput}
                secretprefilledData={secretEditData}
                setOpenModal={setIsAddInput}
                parentId={item.id}
                handleSecretCancel={(val) => {
                  setsecretEditData({});
                  setSecretprefilledData({});
                  handleCancelClick(val);
                }}
                handleSecretSave={(secret) => {
                  setsecretEditData({});
                  setSecretprefilledData({});
                  saveFolder(secret, item.id);
                }}
              />
            )}
          {Array.isArray(item.children) ? (
            <TreeRecursive
              data={item.children}
              saveSecretsToFolder={saveSecretsToFolder}
              setCreateSecretBox={setCreateSecretBox}
              value={value}
              onFolderIsClosed={onFolderClosed}
              handleCancelClick={handleCancelClick}
              saveFolder={saveFolder}
              isAddInput={isAddInput}
              setIsAddInput={setIsAddInput}
              setInputType={setInputType}
              inputType={inputType}
              path={`${item.id}/${item.value}`}
              setStatus={setStatus}
              status={status}
              versionInfo={item?.versionInfo}
              getChildrenData={getChildrenData}
              onDeleteTreeItem={onDeleteTreeItem}
              secretprefilledData={secretprefilledData}
              setSecretprefilledData={setSecretprefilledData}
              userHavePermission={userHavePermission}
            />
          ) : (
            <></>
          )}
          {currentNode === item.value && status.status === 'failed' && (
            <SecretsError>Error in loading secrets!</SecretsError>
          )}
          {[...item?.children, ...arr].length <= 1 &&
            currentNode === item.id &&
            status.status !== 'loading' &&
            (userHavePermission?.type === 'write' ? (
              <CreateSecretButton
                onClick={(e) => setCreateSecretBox(e, item.value)}
              />
            ) : (
              <NoSecretWrap>
                <span>There are no secrets here!</span>
              </NoSecretWrap>
            ))}
        </Folder>
      );
    }
  });
};

export default TreeRecursive;
