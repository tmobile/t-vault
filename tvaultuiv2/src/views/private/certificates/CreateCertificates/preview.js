import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../errorBoundaries/ComponentError/component-error';
import ButtonComponent from '../../../../components/FormFields/ActionButton';
import breakpoints from '../../../../breakpoints';

const DetailsWrap = styled.div``;
const EachDetail = styled.div`
  margin-bottom: 3rem;
  p {
    margin: 0;
  }
`;

const Label = styled.p`
  font-size: 1.3rem;
  color: #8b8ea6;
  margin-bottom: 0.9rem;
`;

const Value = styled.p`
  font-size: 1.8rem;
  text-transform: capitalize;
`;
const DnsName = styled.p`
  border-bottom: 1px solid #5e627c;
  padding: 1rem 0;
`;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
  justify-content: flex-end;
  ${breakpoints.small} {
    margin-top: 5.3rem;
  }
  button {
    ${breakpoints.small} {
      height: 4.5rem;
    }
  }
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${breakpoints.small} {
    margin-right: 1rem;
    width: 100%;
  }
`;

const PreviewCertificate = (props) => {
  const {
    certificateType,
    applicationName,
    certName,
    dns,
    handleClose,
    onCreateClicked,
    onEditClicked,
    isMobileScreen,
    responseType,
    isEditCertificate,
  } = props;
  const [dnsNames, setDnsNames] = useState([]);
  useEffect(() => {
    if (dns) {
      if (typeof dns !== 'string') {
        setDnsNames(dns);
      } else {
        setDnsNames([]);
      }
    }
  }, [dns]);
  return (
    <ComponentError>
      <DetailsWrap>
        <EachDetail>
          <Label>Certificate Type:</Label>
          <Value>{certificateType || 'N/A'}</Value>
        </EachDetail>
        <EachDetail>
          <Label>Certificate Name:</Label>
          <Value>{certName || 'N/A'}</Value>
        </EachDetail>
        <EachDetail>
          <Label>Aplication Name:</Label>
          <Value>{applicationName || 'N/A'}</Value>
        </EachDetail>
        <EachDetail>
          <Label>DNS:</Label>
          {dnsNames?.length > 0 ? (
            <>
              {dnsNames?.map((item) => {
                return <DnsName key={item}>{item}</DnsName>;
              })}
            </>
          ) : (
            'N/A'
          )}
        </EachDetail>
        {!isEditCertificate && (
          <CancelSaveWrapper>
            <CancelButton>
              <ButtonComponent
                label="Cancel"
                color="primary"
                disabled={responseType === 0}
                onClick={() => handleClose()}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelButton>
            <CancelButton>
              <ButtonComponent
                label="Edit"
                color="secondary"
                disabled={responseType === 0}
                onClick={() => onEditClicked()}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelButton>
            <ButtonComponent
              label="Create"
              icon="add"
              color="secondary"
              disabled={responseType === 0}
              onClick={() => onCreateClicked()}
              width={isMobileScreen ? '100%' : ''}
            />
          </CancelSaveWrapper>
        )}
      </DetailsWrap>
    </ComponentError>
  );
};

PreviewCertificate.propTypes = {
  certificateType: PropTypes.string,
  applicationName: PropTypes.string,
  certName: PropTypes.string,
  dns: PropTypes.arrayOf(PropTypes.any),
  handleClose: PropTypes.func,
  onCreateClicked: PropTypes.func,
  onEditClicked: PropTypes.func,
  isMobileScreen: PropTypes.bool,
  responseType: PropTypes.number,
  isEditCertificate: PropTypes.bool,
};

PreviewCertificate.defaultProps = {
  certificateType: 'N/A',
  applicationName: 'N/A',
  certName: 'N/A',
  dns: [],
  handleClose: () => {},
  onCreateClicked: () => {},
  onEditClicked: () => {},
  isMobileScreen: false,
  responseType: null,
  isEditCertificate: false,
};

export default PreviewCertificate;
