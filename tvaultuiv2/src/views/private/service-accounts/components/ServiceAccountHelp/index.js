import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import { Collapse, Paper, makeStyles } from '@material-ui/core';
import { TitleTwo } from '../../../../../styles/GlobalStyles';

const Container = styled('div')``;

const useStyles = makeStyles((theme) => ({
  root: {
    // height: 180,
  },
  container: {
    display: 'flex',
  },
  paper: {
    margin: theme.spacing(1),
  },
}));

const ServiceAccountHelp = (props) => {
  const classes = useStyles();
  const { customStyles, children, title, elevation } = props;
  const [isCollapse, setIsCollapse] = useState(false);
  return (
    <Container classes={customStyles}>
      <TitleTwo
        extraCss="display:flex;align-items:center;"
        onClick={() => setIsCollapse(!isCollapse)}
      >
        {isCollapse ? <ChevronRightIcon /> : <KeyboardArrowDownIcon />}
        <span>{title}</span>
      </TitleTwo>
      <Collapse in={isCollapse}>
        <Paper elevation={elevation}>{children}</Paper>
      </Collapse>
    </Container>
  );
};
ServiceAccountHelp.propTypes = {
  children: PropTypes.node,
  customStyles: PropTypes.objectOf(PropTypes.object),
  title: PropTypes.string.isRequired,
  elevation: PropTypes.number,
};
ServiceAccountHelp.defaultProps = {
  children: <></>,
  customStyles: '',
  elevation: 4,
};
export default ServiceAccountHelp;
