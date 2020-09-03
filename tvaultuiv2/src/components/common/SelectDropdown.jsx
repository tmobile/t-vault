import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import InputLabel from '@material-ui/core/InputLabel';
import MenuItem from '@material-ui/core/MenuItem';
import FormControl from '@material-ui/core/FormControl';
import Select from '@material-ui/core/Select';
// eslint-disable-next-line import/no-unresolved
import ComponentError from 'errorBoundaries/ComponentError/component-error';

const useStyles = makeStyles((theme) => ({
  formControl: {
    margin: theme.spacing(1),
    minWidth: 120,
  },
  selectEmpty: {
    marginTop: theme.spacing(2),
  },
}));

export default function SimpleSelect() {
  const classes = useStyles();
  const [safe, setSafe] = React.useState('');

  const handleChange = (event) => {
    setSafe(event.target.value);
  };

  return (
    <ComponentError>
      <div>
        <FormControl className={classes.formControl}>
          <InputLabel id="safe-select-input-label">All Safes</InputLabel>
          <Select
            labelId="safe-select-label"
            id="safe-select"
            value={safe}
            onChange={handleChange}
          >
            <MenuItem value={10}>Safe one</MenuItem>
            <MenuItem value={20}>Safe two</MenuItem>
            <MenuItem value={30}>Safe three</MenuItem>
          </Select>
        </FormControl>
      </div>
    </ComponentError>
  );
}
