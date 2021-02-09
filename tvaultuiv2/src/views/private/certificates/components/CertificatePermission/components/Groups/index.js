/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-indent */
import React, { useState, useEffect } from 'react';
import { css } from 'styled-components';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../../../components/NoData';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import noPermissionsIcon from '../../../../../../../assets/no-permissions.svg';
import mediaBreakpoints from '../../../../../../../breakpoints';
import AddGroup from '../../../../../../../components/AddGroup';
import apiService from '../../../../apiService';
import LoaderSpinner from '../../../../../../../components/Loaders/LoaderSpinner';
import PermissionsList from '../../../../../../../components/PermissionsList';
import Strings from '../../../../../../../resources';
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
    certificateMetaData,
    newGroup,
    onNewGroupChange,
    updateToastMessage,
    responseStatus,
    refresh,
  } = props;

  const [editGroup, setEditGroup] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [response, setResponse] = useState({ status: 'loading' });
  const isMobileScreen = useMediaQuery(small);

  // on certificate meta data is available.
  useEffect(() => {
    setResponse({ status: responseStatus });
  }, [responseStatus]);

  // When add permission button is clicked.
  useEffect(() => {
    if (newGroup) {
      setResponse({ status: 'add' });
    }
  }, [newGroup]);

  const constructPayload = (groupname, access) => {
    const data = {
      access,
      certType: certificateMetaData.certType,
      certificateName: certificateMetaData.certificateName,
      groupname,
    };
    return data;
  };

  /**
   * @function onDeleteClick
   * @description function to delete the group from the certificate groups list.
   * @param {groupname} string groupname of the group.
   * @param {access} string permission of the group.
   */
  const onDeleteClick = async (groupname, access) => {
    setResponse({ status: 'loading' });
    const payload = constructPayload(groupname, access);
    apiService
      .deleteCertificateGroup(payload)
      .then(async (res) => {
        if (res && res.data?.messages && res.data.messages[0]) {
          updateToastMessage(1, res.data.messages[0]);
          setResponse({ status: '' });
          await refresh();
        }
      })
      .catch((err) => {
        setResponse({ status: 'success' });
        if (err.response?.data?.errors && err?.response?.data?.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
      });
  };

  /**
   * @function onSaveClicked
   * @description function to save the group to the certificate groups list.
   * @param {data} object payload to call api.
   */
  const onSaveClicked = (data) => {
    setResponse({ status: 'loading' });
    apiService
      .addCertificateGroup(data)
      .then(async (res) => {
        if (res && res.data?.messages) {
          updateToastMessage(1, res.data?.messages[0]);
          setResponse({ status: '' });
          await refresh();
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err?.response?.data?.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  /**
   * @function onSubmit
   * @description function structure the payload when save/edit is clicked and call save api.
   * @param {groupname} string group name of the group.
   * @param {access} string permission given to the group.
   */
  const onSubmit = async (groupname, access) => {
    const payload = constructPayload(groupname, access);
    try {
      await onSaveClicked(payload);
      onNewGroupChange();
    } catch {
      setResponse({ status: 'success' });
      updateToastMessage(-1, 'Something went wrong');
    }
  };

  /**
   * @function onEditSaveClicked
   * @description function to edit the existing group.
   * @param {groupname} string group name of the group.
   * @param {access} string permission given to the group.
   */
  const onEditSaveClicked = (groupname, access) => {
    setResponse({ status: 'loading' });
    const payload = constructPayload(groupname, access);
    apiService
      .deleteCertificateGroup(payload)
      .then(async (res) => {
        if (res) {
          setResponse({ status: 'loading' });
          await onSubmit(groupname, access);
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err?.response?.data?.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  /**
   * @function onCancelClicked
   * @description function when cancel of add group and edit group is called.
   */
  const onCancelClicked = () => {
    setResponse({ status: 'success' });
    onNewGroupChange();
  };

  useEffect(() => {
    onCancelClicked();
    // eslint-disable-next-line
  }, [certificateMetaData]);

  /**
   * @function onEditClick
   * @description function to edit the existing group.
   * @param {key} key group name of the group.
   * @param {value} value permission given to the group.
   */
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
            groups={certificateMetaData?.groups}
            handleSaveClick={(group, access) => onSubmit(group, access)}
            handleCancelClick={onCancelClicked}
            isCertificate
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
            isCertificate
          />
        )}
        {response.status === 'success' && (
          <>
            {certificateMetaData.groups &&
              Object.keys(certificateMetaData.groups).length > 0 && (
                <PermissionsList
                  list={certificateMetaData.groups}
                  onEditClick={(key, value) => onEditClick(key, value)}
                  onDeleteClick={(key, value) => onDeleteClick(key, value)}
                />
              )}
            {(!certificateMetaData.groups ||
              Object.keys(certificateMetaData.groups).length === 0) && (
              <NoDataWrapper>
                <NoData
                  imageSrc={noPermissionsIcon}
                  description={Strings.Resources.noGroupsPermissionFound}
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
      </>
    </ComponentError>
  );
};

Groups.propTypes = {
  responseStatus: PropTypes.string.isRequired,
  newGroup: PropTypes.bool.isRequired,
  onNewGroupChange: PropTypes.func.isRequired,
  certificateMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  updateToastMessage: PropTypes.func.isRequired,
  refresh: PropTypes.func.isRequired,
};
export default Groups;
