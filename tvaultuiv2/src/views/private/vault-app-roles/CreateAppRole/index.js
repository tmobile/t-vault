/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect, useReducer } from 'react';
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
import infoIcon from '../../../../assets/info.svg';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';
import ApproleIcon from '../../../../assets/icon-approle.svg';
import leftArrowIcon from '../../../../assets/left-arrow.svg';
import mediaBreakpoints from '../../../../breakpoints';
import SnackbarComponent from '../../../../components/Snackbar';
import { useStateValue } from '../../../../contexts/globalState';
import LoaderSpinner from '../../../../components/Loaders/LoaderSpinner';
import apiService from '../apiService';
import {
  GlobalModalWrapper,
  RequiredCircle,
  RequiredText,
  TitleThree,
} from '../../../../styles/GlobalStyles';

const { small } = mediaBreakpoints;

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
  margin-bottom: 0.5rem;
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
const RequiredInfo = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;
const Span = styled('span')`
  font-size: 1.3rem;
  color: #29bd51;
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
  arrow: {
    color: theme.palette.common.white,
  },
  tooltip: {
    backgroundColor: theme.palette.common.white,
    color: theme.palette.common.black,
    fontSize: theme.typography.subtitle2.fontSize,
  },
}));

const CreateAppRole = (props) => {
  const { refresh } = props;
  const classes = useStyles();
  const [open, setOpen] = useState(true);
  const [responseType, setResponseType] = useState(null);
  const isMobileScreen = useMediaQuery(small);
  const [appRoleError, setApproleError] = useState(null);
  const [editApprole, setEditApprole] = useState(false);
  const [numberError, setNumberError] = useState(false);
  const [allAppRoles, setAllAppRoles] = useState([]);
  const [nameAvailable, setNameAvailable] = useState(true);
  const [status, setStatus] = useState({});
  const history = useHistory();
  const [stateVal] = useStateValue();

  const admin = Boolean(stateVal.isAdmin);

  const initialState = {
    roleName: '',
    maxTokenTtl: '',
    tokenTtl: '',
    sectetIdNumUses: '',
    tokenNumUses: '',
    secretIdTtl: '',
    tokenPolicies: '',
  };
  // eslint-disable-next-line consistent-return
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
    tokenPolicies,
  } = state;

  useEffect(() => {
    setStatus({ status: 'loading' });
    apiService
      .getAppRole()
      .then((res) => {
        setStatus({});
        const appRolesArr = [];
        if (res?.data?.keys) {
          res.data.keys.map((item) => {
            const appObj = {
              name: item,
              admin,
            };
            return appRolesArr.push(appObj);
          });
        }
        setAllAppRoles([...appRolesArr]);
      })
      .catch(() => {
        setStatus({});
      });
  }, [admin]);

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
    const itemExits = allAppRoles?.filter((approle) => approle.name === name);
    if (itemExits?.length) {
      setApproleError({ error: true, type: 'role-exists' });
      setNameAvailable(false);
      return;
    }
    if (name.length < 3 || !name.match(/^[0-9a-zA-Z]*$/g)) {
      setApproleError({ error: true, type: 'invalid-role' });
      setNameAvailable(false);
      return;
    }
    setNameAvailable(true);
  };

  const onRoleNameChange = (e) => {
    setApproleError(false);
    validateRoleName(e.target.value);
    onChange(e);
  };

  const onMaxTokenChange = (e) => {
    setNumberError(false);
    if (!e?.target?.value.match(/^[0-9]*$/g)) {
      setNumberError(true);
    }
    onChange(e);
  };

  const onTokenTtlChange = (e) => {
    setNumberError(false);
    if (!e?.target?.value.match(/^[0-9]*$/g)) {
      setNumberError(true);
    }
    onChange(e);
  };

  const onSecretIdNumUseChange = (e) => {
    setNumberError(false);
    if (!e?.target?.value.match(/^[0-9]*$/g)) {
      setNumberError(true);
    }
    onChange(e);
  };

  const onTokenNumUseChange = (e) => {
    setNumberError(false);
    if (!e?.target?.value.match(/^[0-9]*$/g)) {
      setNumberError(true);
    }
    onChange(e);
  };

  const onSecretIdTtl = (e) => {
    setNumberError(false);
    if (!e?.target?.value.match(/^[0-9]*$/g)) {
      setNumberError(true);
    }
    onChange(e);
  };

  useEffect(() => {
    if (
      history.location.pathname === '/vault-app-roles/edit-vault-app-role' &&
      history.location.state.appRoleDetails.isEdit
    ) {
      setEditApprole(true);
      setResponseType(0);
      setAllAppRoles([...history.location.state.appRoleDetails.allAppRoles]);
      apiService
        .fetchAppRoleDetails(history.location.state.appRoleDetails.name)
        .then((res) => {
          setResponseType(null);
          if (res?.data?.data) {
            dispatch({
              type: 'UPDATE_FORM_FIELDS',
              payload: {
                roleName: history.location.state.appRoleDetails.name,
                maxTokenTtl: res.data.data.token_max_ttl,
                tokenTtl: res.data.data.token_ttl,
                sectetIdNumUses: res.data.data.secret_id_num_uses,
                tokenNumUses: res.data.data.token_num_uses,
                secretIdTtl: res.data.data.secret_id_ttl,
                tokenPolicies: res.data.data.token_policies.join(','),
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
  }, [history]);

  const constructPayload = () => {
    const data = {
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
      .then(async (res) => {
        if (res) {
          setResponseType(1);
          setStatus({ status: 'success', message: res.data.messages[0] });
          await refresh();
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
    setResponseType(0);
    apiService
      .createAppRole(payload)
      .then(async (res) => {
        if (res) {
          setResponseType(1);
          setStatus({ status: 'success', message: res.data.messages[0] });
          await refresh();
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
    setStatus({});
  };

  const onInputBlur = () => {};

  const getDisabledState = () => {
    return (
      !roleName ||
      !maxTokenTtl ||
      !tokenTtl ||
      !sectetIdNumUses ||
      !tokenNumUses ||
      !secretIdTtl ||
      appRoleError ||
      numberError
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
          <GlobalModalWrapper>
            {responseType === 0 && <LoaderSpinner customStyle={loaderStyle} />}
            <HeaderWrapper>
              <LeftIcon
                src={leftArrowIcon}
                alt="go-back"
                onClick={() => handleClose()}
              />
              <Typography variant="h5">
                {editApprole ? 'Edit Approle' : 'Create AppRole'}
              </Typography>
            </HeaderWrapper>
            <IconDescriptionWrapper>
              <SafeIcon src={ApproleIcon} alt="app-role-icon" />
              <TitleThree lineHeight="1.8rem" extraCss={extraCss} color="#ccc">
                Approles’s operate a lot like safes, but they put the aplication
                at the logical unit for sharing.
              </TitleThree>
            </IconDescriptionWrapper>
            <CreateSafeForm>
              <RequiredInfo>
                <RequiredCircle />
                <RequiredText>Required</RequiredText>
              </RequiredInfo>
              <InputFieldLabelWrapper>
                <InputLabelWrap>
                  <InputLabel>
                    Role Name
                    <RequiredCircle margin="0.5rem" />
                  </InputLabel>

                  <InfoIcon src={infoIcon} alt="info-icon-role-name" />
                </InputLabelWrap>

                <TextFieldComponent
                  value={roleName}
                  placeholder="Role_name"
                  fullWidth
                  readOnly={!!editApprole}
                  name="roleName"
                  onChange={(e) => onRoleNameChange(e)}
                  error={appRoleError}
                  helperText={
                    // eslint-disable-next-line no-nested-ternary
                    roleName && appRoleError?.type === 'role-exists'
                      ? 'This approle name already exists, Please take another name.'
                      : roleName && appRoleError?.type === 'invalid-role'
                      ? 'Please enter valid role name'
                      : 'Please enter minimum 3 characters'
                  }
                  onInputBlur={(e) => onInputBlur(e)}
                />

                {roleName && nameAvailable && !editApprole && (
                  <Span>Role Name Available!</Span>
                )}
              </InputFieldLabelWrapper>
              <Tooltip
                classes={classes}
                arrow
                title="Duration in seconds after which the issued token can no longer be renewed"
                placement="top"
              >
                <InputFieldLabelWrapper postion>
                  <InputLabelWrap>
                    <InputLabel>
                      Token Max TTL
                      <RequiredCircle margin="0.5rem" />
                    </InputLabel>
                    <InfoIcon src={infoIcon} alt="info-icon" />
                  </InputLabelWrap>

                  <TextFieldComponent
                    value={maxTokenTtl}
                    placeholder="Token Max TTL"
                    fullWidth
                    name="maxTokenTtl"
                    onChange={(e) => onMaxTokenChange(e)}
                    error={numberError}
                    helperText={
                      numberError
                        ? 'Please enter a valid input(value must be number)'
                        : ''
                    }
                    onInputBlur={(e) => onInputBlur(e)}
                  />
                </InputFieldLabelWrapper>
              </Tooltip>
              <Tooltip
                classes={classes}
                arrow
                title="Duration in seconds to set as a TTL for issued tokens and at renewal time"
                placement="top"
              >
                <InputFieldLabelWrapper>
                  <InputLabelWrap>
                    {' '}
                    <InputLabel>
                      Token TTL
                      <RequiredCircle margin="0.5rem" />
                    </InputLabel>
                    <InfoIcon src={infoIcon} alt="info-icon-token" />
                  </InputLabelWrap>

                  <TextFieldComponent
                    value={tokenTtl}
                    placeholder="Token_TTL"
                    fullWidth
                    name="tokenTtl"
                    onChange={(e) => onTokenTtlChange(e)}
                    error={numberError}
                    helperText={
                      numberError
                        ? 'Please enter a valid input(value must be number)'
                        : ''
                    }
                    onInputBlur={(e) => onInputBlur(e)}
                  />
                </InputFieldLabelWrapper>
              </Tooltip>
              <Tooltip
                classes={classes}
                arrow
                title="Number of times the secretID can be used to fetch a token from this approle"
                placement="top"
              >
                <InputFieldLabelWrapper>
                  <InputLabelWrap>
                    <InputLabel>
                      Sec ID Number Uses
                      <RequiredCircle margin="0.5rem" />
                    </InputLabel>

                    <InfoIcon src={infoIcon} alt="info-icon-sec" />
                  </InputLabelWrap>

                  <TextFieldComponent
                    value={sectetIdNumUses}
                    placeholder="secret_Id_Num_Uses"
                    fullWidth
                    name="sectetIdNumUses"
                    onChange={(e) => onSecretIdNumUseChange(e)}
                    error={numberError}
                    helperText={
                      numberError
                        ? 'Please enter a valid input(value must be number)'
                        : ''
                    }
                    onInputBlur={(e) => onInputBlur(e)}
                  />
                </InputFieldLabelWrapper>
              </Tooltip>
              <Tooltip
                classes={classes}
                arrow
                title="Number of times the issued token can be used"
                placement="top"
              >
                <InputFieldLabelWrapper>
                  <InputLabelWrap>
                    <InputLabel>
                      Token Number Uses
                      <RequiredCircle margin="0.5rem" />
                    </InputLabel>

                    <InfoIcon src={infoIcon} alt="info-icon-token-uses" />
                  </InputLabelWrap>
                  <TextFieldComponent
                    value={tokenNumUses}
                    placeholder="token_num_uses"
                    fullWidth
                    name="tokenNumUses"
                    onChange={(e) => onTokenNumUseChange(e)}
                    error={numberError}
                    helperText={
                      numberError
                        ? 'Please enter a valid input(value must be number)'
                        : ''
                    }
                    onInputBlur={(e) => onInputBlur(e)}
                  />
                </InputFieldLabelWrapper>
              </Tooltip>
              <Tooltip
                classes={classes}
                arrow
                title="Duration in seconds after which the issued secretID expires"
                placement="top"
              >
                <InputFieldLabelWrapper>
                  <InputLabelWrap>
                    <InputLabel>
                      Secret ID TTL
                      <RequiredCircle margin="0.5rem" />
                    </InputLabel>

                    <InfoIcon src={infoIcon} alt="info-icon-secret-id" />
                  </InputLabelWrap>
                  <TextFieldComponent
                    value={secretIdTtl}
                    placeholder="secret_id_ttl"
                    fullWidth
                    name="secretIdTtl"
                    onChange={(e) => onSecretIdTtl(e)}
                    error={numberError}
                    helperText={
                      numberError
                        ? 'Please enter a valid input(value must be number)'
                        : ''
                    }
                    onInputBlur={(e) => onInputBlur(e)}
                  />
                </InputFieldLabelWrapper>
              </Tooltip>
              {tokenPolicies && (
                <Tooltip
                  classes={classes}
                  arrow
                  title="List of permission allowed for this approle to access secrets and passwords"
                  placement="top"
                >
                  <InputFieldLabelWrapper>
                    <InputLabelWrap>
                      <InputLabel>Permissions</InputLabel>

                      <InfoIcon src={infoIcon} alt="info-icon-secret-id" />
                    </InputLabelWrap>
                    <TextFieldComponent
                      value={tokenPolicies}
                      placeholder=""
                      fullWidth
                      readOnly
                      name="tokenPolicies"
                    />
                  </InputFieldLabelWrapper>
                </Tooltip>
              )}
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
                label={!editApprole ? 'Create' : 'Update'}
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
          </GlobalModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};
CreateAppRole.propTypes = { refresh: PropTypes.func };
CreateAppRole.defaultProps = { refresh: () => {} };
export default CreateAppRole;
