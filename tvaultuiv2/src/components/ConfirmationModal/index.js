import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import Backdrop from '@material-ui/core/Backdrop';
import Fade from '@material-ui/core/Fade';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { TitleOne, TitleTwo } from '../../styles/GlobalStyles';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../breakpoints';

const ModalWrapper = styled.div`
  background-color: #fff;
  padding: 4rem 3rem;
  outline: none;
  text-align: center;
`;

const ButtonWrapper = styled.div`
  display: flex;
  justify-content: center;
  margin-top: 3rem;
  ${mediaBreakpoints.small} {
    flex-direction: column;
  }
`;

const CancelButtonWrap = styled.div`
  margin-right: 1rem;
  ${mediaBreakpoints.small} {
    margin-right: 0;
    margin-bottom: 1rem;
  }
`;

const titleOneCss = css`
  margin-bottom: 1rem;
  font-weight: bold;
  font-size: 2.4rem;
`;
const titleTwoCss = css`
  margin: 3rem 0;
`;

const useStyles = makeStyles(() => ({
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
}));

const ConfirmationModal = (props) => {
  const {
    open,
    handleClose,
    title,
    description,
    confirmButton,
    cancelButton,
  } = props;
  const classes = useStyles();

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
            <TitleOne color="#e20074" extraCss={titleOneCss}>
              {title}
            </TitleOne>
            <TitleTwo color="#666" extraCss={titleTwoCss}>
              {description}
            </TitleTwo>
            <ButtonWrapper>
              {cancelButton.type !== 'div' && (
                <CancelButtonWrap>{cancelButton}</CancelButtonWrap>
              )}
              {confirmButton}
            </ButtonWrapper>
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

ConfirmationModal.propTypes = {
  open: PropTypes.bool.isRequired,
  handleClose: PropTypes.func,
  title: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  confirmButton: PropTypes.node.isRequired,
  cancelButton: PropTypes.node,
};

ConfirmationModal.defaultProps = {
  cancelButton: <div />,
  handleClose: () => {},
};

export default ConfirmationModal;
