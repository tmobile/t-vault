/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { useHistory } from 'react-router-dom';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import KeyboardReturnIcon from '@material-ui/icons/KeyboardReturn';
import PropTypes from 'prop-types';
import ConfirmationModal from '../../../../components/ConfirmationModal';
import TextFieldComponent from '../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../components/FormFields/ActionButton';
import TextFieldSelect from '../../../../components/FormFields/TextFieldSelect';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';
import leftArrowIcon from '../../../../assets/left-arrow.svg';
import removeIcon from '../../../../assets/close.svg';
import mediaBreakpoints from '../../../../breakpoints';
import SnackbarComponent from '../../../../components/Snackbar';
import LoaderSpinner from '../../../../components/Loaders/LoaderSpinner';
import { useStateValue } from '../../../../contexts/globalState';
import apiService from '../apiService';
import PreviewCertificate from './preview';
import SwitchComponent from '../../../../components/FormFields/SwitchComponent';
import RadioButtonComponent from '../../../../components/FormFields/RadioButton';
import CertificateHeader from '../components/CertificateHeader';
import { RequiredCircle, RequiredText } from '../../../../styles/GlobalStyles';
import configData from '../../../../config/config';

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
  }
`;

const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  ${small} {
    margin-top: 1rem;
  }
`;

const LeftIcon = styled.img`
  display: none;
  ${small} {
    display: block;
    margin-right: 1.4rem;
    margin-top: 0.3rem;
  }
`;

const CreateCertificateForm = styled.form`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
  flex-direction: column;
  margin-top: 2rem;
`;

const PreviewWrap = styled.div`
  display: ${(props) => (props.showPreview ? 'block' : 'none')};
`;

const InputFieldLabelWrapper = styled.div`
  margin-bottom: 3rem;
  position: ${(props) => (props.postion ? 'relative' : '')};
  .MuiSelect-icon {
    top: auto;
    color: ${(props) => props.theme.customColor.primary.color};
  }
`;
const ContainerOwnerWrap = styled.div`
  font-size: 1.4rem;
  margin-bottom: 1rem;
  display: ${(props) => (!props.showPreview ? 'block' : 'none')};
`;

const Container = styled.div`
  margin-bottom: 0.8rem;
`;
const Owner = styled.div``;
const Label = styled.span`
  color: ${(props) => props.theme.customColor.label.color};
  margin-right: 0.3rem;
`;

const RadioWrap = styled.div`
  margin: 0 0 3rem;
`;

const Value = styled.span``;

const FieldInstruction = styled.p`
  color: ${(props) => props.theme.customColor.label.color};
  font-size: 1.3rem;
  margin-top: 1.2rem;
  margin-bottom: 0;
`;

const DNSArrayList = styled.div`
  display: flex;
  flex-wrap: wrap;
  margin-top: 1rem;
`;
const EachDns = styled.div`
  background-color: #454c5e;
  padding: 1rem;
  display: flex;
  align-items: center;
  margin: 0.3rem 0.5rem 0.3rem 0;
`;

const Name = styled.span`
  font-size: 1.4rem;
`;

const RemoveIcon = styled.img`
  width: 1.5rem;
  margin-left: 1rem;
  cursor: pointer;
`;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
  justify-content: flex-end;
  margin-top: 3rem;
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
const InputEndWrap = styled.div`
  display: flex;
`;

const InputRequiredWrap = styled.div`
  display: flex;
  justify-content: space-between;
`;

const EndingBox = styled.div`
  background-color: ${(props) =>
    props.theme.customColor.primary.backgroundColor};
  color: ${(props) => props.theme.customColor.primary.color};
  width: ${(props) => props.width};
  display: flex;
  align-items: center;
  height: 5rem;
`;

const ReturnIcon = styled.span`
  margin-left: 1rem;
  margin-top: 0.5rem;
  cursor: pointer;
`;

const loaderStyle = css`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  color: red;
  z-index: 1;
`;

