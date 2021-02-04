import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import SelectComponent from '../../../../../../../components/FormFields/SelectFields';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../../../breakpoints';

const { small, belowLarge } = mediaBreakpoints;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
  justify-content: flex-end;
  margin-top: 2rem;
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

const ModalWrapper = styled.section`
  background-color: ${(props) => props.theme.palette.background.modal};
  padding: 5.5rem 6rem 6rem 6rem;
  border: none;
  outline: none;
  width: 69.6rem;
  margin: auto 0;
  display: flex;
  flex-direction: column;
  position: relative;
  ${belowLarge} {
    padding: 2.7rem 5rem 3.2rem 5rem;
    width: 57.2rem;
  }
  ${small} {
    width: 90%;
    padding: 2rem;
    margin: auto;
  }
`;

const SelectFieldWrap = styled.div`
  margin: 4rem 0;
`;

const useStyles = makeStyles((theme) => ({
  select: {
    '&.MuiFilledInput-root.Mui-focused': {
      backgroundColor: '#fff',
    },
  },
  dropdownStyle: {
    backgroundColor: '#fff',
  },
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflowY: 'auto',
    padding: '10rem 0',
    [theme.breakpoints.down('xs')]: {
      alignItems: 'unset',
      justifyContent: 'unset',
      padding: '0',
      height: '100%',
    },
  },
}));

const RevokeCertificate = (props) => {
  const {
    revokeModalOpen,
    handleRevokeModalClose,
    revokeMenu,
    onRevokeConfirm,
    allRevokeReason,
    isMobileScreen,
  } = props;

  const [selectedValue, setSelectedValue] = useState('');
  const classes = useStyles();
  const onRevoke = () => {
    const obj = allRevokeReason.find(
      (item) => item.displayName === selectedValue
    );
    if (obj) {
      const payload = {
        reason: obj.reason,
      };
      onRevokeConfirm(payload);
    }
  };
  return (
    <ComponentError>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={revokeModalOpen}
        onClose={handleRevokeModalClose}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <Fade in={revokeModalOpen}>
          <ModalWrapper>
            <Typography variant="h5">Revocation reasons</Typography>
            <SelectFieldWrap>
              <SelectComponent
                classes={classes}
                value={selectedValue}
                menu={revokeMenu}
                filledText="Select revocation reason"
                handleChange={(e) => setSelectedValue(e.target.value)}
              />
            </SelectFieldWrap>

            <CancelSaveWrapper>
              <CancelButton>
                <ButtonComponent
                  label="Cancel"
                  color="primary"
                  onClick={handleRevokeModalClose}
                  width={isMobileScreen ? '100%' : ''}
                />
              </CancelButton>
              <ButtonComponent
                label="Revoke"
                color="secondary"
                disabled={selectedValue === ''}
                width={isMobileScreen ? '100%' : ''}
                onClick={() => onRevoke()}
              />
            </CancelSaveWrapper>
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

RevokeCertificate.propTypes = {
  revokeModalOpen: PropTypes.bool,
  handleRevokeModalClose: PropTypes.func,
  revokeMenu: PropTypes.arrayOf(PropTypes.any),
  onRevokeConfirm: PropTypes.func,
  allRevokeReason: PropTypes.arrayOf(PropTypes.any),
  isMobileScreen: PropTypes.bool,
};

RevokeCertificate.defaultProps = {
  revokeModalOpen: false,
  handleRevokeModalClose: () => {},
  revokeMenu: [],
  onRevokeConfirm: () => {},
  allRevokeReason: [],
  isMobileScreen: false,
};

export default RevokeCertificate;
