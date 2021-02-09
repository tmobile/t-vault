import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import Backdrop from '@material-ui/core/Backdrop';
import Fade from '@material-ui/core/Fade';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import ReactHtmlParser from 'react-html-parser';
import { TitleOne, TitleTwo } from '../../styles/GlobalStyles';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../breakpoints';

const ModalWrapper = styled.div`
  background-color: ${(props) =>
    props.theme.customColor.modal.backgroundColor || '#ddd'};
  padding: 5rem 6rem;
  outline: none;
  text-align: center;
  width: ${(props) => props.size};
  display: flex;
  flex-direction: column;
  min-height: 21rem;
  ${mediaBreakpoints.smallAndMedium} {
    width: 50%;
  }
  ${mediaBreakpoints.small} {
    width: 80%;
    padding: 3rem;
  }
`;

const ButtonWrapper = styled.div`
  display: flex;
  justify-content: center;
  align-self: flex-end;
  ${mediaBreakpoints.small} {
    width: 100%;
  }
`;

const CancelButtonWrap = styled.div`
  margin-right: 0.8rem;
  ${mediaBreakpoints.small} {
    width: 100%;
  }
`;

const titleOneCss = css`
  font-weight: bold;
  font-size: 2.4rem;
  color: ${(props) => props.theme.customColor.modal.title || '#fff'};
  text-align: left;
  ${mediaBreakpoints.small} {
    font-size: 2.4rem;
  }
`;
const titleTwoCss = css`
  margin: 1.6rem 0 3rem 0;
  color: ${(props) => props.theme.customColor.modal.color || '#fff'};
  text-align: left;
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
    size,
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
          <ModalWrapper
            size={
              // eslint-disable-next-line no-nested-ternary
              size === 'large' ? '90%' : size === 'medium' ? '60%' : '35%'
            }
          >
            <TitleOne color="#e20074" extraCss={titleOneCss}>
              {title}
            </TitleOne>
            <TitleTwo extraCss={titleTwoCss}>
              {ReactHtmlParser(description)}
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
  size: PropTypes.string,
};

ConfirmationModal.defaultProps = {
  cancelButton: <div />,
  handleClose: () => {},
  size: '40%',
};

export default ConfirmationModal;
