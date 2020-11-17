import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Typography } from '@material-ui/core';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import PreviewCertificate from '../../../../CreateCertificates/preview';
import mediaBreakpoints from '../../../../../../../breakpoints';
import leftArrowIcon from '../../../../../../../assets/left-arrow.svg';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import CertificateHeader from '../../../CertificateHeader';
import { GlobalModalWrapper } from '../../../../../../../styles/GlobalStyles';

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
  margin-right: 0.8rem;
  ${small} {
    margin-right: 1rem;
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
  text-transform: capitalize;
`;

const EachDetail = styled.div`
  margin-bottom: 3rem;
  p {
    margin: 0;
  }
`;

const ViewCertificate = (props) => {
  const {
    onCertRenewClicked,
    certificateData,
    isMobileScreen,
    showRevokeRenewBtn,
    onCloseModal,
    onCertRevokeClicked,
  } = props;
  return (
    <ComponentError>
      <GlobalModalWrapper>
        <HeaderWrapper>
          <LeftIcon src={leftArrowIcon} alt="go-back" onClick={onCloseModal} />
          <Typography variant="h5">Edit Certificate</Typography>
        </HeaderWrapper>
        <CertificateHeader />
        <PreviewWrap>
          <PreviewCertificate
            dns={certificateData.dnsNames}
            certificateType={certificateData.certType}
            applicationName={certificateData.applicationName}
            certName={certificateData.certificateName}
            container={certificateData.containerName}
            owner={certificateData.certOwnerEmailId}
            isEditCertificate
          />
          <EachDetail>
            <Label>Status:</Label>
            <Value>{certificateData.certificateStatus}</Value>
          </EachDetail>
          <EachDetail>
            <Label>Create Data:</Label>
            <Value>{certificateData.createDate}</Value>
          </EachDetail>
          <EachDetail>
            <Label>Expiry Date:</Label>
            <Value>{certificateData.expiryDate}</Value>
          </EachDetail>
        </PreviewWrap>
        <CancelSaveWrapper>
          <CancelButton>
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={onCloseModal}
              width={isMobileScreen ? '100%' : ''}
            />
          </CancelButton>
          {showRevokeRenewBtn && (
            <CancelButton>
              <ButtonComponent
                label="Revoke"
                color="secondary"
                onClick={() => onCertRevokeClicked()}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelButton>
          )}
          {showRevokeRenewBtn && (
            <ButtonComponent
              label="Renew"
              color="secondary"
              onClick={() => onCertRenewClicked()}
              width={isMobileScreen ? '100%' : ''}
            />
          )}
        </CancelSaveWrapper>
      </GlobalModalWrapper>
    </ComponentError>
  );
};

ViewCertificate.propTypes = {
  onCertRenewClicked: PropTypes.func,
  onCloseModal: PropTypes.func,
  certificateData: PropTypes.objectOf(PropTypes.any),
  isMobileScreen: PropTypes.bool,
  showRevokeRenewBtn: PropTypes.bool,
  onCertRevokeClicked: PropTypes.func,
};

ViewCertificate.defaultProps = {
  onCertRenewClicked: () => {},
  onCloseModal: () => {},
  onCertRevokeClicked: () => {},
  certificateData: {},
  isMobileScreen: false,
  showRevokeRenewBtn: false,
};

export default ViewCertificate;
