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
  color: ${(props) => props.theme.customColor.label.color};
  margin-bottom: 0.9rem;
`;

const Value = styled.p`
  font-size: 1.8rem;
  text-transform: ${(props) => props.capitalize || ''};
`;
const DnsName = styled.p`
  padding: 0.5rem 0;
  font-size: 1.8rem;
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
    container,
    owner,
    notificationEmails,
    onboard,
  } = props;
  const [dnsNames, setDnsNames] = useState([]);
  useEffect(() => {
    if (dns) {
      if (typeof dns !== 'string') {
        setDnsNames(dns);
      } else {
        const parts = dns.split(/[[\]]{1,2}/);
        const stringArray = parts[1].split(', ');
        setDnsNames([...stringArray]);
      }
    }
  }, [dns]);
  return (
    <ComponentError>
      <DetailsWrap>
        <EachDetail>
          <Label>Container:</Label>
          <Value>{container || 'N/A'}</Value>
        </EachDetail>
        <EachDetail>
          <Label>Owner:</Label>
          <Value>{owner || 'N/A'}</Value>
        </EachDetail>
        <EachDetail>
          <Label>Certificate Type:</Label>
          <Value capitalize="capitalize">{certificateType || 'N/A'}</Value>
        </EachDetail>
        <EachDetail>
          <Label>Certificate Name:</Label>
          <Value>{certName || 'N/A'}</Value>
        </EachDetail>
        <EachDetail>
          <Label>Application Name:</Label>
          <Value>{applicationName || 'N/A'}</Value>
        </EachDetail>
        {notificationEmails?.length > 0 && (
          <EachDetail>
            <Label>Notification Emails:</Label>
            {notificationEmails?.map((item) => {
              return <DnsName key={item}>{item}</DnsName>;
            })}
          </EachDetail>
        )}
        <EachDetail>
          <Label>Dns:</Label>
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
        <EachDetail>
          <Label>Signature Algorithm:</Label>
          <Value>SHA256-RSA</Value>
        </EachDetail>
        <EachDetail>
          <Label>Key Usage:</Label>
          <Value>digitalSignature, keyEncipherment</Value>
        </EachDetail>
        <EachDetail>
          <Label>Extended Key Usage:</Label>
          <Value>serverAuth</Value>
        </EachDetail>
        <EachDetail>
          <Label>Enroll Service:</Label>
          <Value>T-Mobile Issuing CA 01 - SHA2</Value>
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
              label={onboard ? 'Onboard' : 'Create'}
              icon={onboard ? '' : 'add'}
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
  container: PropTypes.string,
  notificationEmails: PropTypes.arrayOf(PropTypes.any),
  owner: PropTypes.string,
  onboard: PropTypes.bool,
};

PreviewCertificate.defaultProps = {
  container: 'N/A',
  owner: 'N/A',
  certificateType: 'N/A',
  applicationName: 'N/A',
  certName: 'N/A',
  dns: [],
  notificationEmails: [],
  handleClose: () => {},
  onCreateClicked: () => {},
  onEditClicked: () => {},
  isMobileScreen: false,
  responseType: null,
  isEditCertificate: false,
  onboard: false,
};

export default PreviewCertificate;
