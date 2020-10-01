import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import { Collapse, Paper, makeStyles } from '@material-ui/core';
import { TitleThree } from '../../../../../styles/GlobalStyles';

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
const TitleCss = css`
  display: flex;
  align-items: center;
  font-weight: 600;
`;
const ServiceAccountHelp = (props) => {
  const classes = useStyles();
  const { customStyles, children, title, elevation } = props;
  const [isCollapse, setIsCollapse] = useState(false);
  return (
    <Container classes={customStyles}>
      <TitleThree
        extraCss={TitleCss}
        onClick={() => setIsCollapse(!isCollapse)}
      >
        {isCollapse ? <ChevronRightIcon /> : <KeyboardArrowDownIcon />}
        <span>{title}</span>
      </TitleThree>
      <Collapse in={isCollapse} classes={classes}>
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
  elevation: 0,
};
export default ServiceAccountHelp;
