/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import MoreVertOutlinedIcon from '@material-ui/icons/MoreVertOutlined';
import Popover from '@material-ui/core/Popover';
import PopupState, { bindTrigger, bindPopover } from 'material-ui-popup-state';

const useStyles = makeStyles((theme) => ({
  popover: { backgroundColor: '#151820' },
  paper: {
    padding: theme.spacing(1),
    color: theme.palette.common.white,
    backgroundColor: '#151820',
  },
}));

const PopperElement = (props) => {
  const { anchorOrigin, transformOrigin, children } = props;
  const classes = useStyles();
  return (
    <PopupState variant="popover" popupId="demo-popup-popover">
      {(popupState) => (
        <div>
          <div {...bindTrigger(popupState)}>
            {' '}
            <MoreVertOutlinedIcon />
          </div>
          <Popover
            {...bindPopover(popupState)}
            anchorOrigin={anchorOrigin}
            transformOrigin={transformOrigin}
            classes={classes}
          >
            {children}
          </Popover>
        </div>
      )}
    </PopupState>
  );
};

PopperElement.propTypes = {
  open: PropTypes.bool,
  // eslint-disable-next-line react/forbid-prop-types
  anchorOrigin: PropTypes.object,
  // eslint-disable-next-line react/forbid-prop-types
  transformOrigin: PropTypes.object,
  placement: PropTypes.string,
  children: PropTypes.node,
};
PopperElement.defaultProps = {
  open: false,
  anchorOrigin: {},
  transformOrigin: {},
  placement: '',
  children: <div />,
};
export default PopperElement;
