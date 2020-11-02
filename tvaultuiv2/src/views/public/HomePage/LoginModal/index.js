import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';
import TextFieldComponent from '../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../components/FormFields/ActionButton';
import mediaBreakpoints from '../../../../breakpoints';

const { small } = mediaBreakpoints;

const ModalWrapper = styled.section`
  padding: 6rem;
  background-color: ${(props) => props.theme.palette.background.modal};
  border: none;
  outline: none;
  width: 69.6rem;
  ${small} {
    width: 90%;
    padding: 3rem;
  }
`;

const Header = styled.h3`
  font-size: 2.4rem;
  margin: 0;
`;

const Form = styled.form`
  margin: 5rem 0;
`;
const InputFieldLabelWrapper = styled.div`
  margin-bottom: 2rem;
`;

const CancelSaveWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
  ${small} {
    margin-top: 5.3rem;
  }
  button {
    ${small} {
      height: 4.5rem;
    }
  }
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${small} {
    margin-right: 1rem;
    width: 100%;
  }
`;

const useStyles = makeStyles(() => ({
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
}));

const LoginModal = (props) => {
  const { open, handleClose } = props;
  const [userName, setUserName] = useState('');
  const [password, setPassword] = useState('');
  const classes = useStyles();
  const isMobileScreen = useMediaQuery(small);

  return (
    <ComponentError>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={open}
        onClose={handleClose}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <Fade in={open}>
          <ModalWrapper>
            <Header>Sign In</Header>
            <Form>
              <InputFieldLabelWrapper>
                <InputLabel>Username</InputLabel>
                <TextFieldComponent
                  value={userName}
                  placeholder="UserName"
                  fullWidth
                  name="userName"
                  onChange={(e) => setUserName(e.target.value)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabel>Password</InputLabel>
                <TextFieldComponent
                  value={password}
                  placeholder="Password"
                  fullWidth
                  name="password"
                  type="password"
                  onChange={(e) => setPassword(e.target.value)}
                />
              </InputFieldLabelWrapper>
            </Form>
            <CancelSaveWrapper>
              <CancelButton>
                <ButtonComponent
                  label="Cancel"
                  color="primary"
                  onClick={() => handleClose()}
                  width={isMobileScreen ? '100%' : ''}
                />
              </CancelButton>
              <ButtonComponent
                label="Sign In"
                color="secondary"
                onClick={() => {}}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelSaveWrapper>
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

LoginModal.propTypes = {
  open: PropTypes.bool.isRequired,
  handleClose: PropTypes.func.isRequired,
};

export default LoginModal;
