import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import OnBoardForm from '../OnBoardForm';
// import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';

const Container = styled('div')``;

const OnBoardServiceAccount = (props) => {
  const { enableOnBoardForm } = props;

  return (
    <ComponentError>
      <Container>
        {enableOnBoardForm ? (
          <OnBoardForm />
        ) : (
          <div>Service accounts onboarded</div>
        )}
      </Container>
    </ComponentError>
  );
};
OnBoardServiceAccount.propTypes = { enableOnBoardForm: PropTypes.bool };
OnBoardServiceAccount.defaultProps = { enableOnBoardForm: false };
export default OnBoardServiceAccount;
