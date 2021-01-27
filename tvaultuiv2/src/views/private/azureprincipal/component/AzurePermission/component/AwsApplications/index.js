/* eslint-disable react/jsx-indent */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable no-nested-ternary */
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
import apiService from '../../../../apiService';
import LoaderSpinner from '../../../../../../../components/Loaders/LoaderSpinner';
import Error from '../../../../../../../components/Error';
import AddAwsApplicationModal from '../../../../../../../components/AddAwsApplicationModal';
import EditAwsApplication from '../../../../../../../components/EditAwsApplication';
import Strings from '../../../../../../../resources';
import { checkAccess } from '../../../../../../../services/helper-function';
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
    width: 75%;
  }
  ${small} {
    width: 100%;
  }
`;

const AwsApplications = (props) => {
  const {
    azureMetaData,
    refresh,
    onNewAwsChange,
    newAwsApplication,
    updateToastMessage,
    responseStatus,
  } = props;

  const [editAws, setEditAws] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [response, setResponse] = useState({ status: 'loading' });

  const isMobileScreen = useMediaQuery(small);

  // on azure svc account meta data is available.
  useEffect(() => {
    setResponse(responseStatus);
  }, [responseStatus]);

  // When add group button is clicked.
  useEffect(() => {
    if (newAwsApplication) {
      setResponse({ status: 'add' });
    }
  }, [newAwsApplication]);

  const constructPayload = (rolename, access) => {
    const data = {
      access: checkAccess(access, 'iamsvcaccount'),
      azureSvcAccName: azureMetaData.servicePrincipalName,
      rolename,
    };
    return data;
  };

  /**
   * @function onDeleteClick
   * @description function to delete the aws configuration from the azure svc account aws
   * application list.
   * @param {role} string role of the aws configuration.
   * @param {access} string permission of the aws configuration.
   */
  const onDeleteClick = (role, access) => {
    setResponse({ status: 'loading' });
    const payload = constructPayload(role, access);
    apiService
      .deleteAwsRole(payload)
      .then(async (res) => {
        if (res && res.data?.messages && res.data?.messages[0]) {
          updateToastMessage(1, res.data.messages[0]);
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
   * @description function to save the aws configuration role to the azure svc account
   * aws application list.
   * @param {data} object payload to call api.
   */
  const onSaveClicked = (role, access) => {
    const payload = constructPayload(role, access);
    return apiService
      .addAwsRole(payload)
      .then(async (res) => {
        if (res && res.data?.messages) {
          updateToastMessage(1, res.data?.messages[0]);
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
   * @description function to save the aws configuration  to the azure svc account
   * aws application list and then call the save of role to aws application list.
   * @param {data} object payload to call api.
   */
  const onSubmit = (data, access) => {
    setResponse({ status: 'loading' });
    onNewAwsChange();
    let url = '';
    if (data.auth_type === 'iam') {
      url = '/azureserviceaccounts/aws/iam/role';
    } else {
      url = '/azureserviceaccounts/aws/role';
    }
    apiService
      .addAwsPermission(url, data)
      .then(async (res) => {
        updateToastMessage(1, res.data?.messages[0]);
        await onSaveClicked(data.role, access);
      })
      .catch((err) => {
        if (err.response?.data?.errors && err?.response?.data?.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  /**
   * @function onEditSaveClicked
   * @description function to edit the existing aws configuration.
   * @param {role} string aws configuration name to edit.
   * @param {access} string permission given to the aws configuration.
   */
  const onEditSaveClicked = (awsName, access) => {
    setResponse({ status: 'loading' });
    const payload = constructPayload(awsName, access);
    apiService
      .deleteAwsRole(payload)
      .then((res) => {
        if (res) {
          setResponse({ status: 'loading' });
          onSaveClicked(awsName, access);
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  /**
   * @function onCancelClicked
   * @description function when cancel of add aws configuration and
   * aws configuration  is called.
   */
  const onCancelClicked = () => {
    setResponse({ status: 'success' });
    onNewAwsChange();
  };

  /**
   * @function onEditClick
   * @description function to edit the existing aws configuration.
   * @param {key} key aws configuration name of  the permission.
   * @param {value} value permission given to the aws configuration.
   */
  const onEditClick = (key, value) => {
    if (value === 'write') {
      setEditAccess('rotate');
    } else {
      setEditAccess(value);
    }
    setEditAws(key);
    setResponse({ status: 'edit' });
  };

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status === 'add' && (
          <AddAwsApplicationModal
            open
            roles={azureMetaData['aws-roles']}
            handleSaveClick={(data, access) => onSubmit(data, access)}
            handleCancelClick={onCancelClicked}
            handleModalClose={() => onCancelClicked()}
            isIamAzureSvcAccount
          />
        )}
        {response.status === 'edit' && (
          <EditAwsApplication
            handleSaveClick={(awsName, access) =>
              onEditSaveClicked(awsName, access)
            }
            handleCancelClick={onCancelClicked}
            awsName={editAws}
            access={editAccess}
            isIamAzureSvcAccount
          />
        )}
        {azureMetaData && response.status === 'success' && (
          <>
            {azureMetaData['aws-roles'] &&
              Object.keys(azureMetaData['aws-roles']).length > 0 && (
                <PermissionsList
                  list={azureMetaData['aws-roles']}
                  onEditClick={(key, value) => onEditClick(key, value)}
                  onDeleteClick={(key, value) => onDeleteClick(key, value)}
                  isIamAzureSvcAccount
                />
              )}
            {(!azureMetaData['aws-roles'] ||
              Object.keys(azureMetaData['aws-roles']).length === 0) && (
              <NoDataWrapper>
                <NoData
                  imageSrc={noPermissionsIcon}
                  description={Strings.Resources.noAwsPermissionFound}
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
          <Error
            description={responseStatus.message || 'Something went wrong!'}
          />
        )}
      </>
    </ComponentError>
  );
};

AwsApplications.propTypes = {
  azureMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  refresh: PropTypes.func.isRequired,
  newAwsApplication: PropTypes.bool.isRequired,
  onNewAwsChange: PropTypes.func.isRequired,
  updateToastMessage: PropTypes.func.isRequired,
  responseStatus: PropTypes.objectOf(PropTypes.any).isRequired,
};
export default AwsApplications;
