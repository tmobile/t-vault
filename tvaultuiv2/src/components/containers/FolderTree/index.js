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
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';

// import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import FolderIcon from '@material-ui/icons/Folder';
import LockIcon from '@material-ui/icons/Lock';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import AccordionFolder from '../../common/AccordionFolder';

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
const FolderTreeView = (props) => {
  const classes = useStyles();
  const { treeData, saveSecretsToFolder, secrets } = props;

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
              {node.labelInfo?.toLowerCase() === 'folder' ? (
                <FolderIcon color="inherit" className={classes.labelIcon} />
              ) : (
                <LockIcon color="inherit" className={classes.labelIcon} />
              )}
              <Typography variant="body1" className={classes.labelKey}>
                {item.id}
              </Typography>
            </div>

            <Typography variant="body2" className={classes.labelText}>
              {item.labelText}
            </Typography>
          </div>
        }
      >
        {Array.isArray(node.children)
          ? node.children.map((childItem) => renderChild(childItem))
          : null}
      </TreeItem>
    ));

  const renderTree = (nodes) => {
    return nodes.map((node) => {
      return (
        <AccordionFolder
          title={node}
          summaryIcon={<ExpandMoreIcon />}
          saveSecretsToFolder={saveSecretsToFolder}
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
        {renderTree(treeData)}
      </TreeView>
    </ComponentError>
  );
};
FolderTreeView.propTypes = {
  treeData: PropTypes.array,
  saveSecretsToFolder: PropTypes.func,
  secrets: PropTypes.array,
};
FolderTreeView.defaultProps = {
  treeData: [],
  saveSecretsToFolder: () => {},
  secrets: [],
};

export default FolderTreeView;
