/* eslint-disable no-console */
/* eslint-disable react/jsx-indent */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../../../components/NoData';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import PermissionsList from '../PermissionsList';
import noPermissionsIcon from '../../../../../../../assets/no-permissions.svg';
import mediaBreakpoints from '../../../../../../../breakpoints';
import AddAppRole from '../../../AddAppRole';
import apiService from '../../../../apiService';
import LoaderSpinner from '../../../../../../../components/LoaderSpinner';
import Error from '../../../../../../../components/Error';

const { small } = mediaBreakpoints;

const NoDataWrapper = styled.section`
  display: flex;
  justify-content: center;
  width: 100%;
  height: 100%;
  p {
    ${small} {
      margin-top: 2rem;
      margin-bottom: 4rem;
      width: 75%;
    }
  }
`;

const bgIconStyle = {
  width: '10rem',
  height: '10rem',
};

const customStyle = css`
  height: 100%;
`;

const Groups = (props) => {
  const {
    safeDetail,
    safeData,
    fetchPermission,
    onNewAppRoleChange,
    newAppRole,
    updateToastMessage,
  } = props;

  const [response, setResponse] = useState({ status: 'loading' });
  const [editRole, setEditRole] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [editClicked, setEditClicked] = useState(false);

  const isMobileScreen = useMediaQuery(small);

  useEffect(() => {
    if (safeData && Object.keys(safeData).length !== 0) {
      if (Object.keys(safeData?.response).length !== 0) {
        setResponse({ status: 'success' });
      } else if (safeData.error !== '') {
        setResponse({ status: 'error' });
      }
    } else {
      setResponse({ status: '' });
    }
  }, [safeData]);

  useEffect(() => {
    if (newAppRole) {
      setResponse({ status: 'add' });
    }
  }, [newAppRole]);

  const onDeleteClick = (role) => {
    setResponse({ status: 'loading' });
    const payload = {
      path: safeDetail.path,
      role_name: role,
    };
    apiService
      .deleteAppRole(payload)
      .then((res) => {
        if (res && res.data?.messages && res.data?.messages[0]) {
          updateToastMessage(1, res.data.messages[0]);
          setResponse({ status: '' });
          fetchPermission();
        }
      })
      .catch((err) => {
        setResponse({ status: 'success' });
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
      });
  };

  const onSaveClicked = (data) => {
    setResponse({ status: 'loading' });
    apiService
      .addAppRole(data)
      .then((res) => {
        if (res && res.data?.messages) {
          updateToastMessage(1, res.data?.messages[0]);
          setResponse({ status: '' });
          fetchPermission();
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  const onSubmit = (role, access) => {
    const value = {
      access,
      path: `${safeDetail.path}`,
      role_name: role,
    };
    onSaveClicked(value);
    onNewAppRoleChange();
  };

  const onEditClick = (key, value) => {
    setEditClicked(true);
    setEditAccess(value);
    setEditRole(key);
    setResponse({ status: 'edit' });
  };

  const onCancelClicked = () => {
    setResponse({ status: 'success' });
    onNewAppRoleChange();
  };

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status === 'add' && (
          <AddAppRole
            handleSaveClick={(role, access) => onSubmit(role, access)}
            handleCancelClick={() => onCancelClicked()}
          />
        )}
        {response.status === 'edit' && (
          <AddAppRole
            handleSaveClick={(role, access) => onSubmit(role, access)}
            handleCancelClick={() => onCancelClicked()}
            access={editAccess}
            editClicked={editClicked}
            role={editRole}
          />
        )}

        {safeData &&
          Object.keys(safeData).length > 0 &&
          Object.keys(safeData?.response).length > 0 &&
          response.status === 'success' && (
            <>
              {safeData.response['app-roles'] &&
                Object.keys(safeData.response['app-roles']).length > 0 && (
                  <PermissionsList
                    list={safeData.response['app-roles']}
                    onEditClick={(key, value) => onEditClick(key, value)}
                    onDeleteClick={(key) => onDeleteClick(key)}
                  />
                )}
              {(safeData.response['app-roles'] === null ||
                !safeData.response['app-roles'] ||
                (safeData.response['app-roles'] &&
                  Object.keys(safeData.response['app-roles']).length ===
                    0)) && (
                <NoDataWrapper>
                  <NoData
                    imageSrc={noPermissionsIcon}
                    description="No approles are given permission to access this safe,
                    add approles to access the safe"
                    actionButton={
                      // eslint-disable-next-line react/jsx-wrap-multilines
                      <ButtonComponent
                        label="add"
                        icon="add"
                        color="secondary"
                        onClick={() => setResponse({ status: 'add' })}
                        width={isMobileScreen ? '100%' : '38%'}
                      />
                    }
                    bgIconStyle={bgIconStyle}
                    width={isMobileScreen ? '100%' : '38%'}
                  />
                </NoDataWrapper>
              )}
            </>
          )}
        {response.status === 'error' && (
          <Error description={safeData.error || 'Something went wrong!'} />
        )}
      </>
    </ComponentError>
  );
};

Groups.propTypes = {
  safeDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  safeData: PropTypes.objectOf(PropTypes.any).isRequired,
  fetchPermission: PropTypes.func.isRequired,
  newAppRole: PropTypes.bool.isRequired,
  onNewAppRoleChange: PropTypes.func.isRequired,
  updateToastMessage: PropTypes.func.isRequired,
};
export default Groups;
