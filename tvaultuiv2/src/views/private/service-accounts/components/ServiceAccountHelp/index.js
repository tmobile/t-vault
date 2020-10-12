import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import { Collapse, Paper, makeStyles } from '@material-ui/core';
import { TitleThree } from '../../../../../styles/GlobalStyles';

const Container = styled('div')`
  .MuiCollapse-wrapper {
    width: 100%;
  }
  .MuiPaper-root {
    ${(props) => props.collapseStyles}
  }
`;

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
  },
  paper: {
    margin: theme.spacing(1),
  },
}));
const titleCss = css`
  display: flex;
  align-items: center;
  font-weight: 600;
  cursor: pointer;
`;
const ServiceAccountHelp = (props) => {
  const classes = useStyles();
  const {
    customStyles,
    children,
    title,
    elevation,
    isAutoExpand,
    setIsAutoExpand,
    collapseStyles,
  } = props;
  const [isCollapse, setIsCollapse] = useState(false);

  useEffect(() => {
    setIsCollapse(isAutoExpand);
  }, [isAutoExpand]);
  const handleCollapse = () => {
    setIsCollapse(!isCollapse);
    setIsAutoExpand(!isAutoExpand);
  };
  return (
    <Container classes={customStyles} collapseStyles={collapseStyles}>
      <TitleThree extraCss={titleCss} onClick={() => handleCollapse()}>
        {!isCollapse ? <ChevronRightIcon /> : <KeyboardArrowDownIcon />}
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
  setIsAutoExpand: PropTypes.func,
  isAutoExpand: PropTypes.bool,
  collapseStyles: PropTypes.string,
};
ServiceAccountHelp.defaultProps = {
  children: <></>,
  customStyles: '',
  elevation: 0,
  setIsAutoExpand: () => {},
  isAutoExpand: false,
  collapseStyles: '',
};
export default ServiceAccountHelp;
