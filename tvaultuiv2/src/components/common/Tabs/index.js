/* eslint-disable react/forbid-prop-types */
/* eslint-disable react/require-default-props */
import React from 'react';
import PropTypes from 'prop-types';

import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import Typography from '@material-ui/core/Typography';
import Box from '@material-ui/core/Box';
import styled, { css } from 'styled-components';
// eslint-disable-next-line import/no-unresolved
import { secrets } from 'mockData/secrets.json';

import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import FolderIcon from '@material-ui/icons/Folder';
import AddIcon from '@material-ui/icons/Add';
import AccordionFolder from '../AccordionFolder';
import MuiButton from '../MuiButton';
import DialogeBox from '../DialogeBox';

// styled components goes here

const WideButton = styled('div')`
  display: flex;
  justify-content: center;
  background: #f2f2f2;
  padding: 0.5em;
`;
const EmptySecretBox = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
`;
const customBtnStyles = css`
  padding: 0.2rem 1rem;
  border-radius: 0.5rem;
`;
function TabPanel(props) {
  const { children = '', value, index } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`safes-tabpanel-${index}`}
      aria-labelledby={`safe-tab-${index}`}
    >
      {value === index && (
        <Box p={3}>
          <Typography>{children}</Typography>
        </Box>
      )}
    </div>
  );
}

TabPanel.propTypes = {
  children: PropTypes.node,
  // eslint-disable-next-line react/forbid-prop-types
  // eslint-disable-next-line react/require-default-props
  // eslint-disable-next-line react/forbid-prop-types
  index: PropTypes.any.isRequired,
  value: PropTypes.any.isRequired,
};

TabPanel.defaultProps = {
  children: <div />,
  // eslint-disable-next-line react/default-props-match-prop-types
  index: PropTypes.any.isRequired,
  // eslint-disable-next-line react/default-props-match-prop-types
  value: PropTypes.any.isRequired,
};

function a11yProps(index) {
  return {
    id: `safety-tab-${index}`,
    'aria-controls': `safety-tabpanel-${index}`,
  };
}

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
    backgroundColor: theme.palette.background.paper,
  },
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
}));

export default function SelectionTabs() {
  const classes = useStyles();
  const [value, setValue] = React.useState(0);

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  return (
    <div className={classes.root}>
      <AppBar position="static" className={classes.appBar}>
        <Tabs value={value} onChange={handleChange} aria-label="safe tabs">
          {/* eslint-disable-next-line react/jsx-props-no-spreading */}
          <Tab label="Secrets" {...a11yProps(0)} />
          {/* eslint-disable-next-line react/jsx-props-no-spreading */}
          <Tab label="Permissions" {...a11yProps(1)} />
        </Tabs>
        <MuiButton
          label="ADD"
          customStyle={customBtnStyles}
          icon={<AddIcon />}
        />
      </AppBar>
      <TabPanel value={value} index={0}>
        {secrets.length > 0 ? (
          secrets.map((sec) => (
            <div key={sec}>
              <AccordionFolder
                summaryIcon={<ExpandMoreIcon />}
                title={sec.title}
                titleIcon={<FolderIcon />}
                date={sec.date}
              />
            </div>
          ))
        ) : (
          <EmptySecretBox>
            <DialogeBox
              description="add a <strong>Folder</strong> and then you will be able to add <strong>secrets</strong> to view them all here"
              actionButton={<MuiButton label="ADD" icon={<AddIcon />} />}
            />
          </EmptySecretBox>
        )}
        <WideButton>
          <span>+</span>
          <span>Create Secrets</span>
        </WideButton>
      </TabPanel>
      <TabPanel value={value} index={1}>
        Permissions
      </TabPanel>
    </div>
  );
}
