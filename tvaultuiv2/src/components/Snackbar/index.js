/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import MuiAlert from '@material-ui/lab/Alert';
import { withStyles } from '@material-ui/core/styles';
import Snackbar from '@material-ui/core/Snackbar';
import Icon from '@material-ui/core/Icon';
import PropTypes from 'prop-types';

const styles = () => ({
  startIcon: {
    fontSize: '2rem',
  },
});

const Alert = (props) => {
  return <MuiAlert variant="filled" {...props} />;
};

const setIcon = (props) => {
  const { icon, classes } = props;
  return <Icon className={classes.startIcon}>{icon}</Icon>;
};

const SnackbarComponent = (props) => {
  const { open, message, onClose, severity, icon } = props;
  const vertical = 'bottom';
  const horizontal = 'right';
  return (
    <Snackbar
      open={open || false}
      autoHideDuration={6000}
      anchorOrigin={{ vertical, horizontal }}
      key={vertical + horizontal}
      onClose={onClose}
    >
      <Alert
        icon={icon && setIcon({ ...props })}
        severity={severity || 'success'}
      >
        {message}
      </Alert>
    </Snackbar>
  );
};

SnackbarComponent.propTypes = {
  open: PropTypes.bool,
  message: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
  severity: PropTypes.string,
  icon: PropTypes.string,
};

SnackbarComponent.defaultProps = {
  open: false,
  severity: 'success',
  icon: 'checked',
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
  classes: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default withStyles(styles)(SnackbarComponent);
