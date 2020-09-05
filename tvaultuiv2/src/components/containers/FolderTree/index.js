/* eslint-disable react/forbid-prop-types */
/* eslint-disable import/no-unresolved */
/* eslint-disable react/require-default-props */
// eslint-disable-next-line react/require-default-props

import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import Typography from '@material-ui/core/Typography';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';

// import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import FolderOutlinedIcon from '@material-ui/icons/FolderOutlined';
import MoreVertOutlinedIcon from '@material-ui/icons/MoreVertOutlined';
import LockIcon from '@material-ui/icons/Lock';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import PopperElement from 'components/common/popper';
import TreeItemWrapper from 'components/common/TreeItemWrapper';
import AddFolder from 'components/add-folder';
import AccordionFolder from '../../common/AccordionFolder';
import CreateSecretButton from '../../createSecretButton';

// custom tree item styling
const useStyles = makeStyles({
  root: {
    flexGrow: 1,
  },
  labelRoot: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  labelText: {
    WebkitTextSecurity: 'disc',
  },
  labelKeyWrap: {
    textAlign: 'left',
    display: 'flex',
    alignItems: 'center',
  },
  labelKey: {
    textAlign: 'left',
    paddingLeft: '1rem',
  },
});

const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
  }
`;
const FolderTreeView = (props) => {
  const classes = useStyles();
  const {
    treeData,
    saveSecretsToFolder,
    handleCancelClick,
    secrets,
    addFolderToTargetFolder,
  } = props;
  const [updatedTreeData, setUpdatedTreeData] = useState([]);
  const [isPopperOpen, setIsPopperOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const [isAddFolder, setIsAddFolder] = useState(false);

  const setTreeData = (data) => {
    setUpdatedTreeData(data);
  };
  useEffect(() => {
    setTreeData(treeData);
  }, [treeData]);

  // handle popper clicks
  const enablePopper = (e) => {
    setIsPopperOpen(true);
    setAnchorEl(e.currentTarget);
  };

  const handlePopperClick = () => {
    addFolderToTargetFolder();
    setIsAddFolder(true);
  };

  // render recursive tree items
  // eslint-disable-next-line consistent-return
  const renderChild = (node) =>
    node.map((item) => (
      <TreeItem
        key={item.id}
        label={
          // eslint-disable-next-line react/jsx-wrap-multilines
          <div className={classes.labelRoot}>
            <div className={classes.labelKeyWrap}>
              {item.type?.toLowerCase() === 'folder' ? (
                <FolderOutlinedIcon
                  color="inherit"
                  className={classes.labelIcon}
                />
              ) : (
                <LockIcon color="inherit" className={classes.labelIcon} />
              )}
              {item.labelKey && (
                <Typography variant="body1" className={classes.labelKey}>
                  {item.id}
                </Typography>
              )}
            </div>
            <Typography variant="body2" className={classes.labelText}>
              {item.labelText}
            </Typography>
            <FolderIconWrap onClick={(e) => enablePopper(e)}>
              <MoreVertOutlinedIcon />
              <PopperElement
                open={isPopperOpen}
                popperContent={<span>create folder</span>}
                position="right-start"
                anchorEl={anchorEl}
                handlePopperClick={handlePopperClick}
              />
            </FolderIconWrap>
          </div>
        }
      >
        {Array.isArray(node.children)
          ? node.children.map((childItem) => (
              // eslint-disable-next-line react/jsx-indent
              <TreeItemWrapper
                inputNode={
                  // eslint-disable-next-line react/jsx-wrap-multilines
                  <AddFolder
                    handleCancelClick={handleCancelClick}
                    handleSaveClick={saveSecretsToFolder}
                  />
                }
                inputEnabled={isAddFolder}
                createButton={<CreateSecretButton />}
                renderChilds={renderChild(childItem)}
              />
            ))
          : null}
      </TreeItem>
    ));

  const renderTree = (nodes) => {
    return nodes.map((node) => {
      return (
        <AccordionFolder
          title={node.labelText}
          summaryIcon={<ExpandMoreIcon />}
          saveSecretsToFolder={saveSecretsToFolder}
          handleCancelClick={handleCancelClick}
          accordianChildren={renderChild(secrets)}
        />
      );
    });
  };

  return (
    <ComponentError>
      <TreeView
        className={classes.root}
        defaultCollapseIcon={<ExpandMoreIcon />}
        defaultExpandIcon={<ChevronRightIcon />}
        defaultEndIcon={<div style={{ width: 24 }} />}
      >
        {!!updatedTreeData && renderTree(updatedTreeData)}
      </TreeView>
    </ComponentError>
  );
};
FolderTreeView.propTypes = {
  treeData: PropTypes.array,
  saveSecretsToFolder: PropTypes.func,
  handleCancelClick: PropTypes.func,
  addFolderToTargetFolder: PropTypes.func,
  secrets: PropTypes.array,
};
FolderTreeView.defaultProps = {
  treeData: [],
  saveSecretsToFolder: () => {},
  handleCancelClick: () => {},
  addFolderToTargetFolder: () => {},
  secrets: [],
};

export default FolderTreeView;
