/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { debounce } from 'lodash';
import { makeStyles } from '@material-ui/core/styles';
import { useHistory } from 'react-router-dom';
import { useMatomo } from '@datapunt/matomo-tracker-react';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import TextFieldComponent from '../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../components/FormFields/ActionButton';
import SelectComponent from '../../../../components/FormFields/SelectFields';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';
import safeIcon from '../../../../assets/icon_safe.svg';
import leftArrowIcon from '../../../../assets/left-arrow.svg';
import mediaBreakpoints from '../../../../breakpoints';
import SnackbarComponent from '../../../../components/Snackbar';
import AutoCompleteComponent from '../../../../components/FormFields/AutoComplete';
import LoaderSpinner from '../../../../components/Loaders/LoaderSpinner';
import BackdropLoader from '../../../../components/Loaders/BackdropLoader';
import { validateEmail } from '../../../../services/helper-function';
import apiService from '../apiService';
import {
  TitleThree,
  RequiredCircle,
  RequiredText,
  LabelRequired,
  GlobalModalWrapper,
  RequiredWrap,
} from '../../../../styles/GlobalStyles';
import TransferSafeOwner from '../components/TransferSafeOwner';
import TypeAheadComponent from '../../../../components/TypeAheadComponent';

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
      height: 100rem;
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

const extraCss = css`
  ${small} {
    font-size: 1.3rem;
  }
`;

const CreateSafeForm = styled.form`
  display: flex;
  flex-direction: column;
  margin-top: 2.5rem;
`;

const InputFieldLabelWrapper = styled.div`
  margin-bottom: 2rem;
  position: ${(props) => (props.postion ? 'relative' : '')};
  .MuiSelect-icon {
    top: auto;
    color: #000;
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

const autoLoaderStyle = css`
  position: absolute;
  top: 3rem;
  right: 1rem;
  color: red;
