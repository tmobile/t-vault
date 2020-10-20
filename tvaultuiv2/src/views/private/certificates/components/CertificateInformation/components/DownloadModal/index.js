import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import SwitchComponent from '../../../../../../../components/FormFields/SwitchComponent';
import TextFieldComponent from '../../../../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import SelectComponent from '../../../../../../../components/FormFields/SelectFields';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../../../breakpoints';

const { small, belowLarge } = mediaBreakpoints;

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
    width: 100%;
    padding: 2rem;
    margin: 0;
    height: fit-content;
  }
`;

const Header = styled.div`
  text-align: center;
  font-size: 3rem;
`;

const PrivateKeyWrap = styled.form`
  display: flex;
  flex-direction: column;
`;

const InputFieldLabelWrapper = styled.div`
  margin-bottom: 2rem;
  position: ${(props) => (props.postion ? 'relative' : '')};
  .MuiSelect-icon {
    top: auto;
    color: #000;
  }
`;

const IncludeChainWrap = styled.div`
  display: flex;
  align-items: center;
  > span {
    margin-bottom: 7px;
    margin-left: 10px;
  }
`;

const FieldInstruction = styled.p`
  color: #8b8ea6;
  font-size: 1.3rem;
  margin-top: 1.2rem;
  margin-bottom: 0.5rem;
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

const PEMDERWrap = styled.div`
  display: flex;
  justify-content: center;
  margin: 3rem 0;
`;

const Description = styled.p`
  text-align: center;
  font-size: 1.4rem;
  margin: 2rem 0;
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${small} {
    margin-right: 1rem;
    width: 100%;
  }
`;

const NonPrivateCancel = styled.div`
  display: flex;
  justify-content: center;
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

const DownloadModal = (props) => {
  const {
    onCloseDownloadModal,
    isPrivateKey,
    openDownloadModal,
    onPemDerFormatClicked,
    certificateMetaData,
    onPrivateDownloadClicked,
  } = props;

  const classes = useStyles();

  const [formatType, setFormatType] = useState('DER-P12');
  const [password, setPassword] = useState('');
  const isMobileScreen = useMediaQuery(small);
  const [helperText] = useState('');
  const [issuerChain, setIssuerChain] = useState(false);
  const [selectItem] = useState([
    { name: 'DER-P12', value: 'pkcs12der' },
    { name: 'PER-PFX', value: 'pembundle' },
    { name: 'PEM-OPENSSL', value: 'pkcs12pem' },
  ]);

  const [menu] = useState(['DER-P12', 'PER-PFX', 'PEM-OPENSSL']);

  const onPriDownload = () => {
    let type = '';
    const obj = selectItem.find((item) => item.name === formatType);
    const payload = {
      certType: certificateMetaData.certType,
      certificateCred: password,
      certificateName: certificateMetaData.certificateName,
      format: obj.value,
      issuerChain,
    };
    if (formatType === 'PER-PFX') {
      type = 'pem';
    } else if (formatType === 'PEM-OPENSSL') {
      type = 'pfx';
    } else {
      type = 'p12';
    }
    onPrivateDownloadClicked(payload, type);
  };

  return (
    <ComponentError>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={openDownloadModal}
        onClose={() => onCloseDownloadModal()}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <Fade in={openDownloadModal}>
          <ModalWrapper>
            <Header>Download Certificate</Header>
            {isPrivateKey && (
              <PrivateKeyWrap>
                <Description>
                  Download certificate with private key.
                </Description>
                <InputFieldLabelWrapper>
                  <InputLabel required>Password</InputLabel>
                  <TextFieldComponent
                    value={password}
                    type="password"
                    placeholder="Password"
                    fullWidth
                    name="password"
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  <FieldInstruction>
                    Please enter minimum 8 characters
                  </FieldInstruction>
                </InputFieldLabelWrapper>
                <InputFieldLabelWrapper>
                  <InputLabel required>Format</InputLabel>
                  <SelectComponent
                    menu={menu}
                    value={formatType}
                    classes={classes}
                    onChange={(e) => setFormatType(e.target.value)}
                    helperText={helperText}
                  />
                </InputFieldLabelWrapper>
                <IncludeChainWrap>
                  <InputLabel>Include CA chain </InputLabel>
                  <SwitchComponent
                    checked={issuerChain}
                    handleChange={(e) => setIssuerChain(e.target.checked)}
                    name="rotate password"
                  />
                </IncludeChainWrap>
                <CancelSaveWrapper>
                  <CancelButton>
                    <ButtonComponent
                      label="Cancel"
                      color="primary"
                      onClick={() => onCloseDownloadModal()}
                      width={isMobileScreen ? '100%' : ''}
                    />
                  </CancelButton>
                  <ButtonComponent
                    label="Download"
                    color="secondary"
                    disabled={password?.length < 7}
                    onClick={() => onPriDownload()}
                    width={isMobileScreen ? '100%' : ''}
                  />
                </CancelSaveWrapper>
              </PrivateKeyWrap>
            )}
            {!isPrivateKey && (
              <>
                <Description>
                  Download certificate in PEM or DER format.
                </Description>
                <PEMDERWrap>
                  <CancelButton>
                    <ButtonComponent
                      label="PEM Format"
                      color="secondary"
                      onClick={() => onPemDerFormatClicked('pem')}
                      width={isMobileScreen ? '100%' : ''}
                    />
                  </CancelButton>
                  <ButtonComponent
                    label="DER Format"
                    color="secondary"
                    onClick={() => onPemDerFormatClicked('der')}
                    width={isMobileScreen ? '100%' : ''}
                  />
                </PEMDERWrap>
                <NonPrivateCancel>
                  <ButtonComponent
                    label="Cancel"
                    color="primary"
                    onClick={() => onCloseDownloadModal()}
                    width={isMobileScreen ? '100%' : ''}
                  />
                </NonPrivateCancel>
              </>
            )}
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

DownloadModal.propTypes = {
  onCloseDownloadModal: PropTypes.func.isRequired,
  isPrivateKey: PropTypes.bool.isRequired,
  openDownloadModal: PropTypes.bool.isRequired,
  onPemDerFormatClicked: PropTypes.func.isRequired,
  onPrivateDownloadClicked: PropTypes.func.isRequired,
  certificateMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
};
export default DownloadModal;
