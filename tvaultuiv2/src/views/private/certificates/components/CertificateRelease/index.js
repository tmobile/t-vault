import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import {
  GlobalModalWrapper,
  InstructionText,
  RequiredCircle,
} from '../../../../../styles/GlobalStyles';

const { small } = mediaBreakpoints;

const HeaderWrapper = styled.h3`
  font-size: 2.4rem;
  margin-bottom: 3rem;
  margin-top: 0rem;
`;

const InputFieldLabelWrapper = styled.div`
  margin-bottom: 3rem;
  margin-top: 4rem;
`;
const EachValueWrap = styled.div`
  display: flex;
  font-size: 1.4rem;
  margin: 0.5rem 0;
  p {
    margin: 0;
  }
`;
const Label = styled.p`
  color: ${(props) => props.theme.customColor.label.color};
  margin-right: 0.5rem !important;
`;

const Value = styled.p`
  text-transform: capitalize;
`;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
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

const extraCss = css`
  ${small} {
    margin: auto;
    width: 90%;
  }
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
  icon: {
    color: '#5e627c',
    fontSize: '2rem',
  },
}));

const CertificateRelease = (props) => {
  const { onCloseModal, open, certificateData, onReleaseSubmitClicked } = props;
  const classes = useStyles();
  const [reason, setReason] = useState('');

  const onSubmitClicked = () => {
    const payload = {
      name: certificateData.certificateName,
      type: certificateData.certType,
      reason,
    };
    onReleaseSubmitClicked(payload);
  };

  return (
    <ComponentError>
      <>
        <Modal
          aria-labelledby="transition-modal-title"
          aria-describedby="transition-modal-description"
          className={classes.modal}
          open={open}
          onClose={() => onCloseModal()}
          closeAfterTransition
          BackdropComponent={Backdrop}
          BackdropProps={{
            timeout: 500,
          }}
        >
          <Fade in={open}>
            <GlobalModalWrapper extraCss={extraCss}>
              <HeaderWrapper>Release Details</HeaderWrapper>
              <EachValueWrap>
                <Label>Certificate Name:</Label>
                <Value>{certificateData.certificateName}</Value>
              </EachValueWrap>
              <EachValueWrap>
                <Label>Certificate Type:</Label>
                <Value>{certificateData.certType}</Value>
              </EachValueWrap>
              <InputFieldLabelWrapper>
                <InputLabel>
                  Reason Release
                  <RequiredCircle margin="1.3rem" />
                </InputLabel>
                <TextFieldComponent
                  fullWidth
                  name="reason"
                  value={reason}
                  placeholder="Start typing here...."
                  onChange={(e) => setReason(e.target.value)}
                />
                <InstructionText>
                  Why do you need to remove this item for the system? This will
                  help us improve T-Vault.
                </InstructionText>
              </InputFieldLabelWrapper>
              <CancelSaveWrapper>
                <CancelButton>
                  <ButtonComponent
                    label="Cancel"
                    color="primary"
                    onClick={() => onCloseModal()}
                  />
                </CancelButton>
                <ButtonComponent
                  label="Release"
                  color="secondary"
                  disabled={reason.length === 0}
                  onClick={() => onSubmitClicked()}
                />
              </CancelSaveWrapper>
            </GlobalModalWrapper>
          </Fade>
        </Modal>
      </>
    </ComponentError>
  );
};

CertificateRelease.propTypes = {
  certificateData: PropTypes.objectOf(PropTypes.any).isRequired,
  onCloseModal: PropTypes.func.isRequired,
  onReleaseSubmitClicked: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
};

export default CertificateRelease;
