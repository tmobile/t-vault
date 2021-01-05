/* eslint-disable react/forbid-prop-types */
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
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
  ${(props) => props.extraCss}
`;

const useStyles = makeStyles(() => ({
  container: {
    display: 'flex',
    margin: '8px 0',
  },
}));

const ServiceAccountHelp = (props) => {
  const classes = useStyles();
  const {
    customStyles,
    children,
    titleMore,
    titleLess,
    elevation,
    isAutoExpand,
    setIsAutoExpand,
    collapseStyles,
    titleCss,
    extraCss,
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
    <Container
      classes={customStyles}
      collapseStyles={collapseStyles}
      extraCss={extraCss}
    >
      <TitleThree extraCss={titleCss} onClick={() => handleCollapse()}>
        {!isCollapse ? <ChevronRightIcon /> : <KeyboardArrowDownIcon />}
        <span>{!isCollapse ? titleMore : titleLess}</span>
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
  titleMore: PropTypes.string.isRequired,
  titleLess: PropTypes.string,
  elevation: PropTypes.number,
  setIsAutoExpand: PropTypes.func,
  isAutoExpand: PropTypes.bool,
  collapseStyles: PropTypes.string,
  titleCss: PropTypes.any,
  extraCss: PropTypes.string,
};
ServiceAccountHelp.defaultProps = {
  children: <></>,
  customStyles: {},
  elevation: 0,
  setIsAutoExpand: () => {},
  isAutoExpand: false,
  collapseStyles: '',
  titleLess: 'View Less',
  titleCss: {},
  extraCss: '',
};
export default ServiceAccountHelp;
