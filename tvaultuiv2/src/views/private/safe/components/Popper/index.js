/* eslint-disable react/jsx-props-no-spreading */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import MoreVertOutlinedIcon from '@material-ui/icons/MoreVertOutlined';
import Popover from '@material-ui/core/Popover';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';

const useStyles = makeStyles((theme) => ({
  paper: {
    padding: theme.spacing(1),
    color: theme.palette.common.white,
    backgroundColor: '#2a2e3e',
  },
}));
const MoreWrap = styled.div``;
const PopoverItemWrap = styled.div``;
const PopperElement = (props) => {
  const { anchorOrigin, transformOrigin, children } = props;
  const [anchorEl, setAnchorEl] = useState(null);
  const classes = useStyles();
  const handleClose = () => {
    setAnchorEl(null);
  };
  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };
  const open = Boolean(anchorEl);
  const id = open ? 'simple-popover' : undefined;
  return (
    <ComponentError>
      <div>
        <MoreWrap role="button" onClick={(e) => handleClick(e)} tab-index={-1}>
          {' '}
          <MoreVertOutlinedIcon />
        </MoreWrap>
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
            {' '}
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
};
PopperElement.defaultProps = {
  anchorOrigin: {},
  transformOrigin: {},
  children: <div />,
};
export default PopperElement;
