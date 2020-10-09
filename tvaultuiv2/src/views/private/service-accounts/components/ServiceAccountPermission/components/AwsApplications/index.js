/* eslint-disable react/jsx-indent */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
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

const { small, belowLarge } = mediaBreakpoints;

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
    accountDetail,
    accountMetaData,
    fetchPermission,
    onNewAwsChange,
    newAwsApplication,
    updateToastMessage,
  } = props;

  const [editAws, setEditAws] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [response, setResponse] = useState({ status: 'loading' });

  const isMobileScreen = useMediaQuery(small);

  useEffect(() => {
    if (accountMetaData && Object.keys(accountMetaData).length !== 0) {
      if (Object.keys(accountMetaData?.response).length !== 0) {
        setResponse({ status: 'success' });
      } else if (accountMetaData.error !== '') {
        setResponse({ status: 'error' });
      }
    } else {
      setResponse({ status: '' });
    }
  }, [accountMetaData]);

  useEffect(() => {
    if (newAwsApplication) {
      setResponse({ status: 'add' });
    }
  }, [newAwsApplication]);

  const onDeleteClick = (role) => {
    setResponse({ status: 'loading' });
    const payload = {
      path: accountDetail.path,
      role,
    };
    apiService
      .deleteAwsConfiguration(payload)
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

  const onSaveClicked = (role, access) => {
    const payload = {
      access: checkAccess(access),
      rolename: role,
      svcAccName: accountDetail.name,
    };
    apiService
      .addAwsRole(payload)
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

  const onSubmit = (data, access) => {
    setResponse({ status: 'loading' });
    onNewAwsChange();
    let url = '';
    if (data.auth_type === 'iam') {
      url = '/serviceaccounts/aws/role';
    } else {
      url = '/serviceaccounts/aws/iam/role';
    }
    apiService
      .addAwsPermission(url, data)
      .then((res) => {
        updateToastMessage(1, res.data?.messages[0]);
        onSaveClicked(data.role, access);
      })
      .catch((err) => {
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  const onEditSaveClicked = (awsName, access) => {
    setResponse({ status: 'loading' });
    const payload = {
      path: `${accountDetail.path}`,
      role: awsName,
    };
    apiService
      .editAwsApplication(payload)
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

  const onCancelClicked = () => {
    setResponse({ status: 'success' });
    onNewAwsChange();
  };

  const onEditClick = (key, value) => {
    setEditAccess(value);
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
            handleSaveClick={(data, access) => onSubmit(data, access)}
            handleCancelClick={onCancelClicked}
            handleModalClose={() => onCancelClicked()}
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
          />
        )}
        {accountMetaData &&
          Object.keys(accountMetaData).length > 0 &&
          Object.keys(accountMetaData?.response).length > 0 &&
          response.status !== 'loading' &&
          response.status !== 'error' &&
          response.status !== 'edit' && (
            <>
              {accountMetaData.response['aws-roles'] &&
                Object.keys(accountMetaData.response['aws-roles']).length >
                  0 && (
                  <PermissionsList
                    list={accountMetaData.response['aws-roles']}
                    onEditClick={(key, value) => onEditClick(key, value)}
                    onDeleteClick={(key) => onDeleteClick(key)}
                  />
                )}
              {(accountMetaData.response['aws-roles'] === null ||
                !accountMetaData.response['aws-roles'] ||
                (accountMetaData.response['aws-roles'] &&
                  Object.keys(accountMetaData.response['aws-roles']).length ===
                    0)) && (
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
            description={accountMetaData.error || 'Something went wrong!'}
          />
        )}
      </>
    </ComponentError>
  );
};

AwsApplications.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  accountMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  fetchPermission: PropTypes.func.isRequired,
  newAwsApplication: PropTypes.bool.isRequired,
  onNewAwsChange: PropTypes.func.isRequired,
  updateToastMessage: PropTypes.func.isRequired,
};
export default AwsApplications;
