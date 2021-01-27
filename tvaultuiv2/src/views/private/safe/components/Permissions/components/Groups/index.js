/* eslint-disable react/jsx-indent */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import { css } from 'styled-components';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../../../components/NoData';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import PermissionsList from '../../../../../../../components/PermissionsList';
import noPermissionsIcon from '../../../../../../../assets/no-permissions.svg';
import mediaBreakpoints from '../../../../../../../breakpoints';
import AddGroup from '../../../../../../../components/AddGroup';
import apiService from '../../../../apiService';
import LoaderSpinner from '../../../../../../../components/Loaders/LoaderSpinner';
import Error from '../../../../../../../components/Error';
import { NoDataWrapper } from '../../../../../../../styles/GlobalStyles';

const { small, belowLarge } = mediaBreakpoints;

const bgIconStyle = {
  width: '10rem',
  height: '10rem',
};

const customStyle = css`
  height: 100%;
`;

const noDataStyle = css`
  width: 42%;
  ${belowLarge} {
    width: 70%;
  }
  ${small} {
    width: 100%;
  }
`;

const Groups = (props) => {
  const {
    safeDetail,
    safeData,
    refresh,
    onNewGroupChange,
    newGroup,
    updateToastMessage,
  } = props;

  const [editGroup, setEditGroup] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [response, setResponse] = useState({ status: 'loading' });

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
    if (newGroup) {
      setResponse({ status: 'add' });
    }
  }, [newGroup]);

  const onDeleteClick = (groupname) => {
    setResponse({ status: 'loading' });
    const payload = {
      path: safeDetail.path,
      groupname,
    };
    apiService
      .deleteGroup(payload)
      .then(async (res) => {
        if (res && res.data?.messages && res.data?.messages[0]) {
          updateToastMessage(1, res.data.messages[0]);
          await refresh();
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
    return apiService
      .addGroup(data)
      .then(async (res) => {
        if (res && res.data?.messages) {
          updateToastMessage(1, res.data?.messages[0]);
          await refresh();
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  const onSubmit = async (group, access) => {
    const value = {
      access,
      path: `${safeDetail.path}`,
      groupname: group.toLowerCase(),
    };
    await onSaveClicked(value);
    onNewGroupChange();
  };

  const onEditSaveClicked = (groupname, access) => {
    setResponse({ status: 'loading' });
    const payload = {
      path: `${safeDetail.path}`,
      groupname,
    };
    apiService
      .deleteGroup(payload)
      .then((res) => {
        if (res) {
          setResponse({ status: 'loading' });
          onSubmit(groupname, access);
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  const onCancelClicked = () => {
    setResponse({ status: 'success' });
    onNewGroupChange();
  };

  const onEditClick = (key, value) => {
    setEditAccess(value);
    setEditGroup(key);
    setResponse({ status: 'edit' });
  };

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status === 'add' && (
          <AddGroup
            groups={safeData?.response?.groups}
            handleSaveClick={(group, access) => onSubmit(group, access)}
            handleCancelClick={onCancelClicked}
          />
        )}

        {response.status === 'edit' && (
          <AddGroup
            handleSaveClick={(group, access) =>
              onEditSaveClicked(group, access)
            }
            handleCancelClick={onCancelClicked}
            groupname={editGroup}
            access={editAccess}
          />
        )}
        {safeData &&
          Object.keys(safeData).length > 0 &&
          Object.keys(safeData?.response).length > 0 &&
          response.status === 'success' && (
            <>
              {safeData.response.groups &&
                Object.keys(safeData.response?.groups).length > 0 && (
                  <PermissionsList
                    list={safeData.response.groups}
                    onEditClick={(key, value) => onEditClick(key, value)}
                    onDeleteClick={(key) => onDeleteClick(key)}
                  />
                )}
              {(safeData.response.groups === null ||
                !safeData.response.groups ||
                (safeData.response.groups &&
                  Object.keys(safeData.response.groups).length === 0)) && (
                <NoDataWrapper>
                  <NoData
                    imageSrc={noPermissionsIcon}
                    description="No <strong>groups</strong> are given permission to access this safe,
                    add groups to access the safe"
                    actionButton={
                      // eslint-disable-next-line react/jsx-wrap-multilines
                      <ButtonComponent
                        label="add"
                        icon="add"
                        color="secondary"
                        onClick={() => setResponse({ status: 'add' })}
                        width={isMobileScreen ? '100%' : '9.4rem'}
                      />
                    }
                    bgIconStyle={bgIconStyle}
                    customStyle={noDataStyle}
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
  newGroup: PropTypes.bool.isRequired,
  onNewGroupChange: PropTypes.func.isRequired,
  updateToastMessage: PropTypes.func.isRequired,
  refresh: PropTypes.func.isRequired,
};
export default Groups;
