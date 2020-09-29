import React from 'react';
import styled from 'styled-components';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';

const Container = styled('div')``;
const InputFieldLabelWrapper = styled('div')``;

const onBoardForm = () => {
  return (
    <Container>
      <InputFieldLabelWrapper>
        <InputLabel>Service Account Name</InputLabel>
        {/* <TextFieldComponent
          value={name}
          placeholder="Save Name"
          fullWidth
          readOnly={!!editSafe}
          name="name"
          onChange={(e) => {
            setName(e.target.value);
            setSafeError(false);
          }}
          error={safeError}
          helperText={safeError ? 'Please enter minimum 3 characters' : ''}
        /> */}
      </InputFieldLabelWrapper>
    </Container>
  );
};
onBoardForm.propTypes = {};
onBoardForm.defaultProps = {};
export default onBoardForm;