const IncludeDnsWrap = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
  label {
    margin-bottom: 0rem;
  }
  > span {
    margin-right: 1rem;
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
    height: '20rem',
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

const CreateCertificates = (props) => {
  const { refresh } = props;
  const classes = useStyles();
  const [open, setOpen] = useState(true);
  const [applicationName, setApplicationName] = useState('');
  const [certName, setCertName] = useState('');
  const [disabledSave, setDisabledSave] = useState(true);
  const [dnsName, setDnsName] = useState('');
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [ownerEmail, setOwnerEmail] = useState('N/A');
  const [certificateType, setCertificateType] = useState('internal');
  const [dnsArray, setDnsArray] = useState([]);
  const [showPreview, setShowPreview] = useState(false);
  const [dnsError, setDnsError] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [errorDnsMessage, setErrorDnsMessage] = useState('');
  const [certNameError, setCertNameError] = useState(false);
  const [responseDesc, setResponseDesc] = useState('');
  const [responseTitle, setResponseTitle] = useState('');
  const [allApplication, setAllApplication] = useState([]);
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [isDns, setIsDns] = useState(false);
  const isMobileScreen = useMediaQuery(small);
  const history = useHistory();
  const [state] = useStateValue();
  const handleClose = () => {
    if (responseType !== 0) {
      setOpen(false);
      history.goBack();
    }
  };
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  useEffect(() => {
    if (allApplication?.length > 0) {
      allApplication.sort((first, sec) =>
        first.appName?.localeCompare(sec.appName)
      );
    }
  }, [allApplication]);

  useEffect(() => {
    setResponseType(0);
    if (state) {
      setResponseType(null);
      if (state.userEmail) {
        setOwnerEmail(state.userEmail);
      }
      if (state.applicationNameList?.length > 0) {
        setAllApplication([...state.applicationNameList]);
      } else if (state.applicationNameList === 'error') {
        setResponseType(-1);
        setToastMessage('Error occured while fetching the application name!');
      }
    }
  }, [state]);

  useEffect(() => {
    if (
      certName === '' ||
      applicationName === '' ||
      dnsError ||
      certNameError ||
      ownerEmail === 'N/A'
    ) {
      setDisabledSave(true);
    } else {
      setDisabledSave(false);
    }
  }, [certName, applicationName, certNameError, dnsError, ownerEmail]);

  const InputValidation = (text) => {
    if (text) {
      const res = /^[A-Za-z0-9.-]*?[a-z0-9]$/i;
      return res.test(text);
    }
    return null;
  };

  const onCertificateNameChange = (e) => {
    setCertName(e.target.value);
    const { value } = e.target;
    if (!InputValidation(e.target.value)) {
      setCertNameError(true);
      setErrorMessage(
        'Certificate name can have alphabets, numbers, . and - characters only or should not ends with special character.'
      );
    } else if (value.toLowerCase().includes('.t-mobile.com')) {
      setCertNameError(true);
      setErrorMessage('Please enter certificate name without .t-mobile.com.');
    } else {
      setCertNameError(false);
      setErrorMessage('');
    }
  };

  const checkDnsAlreadyIncluded = (val) => {
    if (dnsArray.includes(val)) {
      setDnsError(true);
      setErrorDnsMessage('Dns name already added!');
    } else {
      setDnsArray((prev) => [...prev, val.toLowerCase()]);
      setDnsName('');
      setDnsError(false);
      setErrorDnsMessage('');
    }
  };

  const onAddDnsClicked = (e) => {
    if (e.keyCode === 13) {
      const val = `${e.target.value}.t-mobile.com`;
      checkDnsAlreadyIncluded(val);
    }
  };

  const onAddDnsKeyClicked = () => {
    const val = `${dnsName}.t-mobile.com`;
    checkDnsAlreadyIncluded(val);
  };

  const onDnsNameChange = (e) => {
    setDnsName(e.target.value);
    const { value } = e.target;
    if (value && !InputValidation(value)) {
      setDnsError(true);
      setErrorDnsMessage(
        'DNS can have alphabets, numbers, . and - characters only or should not ends with special character.'
      );
    } else if (value && value.toLowerCase().includes('.t-mobile.com')) {
      setDnsError(true);
      setErrorDnsMessage('Please enter DNS without .t-mobile.com.');
    } else {
      setDnsError(false);
      setErrorDnsMessage('');
    }
  };

  const onRemoveClicked = (dns) => {
    const array = dnsArray.filter((item) => item !== dns);
    setDnsArray([...array]);
  };

  const onPreviewClicked = () => {
    setShowPreview(true);
  };

  const onCreateClicked = () => {
    const obj = allApplication.find((item) => item.appName === applicationName);
    const dnsList = [];
    dnsArray.map((item) => dnsList.push(item.replace('.t-mobile.com', '')));
    if (obj) {
      const payload = {
        appName: obj.appID,
        certOwnerEmailId: ownerEmail,
        certOwnerNTId: state.username,
        certType: certificateType.toLowerCase(),
        certificateName: certName,
        dnsList,
      };
      setResponseType(0);
      apiService
        .createCertificate(payload)
        .then(async (res) => {
          setResponseType(null);
          if (res.data.messages && res.data.messages[0]) {
            setOpenConfirmationModal(true);
            setResponseTitle('Successfull');
            setResponseDesc(res.data.messages[0]);
            await refresh();
          }
        })
        .catch((err) => {
          if (err?.response?.data?.errors && err.response.data.errors[0]) {
            setOpenConfirmationModal(true);
            setResponseTitle('Error');
            setResponseDesc(err.response.data.errors[0]);
          }
          setResponseType(null);
        });
    }
  };

  const handleCloseConfirmationModal = () => {
    setOpenConfirmationModal(false);
    handleClose();
  };

  const errorHandleClose = () => {
    setOpenConfirmationModal(false);
  };

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={handleCloseConfirmationModal}
          title={responseTitle}
          description={responseDesc}
          confirmButton={
            <ButtonComponent
              label="Close"
              color="secondary"
              onClick={() =>
                responseTitle === 'Error'
                  ? errorHandleClose()
                  : handleCloseConfirmationModal()
              }
              width={isMobileScreen ? '100%' : '38%'}
            />
          }
        />
        {!openConfirmationModal && (
          <Modal
            aria-labelledby="transition-modal-title"
            aria-describedby="transition-modal-description"
            className={classes.modal}
            open={open}
            onClose={() => handleClose()}
            closeAfterTransition
            BackdropComponent={Backdrop}
            BackdropProps={{
              timeout: 500,
            }}
          >
            <Fade in={open}>
              <ModalWrapper>
                {responseType === 0 && (
                  <LoaderSpinner customStyle={loaderStyle} />
                )}
                <HeaderWrapper>
                  <LeftIcon
                    src={leftArrowIcon}
                    alt="go-back"
                    onClick={() => handleClose()}
                  />
                  {!showPreview ? (
                    <Typography variant="h5">Create Certificate</Typography>
                  ) : (
                    <Typography variant="h5">Certificate Preview</Typography>
                  )}
                </HeaderWrapper>
                <CertificateHeader />
                <ContainerOwnerWrap showPreview={showPreview}>
                  <Container>
                    <Label>Container:</Label>
                    <Value>VenafiBin_12345</Value>
                  </Container>
                  <Owner>
                    <Label>Owner Email:</Label>
                    <Value>{ownerEmail}</Value>
                  </Owner>
                </ContainerOwnerWrap>
                <PreviewWrap showPreview={showPreview}>
                  <PreviewCertificate
                    dns={dnsArray}
                    certificateType={certificateType}
                    applicationName={applicationName}
                    owner={ownerEmail}
                    container="VenafiBin_12345"
                    certName={certName}
                    handleClose={handleClose}
                    onEditClicked={() => setShowPreview(false)}
                    onCreateClicked={() => onCreateClicked()}
                    isMobileScreen={isMobileScreen}
                    responseType={responseType}
                  />
                </PreviewWrap>
                <CreateCertificateForm showPreview={showPreview}>
                  <RadioWrap>
                    <InputRequiredWrap>
                      <InputLabel>
                        Certificate Type
                        <RequiredCircle margin="1.3rem" />
                      </InputLabel>
                      <div>
                        <RequiredCircle />
                        <RequiredText>Required</RequiredText>
                      </div>
                    </InputRequiredWrap>
                    <RadioButtonComponent
                      menu={
                        configData.SSL_EXT_CERTIFICATE
                          ? ['internal', 'external']
                          : ['internal']
                      }
                      handleChange={(e) => setCertificateType(e.target.value)}
                      value={certificateType}
                    />
                  </RadioWrap>
                  <InputFieldLabelWrapper>
                    <InputLabel>
                      Certificate Name
                      <RequiredCircle margin="1.3rem" />
                    </InputLabel>
                    <InputEndWrap>
                      <TextFieldComponent
                        value={certName}
                        placeholder="Enter a name here..."
                        fullWidth
                        name="certName"
                        error={certNameError}
                        helperText={certNameError ? errorMessage : ''}
                        onChange={(e) => {
                          onCertificateNameChange(e);
                        }}
                      />
                      <EndingBox width="14rem">.t-mobile.com</EndingBox>
                    </InputEndWrap>
                  </InputFieldLabelWrapper>
                  <InputFieldLabelWrapper postion>
                    <InputLabel>
                      Aplication Name
                      <RequiredCircle margin="1.3rem" />
                    </InputLabel>
                    <TextFieldSelect
                      menu={[...allApplication.map((item) => item.appName)]}
                      value={applicationName}
                      classes={classes}
                      handleChange={(e) => setApplicationName(e.target.value)}
                      filledText="Select application name"
                    />
                    <FieldInstruction>
                      Please provide the AD group for which read or reset
                      permission to be granted later.
                    </FieldInstruction>
                  </InputFieldLabelWrapper>
                  <IncludeDnsWrap>
                    <SwitchComponent
                      checked={isDns}
                      handleChange={(e) => setIsDns(e.target.checked)}
                      name="dns"
                    />
                    <InputLabel>Enable Additional DNS</InputLabel>
                  </IncludeDnsWrap>
                  {isDns && (
                    <InputFieldLabelWrapper>
                      <InputLabel>Add Dns</InputLabel>
                      <InputEndWrap>
                        <TextFieldComponent
                          value={dnsName}
                          placeholder="Add dns"
                          fullWidth
                          name="dnsName"
                          onChange={(e) => {
                            onDnsNameChange(e);
                          }}
                          error={dnsError}
                          helperText={dnsError ? errorDnsMessage : ''}
                          onKeyDown={(e) => onAddDnsClicked(e)}
                        />
                        <EndingBox width="17rem">
                          .t-mobile.com
                          <ReturnIcon onClick={() => onAddDnsKeyClicked()}>
                            <KeyboardReturnIcon />
                          </ReturnIcon>
                        </EndingBox>
                      </InputEndWrap>
                      <DNSArrayList>
                        {dnsArray.map((item) => {
                          return (
                            <EachDns key={item}>
                              <Name>{item}</Name>
                              <RemoveIcon
                                src={removeIcon}
                                alt="remove"
                                onClick={() => onRemoveClicked(item)}
                              />
                            </EachDns>
                          );
                        })}
                      </DNSArrayList>
                    </InputFieldLabelWrapper>
                  )}
                </CreateCertificateForm>
                <CancelSaveWrapper showPreview={showPreview}>
                  <CancelButton>
                    <ButtonComponent
                      label="Cancel"
                      color="primary"
                      onClick={() => handleClose()}
                      width={isMobileScreen ? '100%' : ''}
                    />
                  </CancelButton>
                  <ButtonComponent
                    label="Preview"
                    color="secondary"
                    disabled={disabledSave}
                    onClick={() => onPreviewClicked()}
                    width={isMobileScreen ? '100%' : ''}
                  />
                </CancelSaveWrapper>
                {responseType === -1 && (
                  <SnackbarComponent
                    open
                    onClose={() => onToastClose()}
                    severity="error"
                    icon="error"
                    message={toastMessage || 'Something went wrong!'}
                  />
                )}
              </ModalWrapper>
            </Fade>
          </Modal>
        )}
      </>
    </ComponentError>
  );
};

CreateCertificates.propTypes = {
  refresh: PropTypes.func.isRequired,
};

export default CreateCertificates;
