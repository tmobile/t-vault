/* eslint-disable react/forbid-prop-types */
/* eslint-disable import/no-unresolved */
/* eslint-disable react/require-default-props */
// eslint-disable-next-line react/require-default-props

import React from 'react';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import Typography from '@material-ui/core/Typography';
// import ArrowDropDownIcon from '@material-ui/icons/ArrowDropDown';
// import ArrowRightIcon from '@material-ui/icons/ArrowRight';

import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import FolderIcon from '@material-ui/icons/Folder';
import LockIcon from '@material-ui/icons/Lock';
import ComponentError from 'errorBoundaries/ComponentError/component-error';

// custom tree item styling
const useStyles = makeStyles({
  root: {
    flexGrow: 1,
    maxWidth: 400,
  },
  labelRoot: {
    display: 'flex',
    alignItems: 'center',
  },
});
const FolderTreeView = (props) => {
  const classes = useStyles();
  const { treeData } = props;

  // render recursive tree items
  const renderChild = (node) => (
    <TreeItem
      key={node.labelText}
      label={
        // eslint-disable-next-line react/jsx-wrap-multilines
        <div className={classes.labelRoot}>
          {node.labelInfo?.toLowerCase() === 'folder' ? (
            <FolderIcon color="inherit" className={classes.labelIcon} />
          ) : (
            <LockIcon color="inherit" className={classes.labelIcon} />
          )}
          <Typography variant="body2" className={classes.labelText}>
            {node.labelText}
          </Typography>
        </div>
      }
    >
      {Array.isArray(node.children)
        ? node.children.map((childItem) => renderChild(childItem))
        : null}
    </TreeItem>
  );

  const renderTree = (nodes) => {
    return nodes.map((node) => {
      return renderChild(node);
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
        {renderTree(treeData)}
      </TreeView>
    </ComponentError>
  );
};
FolderTreeView.propTypes = {
  treeData: PropTypes.array,
};
FolderTreeView.defaultProps = {
  treeData: [],
};

export default FolderTreeView;
