/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import PropTypes from 'prop-types';
import Fade from '@material-ui/core/Fade';
import Paper from '@material-ui/core/Paper';
// import { makeStyles } from '@material-ui/core/styles';
import Popper from '@material-ui/core/Popper';

const PopperElement = (props) => {
  const { open, anchorEl, placement, popperContent, handlePopperClick } = props;
  return (
    <Popper open={open} anchorEl={anchorEl} placement={placement} transition>
      {({ TransitionProps }) => (
        <Fade {...TransitionProps} timeout={350}>
          <Paper onClick={handlePopperClick}>{popperContent}</Paper>
        </Fade>
      )}
    </Popper>
  );
};

PopperElement.propTypes = {
  open: PropTypes.bool,
  anchorEl: PropTypes.node,
  placement: PropTypes.string,
  popperContent: PropTypes.node,
  handlePopperClick: PropTypes.func,
  children: PropTypes.node,
};
PopperElement.defaultProps = {
  open: false,
  anchorEl: <div />,
  placement: '',
  popperContent: <div />,
  handlePopperClick: () => {},
  children: <div />,
};
export default PopperElement;
