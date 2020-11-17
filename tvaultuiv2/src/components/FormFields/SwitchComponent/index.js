import React from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import Switch from '@material-ui/core/Switch';

const AntSwitch = withStyles((theme) => ({
  root: {
    width: 28,
    height: 16,
    padding: 0,
    display: 'flex',
  },
  switchBase: {
    padding: 2,
    color: theme.palette.common.white,
    '&$checked': {
      transform: 'translateX(12px)',
      color: theme.palette.common.white,
      '& + $track': {
        opacity: 1,
        backgroundColor: theme.palette.background.main,
      },
    },
  },
  thumb: {
    width: 12,
    height: 12,
    boxShadow: 'none',
  },
  track: {
    color: theme.palette.common.white,
    borderRadius: 16 / 2,
    opacity: 1,
    backgroundColor: theme.palette.action.disabledBackground,
  },
  checked: {},
}))(Switch);

const SwitchComponent = (props) => {
  const { checked, handleChange, name } = props;
  return <AntSwitch checked={checked} onChange={handleChange} name={name} />;
};

SwitchComponent.propTypes = {
  checked: PropTypes.bool.isRequired,
  handleChange: PropTypes.func.isRequired,
  name: PropTypes.string.isRequired,
};
export default SwitchComponent;
