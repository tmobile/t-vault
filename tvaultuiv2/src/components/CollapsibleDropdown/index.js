import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import KeyboardArrowDownIcon from '@material-ui/icons/KeyboardArrowDown';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import { Collapse, Paper, makeStyles } from '@material-ui/core';
import { TitleThree } from '../../styles/GlobalStyles';

const Container = styled('div')`
  .MuiCollapse-wrapper {
    width: 100%;
  }
  .MuiPaper-root {
    ${(props) => props.collapseStyles}
  }
  ${(props) => props.extraCss}
`;

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    margin: `${theme.spacing(1)}px 0`,
  },
}));

const CollapseDropdown = (props) => {
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
CollapseDropdown.propTypes = {
  children: PropTypes.node,
  customStyles: PropTypes.objectOf(PropTypes.any),
  titleMore: PropTypes.string.isRequired,
  titleLess: PropTypes.string,
  elevation: PropTypes.number,
  setIsAutoExpand: PropTypes.func,
  isAutoExpand: PropTypes.bool,
  collapseStyles: PropTypes.string,
  titleCss: PropTypes.arrayOf(PropTypes.any),
  extraCss: PropTypes.string,
};
CollapseDropdown.defaultProps = {
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
export default CollapseDropdown;
