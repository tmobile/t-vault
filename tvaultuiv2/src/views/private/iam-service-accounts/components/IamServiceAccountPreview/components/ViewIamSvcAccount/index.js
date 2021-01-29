import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { Typography } from '@material-ui/core';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import svcHeaderBgimg from '../../../../../../../assets/icon-service-account.svg';
import mediaBreakpoints from '../../../../../../../breakpoints';
import leftArrowIcon from '../../../../../../../assets/left-arrow.svg';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import CollapsibleDropdown from '../../../../../../../components/CollapsibleDropdown';

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

const PreviewWrap = styled.div``;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
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
  margin-left: 0.8rem;
  ${small} {
    margin-left: 1rem;
    width: 100%;
  }
`;
const Label = styled.p`
  font-size: 1.3rem;
  color: ${(props) => props.theme.customColor.label.color};
  margin-bottom: 0.9rem;
`;

const Value = styled.p`
  font-size: 1.8rem;
`;

const EachDetail = styled.div`
  margin-bottom: 3rem;
  p {
    margin: 0;
  }
`;

const SvcIcon = styled.img`
  height: 5.7rem;
  width: 5rem;
  margin-right: 2rem;
`;

const ViewMoreStyles = css`
  display: flex;
  align-items: center;
  font-weight: 600;
  cursor: pointer;
  margin-left: 5rem;
`;

const HeaderInfoWrapper = styled.div`
  display: flex;
  align-items: center;
`;

const InfoLine = styled('p')`
  color: ${(props) => props.theme.customColor.collapse.color};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
`;

const InfoContainer = styled.div`
  padding: 1rem 0;
`;
const Span = styled('span')`
  color: ${(props) => props.theme.customColor.collapse.title};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
  ${(props) => props.extraStyles}
`;
const CollapsibleContainer = styled('div')``;

const ViewIamSvcAccountDetails = (props) => {
  const {
    iamSvcAccountData,
    isMobileScreen,
    isRotateSecret,
    isActivateIamSvcAcc,
    handleCloseModal,
    viewAccountData,
  } = props;
  const onRotateClicked = () => {
    isRotateSecret(true);
  };

  const onActivateClicked = () => {
    isActivateIamSvcAcc(true);
  };

  const onCancelViewDetails = () => {
    handleCloseModal();
  };

  return (
    <ComponentError>
      <ModalWrapper>
        <HeaderWrapper>
          <LeftIcon
            src={leftArrowIcon}
            alt="go-back"
            onClick={() => onCancelViewDetails(false)}
          />
          <Typography variant="h5">View IAM Service Account</Typography>
        </HeaderWrapper>
        <PreviewWrap>
          <InfoContainer>
            <HeaderInfoWrapper>
              <SvcIcon alt="safe-icon" src={svcHeaderBgimg} />
              <InfoLine>
                T-Vault can be used to manage to read and rotate the secrets of
                IAM service accounts. In order to self-service your IAM service
                accounts there is a three-step process to onboard the account
                into T-Vault.
              </InfoLine>
            </HeaderInfoWrapper>
            <CollapsibleDropdown
              titleMore="View More"
              titleLess="View Less"
              collapseStyles="background:none"
              titleCss={ViewMoreStyles}
            >
              <CollapsibleContainer>
                <InfoLine>
                  <Span>
                    <strong>Step 1: On-Boarding: </strong>
                  </Span>
                  This step brings the IAM service account into T-Vault so that
                  the secrets can be read or rotated through T-Vault. This is a
                  one-time operation.
                </InfoLine>

                <InfoLine>
                  <Span>
                    <strong>Step 2: Service Account Activation: </strong>
                  </Span>
                  The IAM service account owner will Activate (rotate) the
                  account password once after on-boarding the account into
                  T-Vault. This process ensures that the secrets in T-Vault and
                  IAM portal are in sync.
                </InfoLine>
                <InfoLine>
                  <Span>
                    <strong>Step 3: Granting Permissions: </strong>
                  </Span>
                  When an IAM service account is activated in T-Vault, the
                  account owner can grant specific permissions to other users
                  and groups allowing others to read and/or rotate the secrets
                  for the IAM service account as well through T-Vault.
                </InfoLine>
              </CollapsibleContainer>
            </CollapsibleDropdown>
          </InfoContainer>
          <EachDetail>
            <Label>IAM Service Account Name</Label>
            <Value>{iamSvcAccountData?.userName}</Value>
          </EachDetail>
          <EachDetail>
            <Label>Owner (Managed By)</Label>
            <Value>{iamSvcAccountData?.owner_ntid}</Value>
          </EachDetail>
          <EachDetail>
            <Label>Owner Email</Label>
            <Value>{iamSvcAccountData?.owner_email}</Value>
          </EachDetail>
          <EachDetail>
            <Label>AWS Accounnt ID</Label>
            <Value>{iamSvcAccountData?.awsAccountId}</Value>
          </EachDetail>
          <EachDetail>
            <Label>AWS Account Name</Label>
            <Value>{iamSvcAccountData?.awsAccountName}</Value>
          </EachDetail>
          <EachDetail>
            <Label>Created On</Label>
            <Value>{iamSvcAccountData?.createdDate}</Value>
          </EachDetail>
          <EachDetail>
            <Label>Application Name</Label>
            <Value>{iamSvcAccountData?.application_name}</Value>
          </EachDetail>
        </PreviewWrap>
        <CancelSaveWrapper>
          <ButtonComponent
            label="Cancel"
            color="primary"
            onClick={() => onCancelViewDetails(false)}
            width={isMobileScreen ? '100%' : ''}
          />
          {viewAccountData.permission === 'write' && (
            <CancelButton>
              <ButtonComponent
                label="Rotate"
                color="secondary"
                onClick={() => onRotateClicked()}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelButton>
          )}
          {!iamSvcAccountData?.isActivated && (
            <CancelButton>
              <ButtonComponent
                label="Activate"
                color="secondary"
                onClick={() => onActivateClicked()}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelButton>
          )}
        </CancelSaveWrapper>
      </ModalWrapper>
    </ComponentError>
  );
};

ViewIamSvcAccountDetails.propTypes = {
  iamSvcAccountData: PropTypes.objectOf(PropTypes.any),
  isMobileScreen: PropTypes.bool,
  isRotateSecret: PropTypes.func,
  isActivateIamSvcAcc: PropTypes.func,
  handleCloseModal: PropTypes.func,
  viewAccountData: PropTypes.objectOf(PropTypes.any).isRequired,
};

ViewIamSvcAccountDetails.defaultProps = {
  iamSvcAccountData: {},
  isMobileScreen: false,
  isRotateSecret: () => {},
  isActivateIamSvcAcc: () => {},
  handleCloseModal: () => {},
};

export default ViewIamSvcAccountDetails;
