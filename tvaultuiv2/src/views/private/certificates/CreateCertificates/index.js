/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect, useCallback } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { useHistory } from 'react-router-dom';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Radio from '@material-ui/core/Radio';
import KeyboardReturnIcon from '@material-ui/icons/KeyboardReturn';
import PropTypes from 'prop-types';
import ReactHtmlParser from 'react-html-parser';
import ConfirmationModal from '../../../../components/ConfirmationModal';
import TextFieldComponent from '../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../components/FormFields/ActionButton';
import TextFieldSelect from '../../../../components/FormFields/TextFieldSelect';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';
import certIcon from '../../../../assets/cert-icon.svg';
import leftArrowIcon from '../../../../assets/left-arrow.svg';
import removeIcon from '../../../../assets/close.svg';
import mediaBreakpoints from '../../../../breakpoints';
import SnackbarComponent from '../../../../components/Snackbar';
import LoaderSpinner from '../../../../components/Loaders/LoaderSpinner';
import { useStateValue } from '../../../../contexts/globalState';
import apiService from '../apiService';
import PreviewCertificate from './preview';
import SwitchComponent from '../../../../components/FormFields/SwitchComponent';
import ServiceAccountHelp from '../../service-accounts/components/ServiceAccountHelp';
import Strings from '../../../../resources';

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
const IconDescriptionWrapper = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
  position: relative;
  margin-top: 3.2rem;
`;

const SafeIcon = styled.img`
  height: 5.7rem;
  width: 5rem;
  margin-right: 2rem;
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
    color: #000;
  }
`;
const ContainerOwnerWrap = styled.div`
  font-size: 1.4rem;
`;

const Container = styled.div``;
const Owner = styled.div``;
const Label = styled.span`
  color: #8b8ea6;
  margin-right: 0.3rem;
`;

const RadioWrap = styled.div`
  margin: 0 0 3rem;
`;

const Value = styled.span``;

const FieldInstruction = styled.p`
  color: #8b8ea6;
  font-size: 1.3rem;
  margin-top: 1.2rem;
  margin-bottom: 0.5rem;
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
  margin: 0.3rem 0.5rem;
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

const EndingBox = styled.div`
  background-color: #fff;
  color: #000;
  width: ${(props) => props.width};
  display: flex;
  align-items: center;
  height: 5rem;
  span {
    margin-left: 1rem;
    margin-top: 0.5rem;
    cursor: pointer;
  }
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
  margin-bottom: 2rem;
  label {
    margin-bottom: 0rem;
  }
  > span {
    margin-left: 1rem;
  }
`;
const InfoLine = styled('p')`
  color: ${(props) => props.theme.customColor.collapse.color};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
  strong {
    margin-right: 0.5rem;
  }
  a {
    color: ${(props) => props.theme.customColor.magenta};
  }
`;