`;

const useStyles = makeStyles((theme) => ({
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflowY: 'auto',
    padding: '10rem 0',
    [theme?.breakpoints?.down('xs')]: {
      alignItems: 'unset',
      justifyContent: 'unset',
      padding: '0',
      height: '100%',
    },
  },
}));

const CreateModal = (props) => {
  const { refresh } = props;
  const classes = useStyles();
  const [applicationName, setApplicationName] = useState('');
  const [allApplication, setAllApplication] = useState([]);
  const [open, setOpen] = useState(true);
  const [safeType, setSafeType] = useState('Users Safe');
  const [owner, setOwner] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [disabledSave, setDisabledSave] = useState(true);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [autoLoader, setAutoLoader] = useState(false);
  const [openModal, setOpenModal] = useState({ status: 'edit' });
  const [options, setOptions] = useState([]);
  const isMobileScreen = useMediaQuery(small);
  const [emailError, setEmailError] = useState(false);
  const [safeError, setSafeError] = useState(false);
  const [editSafe, setEditSafe] = useState(false);
  const [safeDetails, setSafeDetails] = useState({});
  const [isValidEmail, setIsValidEmail] = useState(true);
  const history = useHistory();
  const [ownerSelected, setOwnerSelected] = useState(false);

  const { trackPageView, trackEvent } = useMatomo();

  useEffect(() => {
    trackPageView();
    return () => {
      trackPageView();
    };
  }, [trackPageView]);

  useEffect(() => {
    if (
      owner?.length > 2 &&
      ownerSelected?.userEmail &&
      sessionStorage.getItem('isAdmin') !== 'false'
    ) {
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
    if (
      name === '' ||
      owner === '' ||
      description.length < 10 ||
      safeError ||
      name.length < 3 ||
      emailError ||
      applicationName === '' ||
      !isValidEmail ||
      (![...allApplication.map((item) => item.appName)].includes(
        applicationName
      ) &&
        applicationName !== '') ||
      (safeDetails.owner === owner &&
        safeDetails.description === description &&
        safeDetails.appName === applicationName)
    ) {
      setDisabledSave(true);
    } else {
      setDisabledSave(false);
    }
  }, [
    allApplication,
    name,
    description,
    owner,
    safeError,
    emailError,
    editSafe,
    safeDetails,
    isValidEmail,
    applicationName,
  ]);

  const [menu] = useState(['Users Safe', 'Shared Safe', 'Application Safe']);

  const handleClose = () => {
    if (responseType !== 0) {
      setOpen(false);
      history.goBack();
    }
  };

  useEffect(() => {
    if (sessionStorage.getItem('isAdmin') === 'false') {
      setOwner(sessionStorage.getItem('owner'));
    }
  }, []);

  useEffect(() => {
    setResponseType(0);
    apiService
      .getApplicationName()
      .then((res) => {
        setResponseType(null);
        if (res) {
          setAllApplication([...res?.data]);
        }
      })
      .catch(() => {
        setResponseType(-1);
        setOpen(false);
        history.goBack();
      });
  }, [history]);

  useEffect(() => {
    if (
      history?.location?.pathname === '/safes/edit-safe' &&
      history?.location?.state
    ) {
      setEditSafe(true);
      setResponseType(0);
      apiService
        .getSafeDetails(history.location.state.safe.path)
        .then((res) => {
          setResponseType(null);
          if (res?.data?.data) {
            setSafeDetails(res.data.data);
            setName(res.data.data.name);
            setDescription(res.data.data.description);
            if (sessionStorage.getItem('isAdmin') === 'false') {
              setOwner(sessionStorage.getItem('owner'));
            } else {
              setOwner(res.data.data.owner);
            }
            setApplicationName(res.data.data.appName || '');
            if (res.data.data.type === 'users') {
              setSafeType('Users Safe');
            } else if (res.data.data.type === 'apps') {
              setSafeType('Application Safe');
            } else {
              setSafeType('Shared Safe');
            }
            setIsValidEmail(true);
          }
        })
        .catch((err) => {
          if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
            setToastMessage(err.response.data.errors[0]);
          }
          setResponseType(-1);
        });
    }
  }, [history]);

  const constructPayload = () => {
    let value = safeType.split(' ')[0].toLowerCase();
    const obj = allApplication.find((item) => applicationName === item.appName);
    if (value === 'application') {
      value = 'apps';
    }
    const data = {
      data: {
        appName: obj.appID,
        name,
        description,
        type: '',
        owner,
      },
      path: `${value}/${name}`,
    };
    return data;
  };

  const onEditSafes = () => {
    const payload = constructPayload();
    setResponseType(0);
    apiService
      .editSafe(payload)
      .then(async (res) => {
        if (res) {
          await refresh();
          setResponseType(1);
          setToastMessage(`Safe ${name} updated successfully!`);
          setTimeout(() => {
            setOpen(false);
            history.goBack();
          }, 1000);
        }
      })
      .catch((err) => {
        if (err.response && err.response.data?.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setResponseType(-1);
      });
  };

  const onCreateSafes = () => {
    const payload = constructPayload();
    setDisabledSave(true);
    setResponseType(0);
    apiService
      .createSafe(payload)
      .then(async (res) => {
        await refresh();
        if (res) {
          setResponseType(1);
          trackEvent({ category: 'safe-creation', action: 'click-event' });
          setTimeout(() => {
            setOpen(false);
            history.goBack();
          }, 1000);
        }
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setDisabledSave(false);
        setResponseType(-1);
      });
  };

  const callSearchApi = useCallback(
    debounce(
      (value) => {
        setAutoLoader(true);
        const userNameSearch = apiService.getUserName(value);
        const emailSearch = apiService.getOwnerEmail(value);
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
          .catch(() => setAutoLoader(false));
      },
      1000,
      true
    ),
    []
  );
  const onOwnerChange = (e) => {
    if (e && e?.target?.value !== undefined) {
      setOwner(e?.target?.value);
      if (e?.target?.value !== '' && e.target.value.length > 2) {
        callSearchApi(e.target.value);
      }
    }
  };

  const onSelected = (e, val) => {
    if (val) {
      const ownerEmail = val?.split(', ')[0];
      setOwnerSelected(
        options.filter((i) => i?.userEmail?.toLowerCase() === ownerEmail)[0]
      );
      setOwner(ownerEmail);
    }
  };
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  const InputValidation = (text) => {
    if (text) {
      const res = /^[A-Za-z0-9_.-]*?[a-z0-9]$/i;
      return res.test(text);
    }
    return null;
  };

  const onSafeNameChange = (value) => {
    if (value !== '') {
      if (!InputValidation(value)) {
        setSafeError(true);
      } else {
        setSafeError(false);
      }
    } else {
      setSafeError(false);
    }
    setName(value);
  };

  useEffect(() => {
    if (ownerSelected) {
      if (validateEmail(owner)) {
        setEmailError(false);
      } else {
        setEmailError(true);
      }
    }
  }, [owner, ownerSelected]);

  const onTransferCancelClicked = () => {
    setOpenModal({ status: 'edit' });
  };

  const onTransferOwnerConfirmationClicked = (payload) => {
    setResponseType(0);
    apiService
      .transferSafeOwner(payload)
      .then(() => {
        setResponseType(1);
        setToastMessage('Safe Transfer successfully!');
        setTimeout(() => {
          setOpen(false);
          history.goBack();
        }, 1000);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setResponseType(-1);
      });
  };

  const onChangeAppilcationName = (value) => {
    setApplicationName(value);
  };

  const getName = (displayName) => {
    if (displayName?.match(/(.*)\[(.*)\]/)) {
      const lastFirstName = displayName?.match(/(.*)\[(.*)\]/)[1].split(', ');
      const finalName = `${lastFirstName[1]} ${lastFirstName[0]}`;
      const optionalDetail = displayName?.match(/(.*)\[(.*)\]/)[2];
      return `${finalName}, ${optionalDetail}`;
    }
    if (displayName?.match(/(.*), (.*)/)) {
      const lastFirstName = displayName?.split(', ');
      const finalName = `${lastFirstName[1]} ${lastFirstName[0]}`;
      return finalName;
    }
    return displayName;
  };

  return (
    <ComponentError>
      <StyledModal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes?.modal}
        open={open}
        onClose={() => handleClose()}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <Fade in={open}>
          <GlobalModalWrapper>
            {responseType === 0 && <BackdropLoader customStyle={loaderStyle} />}
            {openModal.status === 'edit' && (
              <>
                <HeaderWrapper>
                  <LeftIcon
                    src={leftArrowIcon}
                    alt="go-back"
                    onClick={() => handleClose()}
                  />
                  {editSafe ? (
                    <Typography variant="h5">Edit Safe</Typography>
                  ) : (
                    <Typography variant="h5">Create Safe</Typography>
                  )}
                </HeaderWrapper>
                <IconDescriptionWrapper>
                  <SafeIcon src={safeIcon} alt="safe-icon" />
                  <TitleThree
                    lineHeight="1.8rem"
                    extraCss={extraCss}
                    color="#ccc"
                  >
                    A Safe is a logical unit to store the secrets. All the safes
                    are created within Vault. You can control access only at the
                    safe level. As a vault administrator you can manage safes
                    but cannot view the content of the safe.
                  </TitleThree>
                </IconDescriptionWrapper>
                <CreateSafeForm>
                  <InputFieldLabelWrapper>
                    <LabelRequired>
                      <InputLabel>
                        Safe Name
                        <RequiredCircle margin="0.5rem" />
                      </InputLabel>
                      <RequiredWrap>
                        <RequiredCircle />
                        <RequiredText>Required</RequiredText>
                      </RequiredWrap>
                    </LabelRequired>
                    <TextFieldComponent
                      value={name}
                      placeholder="Safe Name- Enter min 3 characters"
                      fullWidth
                      characterLimit={40}
                      readOnly={!!editSafe}
                      name="name"
                      onChange={(e) =>
                        onSafeNameChange(e?.target?.value?.toLowerCase())
                      }
                      error={safeError}
                      helperText={
                        safeError
                          ? 'Safe name can have alphabets, numbers, dot, hyphen and underscore only, and it should not start or end with any special characters!'
                          : ''
                      }
                    />
                  </InputFieldLabelWrapper>
                  <InputFieldLabelWrapper postion>
                    <InputLabel>
                      Owner
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
                      disabled={!!editSafe}
                      name="owner"
                      onSelected={(e, val) => onSelected(e, val)}
                      onChange={(e) => onOwnerChange(e)}
                      placeholder="Search by NTID, Email or Name "
                      error={
                        emailError ||
                        (owner?.length > 2 &&
                          !isValidEmail &&
                          safeDetails.owner !== owner)
                      }
                      // onInputBlur={(e) => onInputBlur(e)}
                      helperText={
                        ((!isValidEmail && safeDetails.owner !== owner) ||
                          emailError) &&
                        sessionStorage.getItem('isAdmin') !== 'false'
                          ? 'Please enter a valid value or not available!'
                          : ''
                      }
                    />
                    {autoLoader && (
                      <LoaderSpinner customStyle={autoLoaderStyle} />
                    )}
                  </InputFieldLabelWrapper>
                  <InputFieldLabelWrapper>
                    <InputLabel>
                      Type of Safe
                      <RequiredCircle margin="0.5rem" />
                    </InputLabel>
                    <SelectComponent
                      menu={menu}
                      value={safeType}
                      readOnly={!!editSafe}
                      onChange={(e) => setSafeType(e)}
                    />
                  </InputFieldLabelWrapper>
                  <InputFieldLabelWrapper>
                    <InputLabel>
                      Application Name
                      <RequiredCircle margin="1.3rem" />
                    </InputLabel>
                    <AutoCompleteComponent
                      icon="search"
                      options={[...allApplication.map((item) => item.appName)]}
                      searchValue={applicationName}
                      classes={classes}
                      onChange={(e) =>
                        onChangeAppilcationName(e?.target?.value)
                      }
                      onSelected={(event, value) => setApplicationName(value)}
                      placeholder="Search for Application Name"
                      error={
                        applicationName !== '' &&
                        ![
                          ...allApplication.map((item) => item.appName),
                        ].includes(applicationName)
                      }
                      helperText={
                        applicationName !== '' &&
                        ![
                          ...allApplication.map((item) => item.appName),
                        ].includes(applicationName)
                          ? `Application ${applicationName} does not exist!`
                          : ''
                      }
                    />
                  </InputFieldLabelWrapper>
                  <InputFieldLabelWrapper>
                    <InputLabel>
                      Description
                      <RequiredCircle margin="0.5rem" />
                    </InputLabel>
                    <TextFieldComponent
                      multiline
                      value={description}
                      fullWidth
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="Add some details about this safe"
                    />
                    <FieldInstruction>
                      Please add a minimum of 10 characters
                    </FieldInstruction>
                  </InputFieldLabelWrapper>
                </CreateSafeForm>
                <CancelSaveWrapper>
                  <CancelButton>
                    <ButtonComponent
                      label="Cancel"
                      color="primary"
                      onClick={() => handleClose()}
                      width={isMobileScreen ? '100%' : ''}
                    />
                  </CancelButton>
                  {editSafe && (
                    <CancelButton>
                      <ButtonComponent
                        label="Transfer"
                        color="secondary"
                        onClick={() => setOpenModal({ status: 'transfer' })}
                        width={isMobileScreen ? '100%' : ''}
                      />
                    </CancelButton>
                  )}
                  <ButtonComponent
                    label={!editSafe ? 'Create' : 'Edit'}
                    color="secondary"
                    icon={!editSafe ? 'add' : ''}
                    disabled={disabledSave}
                    onClick={() =>
                      !editSafe ? onCreateSafes() : onEditSafes()
                    }
                    width={isMobileScreen ? '100%' : ''}
                  />
                </CancelSaveWrapper>
              </>
            )}
            {openModal.status === 'transfer' && (
              <TransferSafeOwner
                onTransferCancelClicked={() => onTransferCancelClicked()}
                transferData={safeDetails}
                onTransferOwnerConfirmationClicked={(data) =>
                  onTransferOwnerConfirmationClicked(data)
                }
              />
            )}
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
                  toastMessage || 'New Safe has been createtd successfully'
                }
              />
            )}
          </GlobalModalWrapper>
        </Fade>
      </StyledModal>
    </ComponentError>
  );
};
CreateModal.propTypes = { refresh: PropTypes.func };
CreateModal.defaultProps = { refresh: () => {} };
export default CreateModal;
