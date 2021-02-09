/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useCallback, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { debounce } from 'lodash';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import leftArrowIcon from '../../../../../assets/left-arrow.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import PreviewCertificate from '../../CreateCertificates/preview';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import BackdropLoader from '../../../../../components/Loaders/BackdropLoader';
import apiService from '../../apiService';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import Strings from '../../../../../resources';
import { validateEmail } from '../../../../../services/helper-function';
import CertificateHeader from '../CertificateHeader';
import {
  InstructionText,
  GlobalModalWrapper,
  RequiredCircle,
} from '../../../../../styles/GlobalStyles';
import TypeAheadComponent from '../../../../../components/TypeAheadComponent';

const { small } = mediaBreakpoints;

const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  ${small} {
    margin-top: 1rem;
  }
`;

const StyledModal = styled(Modal)`
  @-moz-document url-prefix() {
    .MuiBackdrop-root {
      position: absolute;
      height: 130rem;
    }
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

const InputFieldLabelWrapper = styled.div`
  margin-bottom: 3rem;
  position: ${(props) => (props.postion ? 'relative' : '')};
  .MuiSelect-icon {
    top: auto;
    color: ${(props) => props.theme.customColor.primary.color};
  }
`;

const autoLoaderStyle = css`
  position: absolute;
  top: 3rem;
  right: 1rem;
  color: red;
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

const loaderStyle = css`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  color: red;
  z-index: 1;
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

const CreateCertificates = (props) => {
  const { onCloseModal, open, certificateData } = props;

  const [owner, setOwner] = useState('');
  const [ownerSelected, setOwnerSelected] = useState({});
  const [options, setOptions] = useState([]);
  const [autoLoader, setAutoLoader] = useState(false);
  const classes = useStyles();
  const isMobileScreen = useMediaQuery(small);
  const [openConfirmationModal, setOpenConfirmationModal] = useState(false);
  const [transferOwnerSuccess, setTransferOwnerSuccess] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [modalDetail, setModalDetail] = useState({
    title: '',
    description: '',
  });
  const [permission, setPermission] = useState(false);
  const [isValidEmail, setIsValidEmail] = useState(false);
  const [emailError, setEmailError] = useState(false);
  const [disabledTransfer, setDisabledTransfer] = useState(true);

  useEffect(() => {
    if (owner?.length > 2 && ownerSelected?.userEmail) {
      if (!autoLoader) {
        if (ownerSelected?.userEmail.toLowerCase() !== owner) {
          setIsValidEmail(false);
        } else {
          setIsValidEmail(true);
        }
      }
    }
  }, [owner, ownerSelected, autoLoader]);

  useEffect(() => {
    if (emailError || !isValidEmail) {
      setDisabledTransfer(true);
    } else {
      setDisabledTransfer(false);
    }
  }, [emailError, owner, isValidEmail]);

  useEffect(() => {
    if (!certificateData.certificateStatus) {
      setOpenConfirmationModal(true);
      setModalDetail({
        title: 'Certificate Status',
        description: Strings.Resources.noTransferOwnerAvailable,
      });
      setPermission(false);
    } else {
      setPermission(true);
    }
  }, [certificateData]);

  const onTransferOwnerClicked = () => {
    setResponseType(0);
    setDisabledTransfer(true);
    apiService
      .transferOwner(
        certificateData.certType,
        certificateData.certificateName,
        owner
      )
      .then((res) => {
        if (res?.data?.messages && res.data.messages[0]) {
          setModalDetail({
            title: 'Successful',
            description: res.data.messages[0],
          });
        }
        setResponseType(null);
        setOpenConfirmationModal(true);
        setTransferOwnerSuccess(true);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setModalDetail({
            title: 'Error',
            description: err.response.data.errors[0],
          });
        }
        setResponseType(null);
        setTransferOwnerSuccess(false);
        setOpenConfirmationModal(true);
      });
  };

  const callSearchApi = useCallback(
    debounce(
      (value) => {
        setAutoLoader(true);
        const userNameSearch = apiService.getUserName(value);
        const emailSearch = apiService.getOwnerTransferEmail(value);
        Promise.all([userNameSearch, emailSearch])
          .then((responses) => {
            setOptions([]);
            const array = new Set([]);
            if (responses[0]?.data?.data?.values?.length > 0) {
              responses[0].data.data.values.map((item) => {
                if (item.userName) {
                  return array.add(item);
                }
                return null;
              });
            }
            if (responses[1]?.data?.data?.values?.length > 0) {
              responses[1].data.data.values.map((item) => {
                if (item.userName) {
                  return array.add(item);
                }
                return null;
              });
            }
            setOptions([...array]);
            setAutoLoader(false);
          })
          .catch(() => {
            setAutoLoader(false);
          });
      },
      1000,
      true
    ),
    []
  );

  const onOwnerChange = (e) => {
    if (e && e?.target?.value !== undefined) {
      setOwner(e.target.value);
      if (e.target.value && e.target.value?.length > 2) {
        callSearchApi(e.target.value);
        if (validateEmail(owner)) {
          setEmailError(false);
        } else {
          setEmailError(true);
        }
      }
    }
  };

  const onSelected = (e, val) => {
    const ownerEmail = val?.split(', ')[0];
    setOwnerSelected(
      options.filter((i) => i?.userEmail?.toLowerCase() === ownerEmail)[0]
    );
    setOwner(ownerEmail);
    setEmailError(false);
  };

  const backToTransfer = () => {
    setOpenConfirmationModal(false);
  };

  const onCloseTransferModal = () => {
    if (responseType !== 0) {
      setOwner('');
      onCloseModal(transferOwnerSuccess);
      setOpenConfirmationModal(false);
    }
  };

  const closeModal = () => {
    onCloseModal(transferOwnerSuccess);
    setOpenConfirmationModal(false);
  };

  const getName = (displayName) => {
    if (displayName?.match(/(.*)\[(.*)\]/)) {
      const lastFirstName = displayName?.match(/(.*)\[(.*)\]/)[1].split(', ');
      const name = `${lastFirstName[1]} ${lastFirstName[0]}`;
      const optionalDetail = displayName?.match(/(.*)\[(.*)\]/)[2];
      return `${name}, ${optionalDetail}`;
    }
    if (displayName?.match(/(.*), (.*)/)) {
      const lastFirstName = displayName?.split(', ');
      const name = `${lastFirstName[1]} ${lastFirstName[0]}`;
      return name;
    }
    return displayName;
  };

  return (
    <ComponentError>
      <>
        <ConfirmationModal
          open={openConfirmationModal}
          handleClose={
            transferOwnerSuccess
              ? onCloseTransferModal
              : permission
              ? backToTransfer
              : closeModal
          }
          title={modalDetail.title}
          description={modalDetail.description}
          confirmButton={
            <ButtonComponent
              label="Close"
              color="secondary"
              onClick={() =>
                transferOwnerSuccess
                  ? onCloseTransferModal()
                  : permission
                  ? backToTransfer()
                  : closeModal()
              }
              width={isMobileScreen ? '100%' : '45%'}
            />
          }
        />
        {!openConfirmationModal && (
          <StyledModal
            aria-labelledby="transition-modal-title"
            aria-describedby="transition-modal-description"
            className={classes.modal}
            open={open}
            onClose={() => onCloseTransferModal()}
            closeAfterTransition
            BackdropComponent={Backdrop}
            BackdropProps={{
              timeout: 500,
            }}
          >
            <Fade in={open}>
              <GlobalModalWrapper>
                {responseType === 0 && (
                  <BackdropLoader customStyle={loaderStyle} />
                )}
                <HeaderWrapper>
                  <LeftIcon
                    src={leftArrowIcon}
                    alt="go-back"
                    onClick={() => onCloseModal()}
                  />
                  <Typography variant="h5">Transfer Ownership</Typography>
                </HeaderWrapper>
                <CertificateHeader />
                <PreviewCertificate
                  dns={certificateData.dnsNames}
                  certificateType={certificateData.certType}
                  applicationName={certificateData.applicationName}
                  certName={certificateData.certificateName}
                  owner={certificateData.certOwnerEmailId}
                  container={certificateData.containerName}
                  isEditCertificate
                />
                <InputFieldLabelWrapper postion>
                  <InputLabel>
                    New Owner
                    <RequiredCircle margin="0.5rem" />
                  </InputLabel>
                  <TypeAheadComponent
                    options={options.map(
                      (item) =>
                        `${item?.userEmail?.toLowerCase()}, ${getName(
                          item?.displayName?.toLowerCase()
                        )}, ${item?.userName?.toLowerCase()}`
                    )}
                    loader={autoLoader}
                    userInput={owner}
                    icon="search"
                    name="owner"
                    onSelected={(e, val) => onSelected(e, val)}
                    onChange={(e) => onOwnerChange(e)}
                    placeholder="Search by NTID, Email or Name "
                    error={owner?.length > 2 && (emailError || !isValidEmail)}
                    helperText={
                      owner?.length > 2 && (emailError || !isValidEmail)
                        ? 'Please enter a valid email address or not available!'
                        : ''
                    }
                    styling={{ bottom: '5rem' }}
                  />
                  <InstructionText>
                    Search the T-Mobile system to add users
                  </InstructionText>
                  {autoLoader && (
                    <LoaderSpinner customStyle={autoLoaderStyle} />
                  )}
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
                    label="Transfer"
                    color="secondary"
                    disabled={disabledTransfer}
                    onClick={() => onTransferOwnerClicked()}
                  />
                </CancelSaveWrapper>
              </GlobalModalWrapper>
            </Fade>
          </StyledModal>
        )}
      </>
    </ComponentError>
  );
};

CreateCertificates.propTypes = {
  certificateData: PropTypes.objectOf(PropTypes.any).isRequired,
  onCloseModal: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
};

export default CreateCertificates;
