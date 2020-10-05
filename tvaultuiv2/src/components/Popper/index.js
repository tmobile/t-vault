/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import Popover from '@material-ui/core/Popover';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';

const useStyles = makeStyles((theme) => ({
  paper: {
    padding: theme.spacing(1),
    color: theme.palette.common.white,
    backgroundColor: theme.customColor.hoverColor.list || '#151820',
  },
}));
const PopoverItemWrap = styled.div``;
const PopperElement = (props) => {
  const {
    anchorOrigin,
    transformOrigin,
    children,
    anchorEl,
    setAnchorEl,
  } = props;
  const classes = useStyles();
  const handleClose = () => {
    setAnchorEl(null);
  };
  const open = Boolean(anchorEl);
  const id = open ? 'simple-popover' : undefined;
  return (
    <ComponentError>
      <div>
        <Popover
          id={id}
          anchorOrigin={anchorOrigin}
          transformOrigin={transformOrigin}
          classes={classes}
          open={open}
          anchorEl={anchorEl}
          onClose={handleClose}
        >
          <PopoverItemWrap onClick={() => setAnchorEl(null)}>
            {children}
          </PopoverItemWrap>
        </Popover>
      </div>
    </ComponentError>
  );
};

PopperElement.propTypes = {
  anchorOrigin: PropTypes.objectOf(PropTypes.any),
  transformOrigin: PropTypes.objectOf(PropTypes.any),
  children: PropTypes.node,
  anchorEl: PropTypes.node,
  setAnchorEl: PropTypes.node,
};
PopperElement.defaultProps = {
  anchorOrigin: {},
  transformOrigin: {},
  children: <div />,
  anchorEl: <></>,
  setAnchorEl: <></>,
};
export default PopperElement;
