/* eslint-disable react/jsx-wrap-multilines */
import React from 'react';
import Radio from '@material-ui/core/Radio';
import { makeStyles } from '@material-ui/core/styles';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import PropTypes from 'prop-types';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

const useStyles = makeStyles(() => ({
  root: {
    flexDirection: 'row',
  },
  checked: {
    '&, & + $label': {
      color: '#fff',
    },
  },
  label: {},
}));

const RadioButtonComponent = (props) => {
  const { value, menu, handleChange } = props;
  const classes = useStyles();
  return (
    <ComponentError>
      <RadioGroup
        aria-label="radio"
        name="radio"
        value={value}
        onChange={handleChange}
        className={classes.root}
      >
        {menu.map((item) => (
          <FormControlLabel
            value={item}
            control={
              <Radio
                color="default"
                classes={{
                  checked: classes.checked,
                }}
              />
            }
            label={item}
            classes={{
              label: classes.label,
            }}
          />
        ))}
      </RadioGroup>
    </ComponentError>
  );
};
RadioButtonComponent.propTypes = {
  value: PropTypes.string.isRequired,
  menu: PropTypes.arrayOf(PropTypes.any).isRequired,
  handleChange: PropTypes.func.isRequired,
};

export default RadioButtonComponent;
