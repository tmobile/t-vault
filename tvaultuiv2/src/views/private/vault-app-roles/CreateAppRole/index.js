import React, { useState, useEffect, useCallback, useReducer } from 'react';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import { useHistory } from 'react-router-dom';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography, InputLabel } from '@material-ui/core';
import Tooltip from '@material-ui/core/Tooltip';

import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import TextFieldComponent from '../../../../components/FormFields/TextField';
import ButtonComponent from '../../../../components/FormFields/ActionButton';
// import SelectComponent from '../../../../components/FormFields/SelectFields';
import infoIcon from '../../../../assets/info.svg';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';
import ApproleIcon from '../../../../assets/icon-approle.svg';
import leftArrowIcon from '../../../../assets/left-arrow.svg';
import mediaBreakpoints from '../../../../breakpoints';
import SnackbarComponent from '../../../../components/Snackbar';
// import AutoCompleteComponent from '../../../../components/FormFields/AutoComplete';
import LoaderSpinner from '../../../../components/Loaders/LoaderSpinner';
import apiService from '../apiService';
import { TitleThree } from '../../../../styles/GlobalStyles';

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

const extraCss = css`
  ${small} {
    font-size: 1.3rem;
  }
`;

const CreateSafeForm = styled.form`
  display: flex;
  flex-direction: column;
  margin-top: 2.8rem;
`;

const InputFieldLabelWrapper = styled.div`
  margin-bottom: 2rem;
  position: ${(props) => (props.postion ? 'relative' : '')};
  .MuiSelect-icon {
    top: auto;
    color: #000;
  }
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

const InputLabelWrap = styled.div`
  display: flex;
  justify-content: space-between;