const Span = styled.span`
  color: ${(props) => props.theme.customColor.collapse.title};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
  ${(props) => props.extraStyles}
`;
const CollapsibleContainer = styled.div``;

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
  const [certificateType, setCertificateType] = useState('Internal');
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

  const getOwnerEmail = useCallback(() => {
    apiService
      .getOwnerEmail(`${state.username}`)
      .then((res) => {
        if (res.data.data.values && res.data.data.values[0]) {
          setOwnerEmail(res.data.data.values[0].userEmail);
        }
      })
      .catch(() => {
        setResponseType(-1);
        setToastMessage('Something went wrong while fetching the owner Email!');
      });
  }, [state]);

  useEffect(() => {
    if (allApplication?.length > 0) {
      allApplication.sort((first, sec) =>
        first.appName.localeCompare(sec.appName)
      );
    }
  }, [allApplication]);

  useEffect(() => {
    async function getApplicationName() {
      try {
        const res = await apiService.getApplicationName();
        if (res) {
          if (res.data && res.data.length > 0) {
            setAllApplication([...res.data]);
            setResponseType(null);
          }
        }
      } catch {
        setResponseType(-1);
        setToastMessage(
          'Something went wrong while fetching the application name!'
        );
      }
    }
    setResponseType(0);
    getApplicationName();
    if (state.username) {
      getOwnerEmail();
    }
  }, [state, getOwnerEmail]);

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
    if (obj) {
      const payload = {
        appName: obj.appID,
        certOwnerEmailId: ownerEmail,
        certOwnerNTId: state.username,
        certType: certificateType.toLowerCase(),
        certificateName: certName,
        dnsList: dnsArray,
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
                <IconDescriptionWrapper>
                  <SafeIcon src={certIcon} alt="cert-icon" />
                  <ContainerOwnerWrap>
                    <Container>
                      <Label>Container:</Label>
                      <Value>VenafiBin_12345</Value>
                    </Container>
                    <Owner>
                      <Label>Owner Email:</Label>
                      <Value>{ownerEmail}</Value>
                    </Owner>
                  </ContainerOwnerWrap>
                </IconDescriptionWrapper>
                <PreviewWrap showPreview={showPreview}>
                  <PreviewCertificate
                    dns={dnsArray}
                    certificateType={certificateType}
                    applicationName={applicationName}
                    certName={certName}
                    handleClose={handleClose}
                    onEditClicked={() => setShowPreview(false)}
                    onCreateClicked={() => onCreateClicked()}
                    isMobileScreen={isMobileScreen}
                    responseType={responseType}
                  />
                </PreviewWrap>
                <CreateCertificateForm showPreview={showPreview}>
                  <ServiceAccountHelp
                    title="How to get started and what to know:"
                    collapseStyles="background:none"
                  >
                    <CollapsibleContainer>
                      <InfoLine>
                        <Span>
                          <strong>1:</strong>
                        </Span>
                        {ReactHtmlParser(Strings.Resources.certificateGuide1)}
                      </InfoLine>

                      <InfoLine>
                        <Span>
                          <strong>2:</strong>
                        </Span>
                        {ReactHtmlParser(Strings.Resources.certificateGuide2)}
                      </InfoLine>

                      <InfoLine>
                        <Span>
                          <strong>3:</strong>
                        </Span>
                        {ReactHtmlParser(Strings.Resources.certificateGuide3)}
                      </InfoLine>

                      <InfoLine>
                        <Span>
                          <strong>4:</strong>
                        </Span>
                        {ReactHtmlParser(Strings.Resources.certificateGuide4)}
                      </InfoLine>
                      <InfoLine>
                        <Span>
                          <strong>5:</strong>
                        </Span>
                        {ReactHtmlParser(Strings.Resources.certificateGuide5)}
                      </InfoLine>
                    </CollapsibleContainer>
                  </ServiceAccountHelp>
                  <RadioWrap>
                    <InputLabel required>Certificate Type</InputLabel>
                    <RadioGroup
                      row
                      aria-label="certificateType"
                      name="certificateType"
                      value={certificateType}
                      onChange={(e) => setCertificateType(e.target.value)}
                    >
                      <FormControlLabel
                        value="Internal"
                        control={<Radio color="default" />}
                        label="Internal"
                      />
                      <FormControlLabel
                        value="External"
                        control={<Radio color="default" />}
                        label="External"
                      />
                    </RadioGroup>
                  </RadioWrap>
                  <InputFieldLabelWrapper>
                    <InputLabel required>Certificate Name</InputLabel>
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
                  <InputFieldLabelWrapper>
                    <InputLabel required>Aplication Name</InputLabel>
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
                    <InputLabel>
                      Are additional DNS names required (SAN Certificate)?
                    </InputLabel>
                    <SwitchComponent
                      checked={isDns}
                      handleChange={(e) => setIsDns(e.target.checked)}
                      name="dns"
                    />
                  </IncludeDnsWrap>
                  {isDns && (
                    <InputFieldLabelWrapper>
                      <InputLabel>Add DNS</InputLabel>
                      <InputEndWrap>
                        <TextFieldComponent
                          value={dnsName}
                          placeholder="Add DNS"
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
                          <span onClick={() => onAddDnsKeyClicked()}>
                            <KeyboardReturnIcon />
                          </span>
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
                {responseType === 1 && (
                  <SnackbarComponent
                    open
                    onClose={() => onToastClose()}
                    message={
                      toastMessage ||
                      'New Certificate has been createtd successfully'
                    }
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
