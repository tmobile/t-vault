import React from 'react';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import FormControl from '@material-ui/core/FormControl';
import PropTypes from 'prop-types';
import Radio from '@material-ui/core/Radio';

const RadioPermissionComponent = (props) => {
  const { radioValue, handleRadioChange, isEdit } = props;
  return (
    <div>
      <FormControl component="fieldset">
        <RadioGroup
          row
          aria-label="permissions"
          name="permissions"
          value={radioValue}
          onChange={handleRadioChange}
        >
          <FormControlLabel
            value="read"
            control={<Radio color="default" />}
            label="Read"
          />
          {isEdit && (
            <FormControlLabel
              value="write"
              control={<Radio color="default" />}
              label="Reset"
            />
          )}
          {!isEdit && (
            <FormControlLabel
              value="reset"
              control={<Radio color="default" />}
              label="Reset"
            />
          )}

          <FormControlLabel
            value="deny"
            control={<Radio color="default" />}
            label="Deny"
          />
        </RadioGroup>
      </FormControl>
    </div>
  );
};

RadioPermissionComponent.propTypes = {
  radioValue: PropTypes.string,
  handleRadioChange: PropTypes.func,
  isEdit: PropTypes.bool,
};

RadioPermissionComponent.defaultProps = {
  radioValue: 'read',
  handleRadioChange: () => {},
  isEdit: false,
};

export default RadioPermissionComponent;
