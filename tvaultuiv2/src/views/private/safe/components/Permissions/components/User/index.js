/* eslint-disable prettier/prettier */
/* eslint-disable import/no-unresolved */
import React, { useState, useEffect } from 'react';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import NoData from 'components/NoData';
import ButtonComponent from 'components/FormFields/ActionButton';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import noPermissionsIcon from '../../../../../../../assets/no-permissions.svg';
import AddUser from '../../../AddUser';

const NoDataWrapper = styled.section`
  display: flex;
  justify-content: center;
`;
const bgIconStyle = {
  width: '16rem',
  height: '16rem',
};
const User = (props) => {
  const { users } = props;
  const [addPermission, setAddPermission] = useState(false);

  useEffect(() => {
    console.log('users', users);
  }, []);
  // eslint-disable-next-line no-unused-vars
  const onSaveClicked = (search, radio) => {
    setAddPermission(false);
  };
  const onCancelClicked = () => {
    setAddPermission(false);
  };
  return (
    <ComponentError>
      {addPermission ? (
        <AddUser
          handleSaveClick={(search, radio) => onSaveClicked(search, radio)}
          handleCancelClick={() => onCancelClicked()}
        />
      ) : (
        <NoDataWrapper>
          <NoData
            imageSrc={noPermissionsIcon}
            description="Add <strong>Permissions</strong> to allow people, groups or aplication to access this safe"
            actionButton={(
              <ButtonComponent
                label="add"
                icon="add"
                color="secondary"
                onClick={() => setAddPermission(true)}
              />
            )}
            bgIconStyle={bgIconStyle}
            width="38%"
          />
        </NoDataWrapper>
      )}
    </ComponentError>
  );
};

User.propTypes = {
  users: PropTypes.arrayOf(PropTypes.any).isRequired,
};
export default User;