`;

const InfoIcon = styled('img')``;

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
  arrow: {
    color: theme.palette.common.white,
  },
  tooltip: {
    backgroundColor: theme.palette.common.white,
    color: theme.palette.common.black,
    fontSize: theme.typography.subtitle2.fontSize,
  },
}));

const CreateAppRole = () => {
  const classes = useStyles();
  const [open, setOpen] = useState(true);
  const [disabledSave, setDisabledSave] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const isMobileScreen = useMediaQuery(small);
  const [appRoleError, setApproleError] = useState(false);
  const [editApprole, setEditApprole] = useState(false);
  const [allAppRoles, setAllAppRoles] = useState([]);
  const [status, setStatus] = useState({});
  const history = useHistory();

  const initialState = {
    roleName: '',
    maxTokenTtl: '',
    tokenTtl: '',
    sectetIdNumUses: '',
    tokenNumUses: '',
    secretIdTtl: '',
  };
  const reducer = (state, { field, type, value, payload }) => {
    switch (type) {
      case 'INPUT_FORM_FIELDS':
        return { ...state, [field]: value };

      case 'UPDATE_FORM_FIELDS':
        return { ...state, ...payload };

      default:
        break;
    }
  };
  const [state, dispatch] = useReducer(reducer, initialState);

  const onChange = (e) => {
    // setInputServiceName(name);
    dispatch({
      type: 'INPUT_FORM_FIELDS',
      field: e?.target?.name,
      value: e?.target?.value,
    });
  };
  const {
    roleName,
    maxTokenTtl,
    tokenTtl,
    sectetIdNumUses,
    tokenNumUses,
    secretIdTtl,
  } = state;

  useEffect(() => {}, []);

  const handleClose = () => {
    setOpen(false);
    history.goBack();
  };

  /**
   * @function validateRoleName
   * @description To check the existing rolenames
   * @param {*} e event of input handler
   */

  const validateRoleName = (name) => {
    if (allAppRoles?.includes(name)) {
      setApproleError(true);
    }
  };

  const onRoleNameChange = (e) => {
    setApproleError(false);
    validateRoleName(e.target.value);
    onChange(e);
  };
  console.log(
    'roleName',
    roleName,
    maxTokenTtl,
    tokenTtl,
    sectetIdNumUses,
    tokenNumUses,
    secretIdTtl
  );
  useEffect(() => {
    if (
      history.location.pathname === '/vault-app-roles/edit-vault-app-role' &&
      history.location.state.appRoleDetails.isEdit
    ) {
      setEditApprole(true);
      setAllAppRoles([...history.location.state.appRoleDetails.allAppRoles]);
      apiService
        .fetchAppRoleDetails(history.location.state.appRoleDetails.name)
        .then((res) => {
          setResponseType(null);
          if (res?.data?.data) {
            // setSafeDetails(res.data.data);
            // setName(res.data.data.name);
            // setDescription(res.data.data.description);
            // setOwner(res.data.data.owner);

            dispatch({
              type: 'UPDATE_FORM_FIELDS',
              payload: {
                roleName,
                maxTokenTtl,
                tokenTtl,
                sectetIdNumUses,
                tokenNumUses,
                secretIdTtl,
              },
            });
          }
        })
        .catch((err) => {
          if (err.response && err.response.data?.errors[0]) {
            setStatus({ message: err.response.data.errors[0] });
          }
          setResponseType(-1);
        });
    }
  }, [
    history,
    roleName,
    maxTokenTtl,
    tokenTtl,
    sectetIdNumUses,
    tokenNumUses,
    secretIdTtl,
  ]);

  const constructPayload = () => {
    const data = {
      bind_secret_id: true,
      policies: [],
      role_name: roleName,
      secret_id_num_uses: sectetIdNumUses,
      secret_id_ttl: secretIdTtl,
      token_max_ttl: maxTokenTtl,
      token_num_uses: tokenNumUses,
      token_ttl: tokenTtl,
    };

    return data;
  };

  const onEditApprole = () => {
    const payload = constructPayload();
    setResponseType(0);
    apiService
      .updateAppRole(payload)
      .then((res) => {
        if (res && res.status === 200) {
          setResponseType(1);
          setStatus({ status: 'success', message: res.data.messages[0] });
          setTimeout(() => {
            setOpen(false);
            history.goBack();
          }, 1000);
        }
      })
      .catch((err) => {
        if (err.response && err.response.data?.errors[0]) {
          setStatus({ status: 'failed', message: err.response.data.errors[0] });
        }
        setResponseType(-1);
      });
  };

  const onCreateApprole = () => {
    const payload = constructPayload();
    setDisabledSave(true);
    setResponseType(0);
    apiService
      .createAppRole(payload)
      .then((res) => {
        console.log('res approle', res);
        if (res && res.status === 200) {
          setResponseType(1);
          setStatus({ status: 'success', message: res.data.messages[0] });
          setTimeout(() => {
            setOpen(false);
            history.goBack();
          }, 1000);
        }
      })
      .catch((err) => {
        if (err.response && err.response.data?.errors[0]) {
          setStatus({ status: 'failed', message: err.response.data.errors[0] });
        }
        setResponseType(-1);
      });
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  const onInputBlur = (e) => {};

  const getDisabledState = () => {
    return (
      !roleName ||
      !maxTokenTtl ||
      !tokenTtl ||
      !sectetIdNumUses ||
      !tokenNumUses ||
      !secretIdTtl ||
      appRoleError
    );
  };
  return (
    <ComponentError>
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
            {responseType === 0 && <LoaderSpinner customStyle={loaderStyle} />}
            <HeaderWrapper>
              <LeftIcon
                src={leftArrowIcon}
                alt="go-back"
                onClick={() => handleClose()}
              />
              <Typography variant="h5">Create AppRole</Typography>
            </HeaderWrapper>
            <IconDescriptionWrapper>
              <SafeIcon src={ApproleIcon} alt="app-role-icon" />
              <TitleThree lineHeight="1.8rem" extraCss={extraCss} color="#ccc">
                Approles’s operate a lot like safes, but they put the aplication
                at the logical unit for sharing.
              </TitleThree>
            </IconDescriptionWrapper>
            <CreateSafeForm>
              <InputFieldLabelWrapper>
                <InputLabelWrap>
                  <InputLabel required>Role Name</InputLabel>
                  <Tooltip
                    classes={classes}
                    title="Duration in seconds after which
 the issued token can no longer
be renewed "
                    placement="top"
                    arrow
                  >
                    <div>
                      <InfoIcon src={infoIcon} alt="info-icon-role-name" />
                    </div>
                  </Tooltip>
                </InputLabelWrap>
                <TextFieldComponent
                  value={roleName}
                  placeholder="Role_name"
                  fullWidth
                  name="roleName"
                  onChange={(e) => onRoleNameChange(e)}
                  error={appRoleError}
                  helperText={
                    appRoleError ? 'Please enter minimum 3 characters' : ''
                  }
                  onInputBlur={(e) => onInputBlur(e)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper postion>
                <InputLabelWrap>
                  <InputLabel required>Token Max TTL</InputLabel>
                  <Tooltip
                    classes={classes}
                    arrow
                    title="Duration in seconds after which the issued token can no longer be renewed"
                    placement="top"
                  >
                    <div>
                      <InfoIcon src={infoIcon} alt="info-icon" />
                    </div>
                  </Tooltip>
                </InputLabelWrap>
                <TextFieldComponent
                  value={maxTokenTtl}
                  placeholder="Token Max TTL"
                  fullWidth
                  readOnly={!!editApprole}
                  name="maxTokenTtl"
                  onChange={(e) => onChange(e)}
                  // error={appRoleError}
                  helperText={
                    appRoleError ? 'Please enter minimum 3 characters' : ''
                  }
                  onInputBlur={(e) => onInputBlur(e)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabelWrap>
                  {' '}
                  <InputLabel required>Token TTL</InputLabel>
                  <Tooltip
                    classes={classes}
                    arrow
                    title="Duration in seconds after which the issued token can no longer be renewed"
                    placement="top"
                  >
                    <div>
                      <InfoIcon src={infoIcon} alt="info-icon-token" />
                    </div>
                  </Tooltip>
                </InputLabelWrap>
                <TextFieldComponent
                  value={tokenTtl}
                  placeholder="Token_TTL"
                  fullWidth
                  readOnly={!!editApprole}
                  name="tokenTtl"
                  onChange={(e) => onChange(e)}
                  // error={appRoleError}
                  helperText={
                    appRoleError ? 'Please enter minimum 3 characters' : ''
                  }
                  onInputBlur={(e) => onInputBlur(e)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabelWrap>
                  <InputLabel required>Sec ID Number Uses</InputLabel>
                  <Tooltip
                    classes={classes}
                    arrow
                    title="Duration in seconds after which the issued token can no longer be renewed"
                    placement="top"
                  >
                    <div>
                      <InfoIcon src={infoIcon} alt="info-icon-sec" />
                    </div>
                  </Tooltip>
                </InputLabelWrap>
                <TextFieldComponent
                  value={sectetIdNumUses}
                  placeholder="secret_Id_Num_Uses"
                  fullWidth
                  readOnly={!!editApprole}
                  name="sectetIdNumUses"
                  onChange={(e) => onChange(e)}
                  // error={appRoleError}
                  onInputBlur={(e) => onInputBlur(e)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabelWrap>
                  <InputLabel required>Token Number Uses</InputLabel>
                  <Tooltip
                    classes={classes}
                    arrow
                    title="Duration in seconds after which the issued token can no longer be renewed"
                    placement="top"
                  >
                    <div>
                      <InfoIcon src={infoIcon} alt="info-icon-token-uses" />
                    </div>
                  </Tooltip>
                </InputLabelWrap>
                <TextFieldComponent
                  value={tokenNumUses}
                  placeholder="token_num_uses"
                  fullWidth
                  readOnly={!!editApprole}
                  name="tokenNumUses"
                  onChange={(e) => onChange(e)}
                  // error={appRoleError}
                  onInputBlur={(e) => onInputBlur(e)}
                />
              </InputFieldLabelWrapper>
              <InputFieldLabelWrapper>
                <InputLabelWrap>
                  <InputLabel required>Secret ID TTL</InputLabel>
                  <Tooltip
                    classes={classes}
                    arrow
                    title="Duration in seconds after which the issued token can no longer be renewed"
                    placement="top"
                  >
                    <div>
                      <InfoIcon src={infoIcon} alt="info-icon-secret-id" />
                    </div>
                  </Tooltip>
                </InputLabelWrap>
                <TextFieldComponent
                  value={secretIdTtl}
                  placeholder="secret_id_ttl"
                  fullWidth
                  readOnly={!!editApprole}
                  name="secretIdTtl"
                  onChange={(e) => onChange(e)}
                  // error={appRoleError}
                  onInputBlur={(e) => onInputBlur(e)}
                />
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
              <ButtonComponent
                label={!editApprole ? 'Create' : 'Edit'}
                color="secondary"
                icon={!editApprole ? 'add' : ''}
                disabled={getDisabledState()}
                onClick={() =>
                  !editApprole ? onCreateApprole() : onEditApprole()
                }
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelSaveWrapper>
            {status.status === 'failed' && (
              <SnackbarComponent
                open
                onClose={() => onToastClose()}
                severity="error"
                icon="error"
                message={status.message || 'Something went wrong!'}
              />
            )}
            {status.status === 'success' && (
              <SnackbarComponent
                open
                onClose={() => onToastClose()}
                message={
                  status.message || 'Approle has been created successfully '
                }
              />
            )}
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

export default CreateAppRole;
