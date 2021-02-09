/* eslint-disable react/jsx-curly-newline */
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import Loader from '../../../../../components/Loaders/LoaderSpinner';
import mediaBreakPoint from '../../../../../breakpoints';
import accessDeniedLogo from '../../../../../assets/accessdenied-logo.svg';

const DetailsWrap = styled.div`
  padding: 0 4rem;
  overflow-y: auto;
  height: 100%;
  display: flex;
  flex-direction: column;
  ${mediaBreakPoint.small} {
    padding: 0;
  }
`;

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
  font-size: 1.6rem;
  text-transform: ${(props) => props.capitalize || ''};
`;

const DnsName = styled.p`
  padding: 0.5rem 0;
  font-size: 1.6rem;
`;

const customStyle = css`
  height: 100%;
`;

const ErrorWrap = styled.div`
  height: 100%;
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
`;

const AccessDeniedIcon = styled.img`
  width: 16rem;
  height: 16rem;
`;

const NoPermission = styled.div`
  color: ${(props) => props.theme.customColor.label.color};
  text-align: center;
  margin-top: 2rem;
`;

const CertificateInformation = (props) => {
  const { responseStatus, certificateMetaData, errorMessage } = props;
  const [response, setResponse] = useState({ status: 'loading' });
  const [dnsNames, setDnsNames] = useState([]);

  useEffect(() => {
    setResponse({ status: responseStatus });
  }, [responseStatus]);

  useEffect(() => {
    if (certificateMetaData?.dnsNames) {
      if (typeof certificateMetaData.dnsNames !== 'string') {
        setDnsNames(certificateMetaData.dnsNames);
      } else {
        const parts = certificateMetaData?.dnsNames.split(/[[\]]{1,2}/);
        const stringArray = parts[1].split(', ');
        setDnsNames([...stringArray]);
      }
    }
  }, [certificateMetaData]);

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && <Loader customStyle={customStyle} />}
        {response.status === 'success' && (
          <>
            {!certificateMetaData.isOnboardCert ? (
              <DetailsWrap>
                {certificateMetaData?.containerName && (
                  <EachDetail>
                    <Label>Container:</Label>
                    <Value>{certificateMetaData.containerName || 'N/A'}</Value>
                  </EachDetail>
                )}
                <EachDetail>
                  <Label>Owner Email:</Label>
                  <Value>{certificateMetaData.certOwnerEmailId || 'N/A'}</Value>
                </EachDetail>
                <EachDetail>
                  <Label>Certificate Type:</Label>
                  <Value capitalize="capitalize">
                    {certificateMetaData.certType || 'N/A'}
                  </Value>
                </EachDetail>
                <EachDetail>
                  <Label>Certificate Name:</Label>
                  <Value>{certificateMetaData.certificateName || 'N/A'}</Value>
                </EachDetail>
                <EachDetail>
                  <Label>Application Name:</Label>
                  <Value>{certificateMetaData.applicationTag || 'N/A'}</Value>
                </EachDetail>
                <EachDetail>
                  <Label>Validity:</Label>
                  {certificateMetaData?.createDate ? (
                    <Value>
                      {new Date(certificateMetaData?.createDate).toDateString()}
                      {' - '}
                      {new Date(certificateMetaData?.expiryDate).toDateString()}
                    </Value>
                  ) : (
                    <Value>N/A</Value>
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
                <EachDetail>
                  <Label>Dns:</Label>
                  {certificateMetaData.dnsNames && dnsNames.length > 0 ? (
                    <>
                      {dnsNames?.map((item) => {
                        return (
                          <DnsName key={item}>{item.replace(/"/g, '')}</DnsName>
                        );
                      })}
                    </>
                  ) : (
                    'N/A'
                  )}
                </EachDetail>
              </DetailsWrap>
            ) : (
              <ErrorWrap>
                <AccessDeniedIcon
                  src={accessDeniedLogo}
                  alt="accessDeniedLogo"
                />
                <NoPermission>Certificate is not onboarded!</NoPermission>
              </ErrorWrap>
            )}
          </>
        )}
        {response.status === 'error' && (
          <ErrorWrap>
            <AccessDeniedIcon src={accessDeniedLogo} alt="accessDeniedLogo" />
            <NoPermission>{errorMessage}</NoPermission>
          </ErrorWrap>
        )}
      </>
    </ComponentError>
  );
};

CertificateInformation.propTypes = {
  certificateMetaData: PropTypes.objectOf(PropTypes.any),
  responseStatus: PropTypes.string,
  errorMessage: PropTypes.string,
};

CertificateInformation.defaultProps = {
  certificateMetaData: {},
  responseStatus: 'loading',
  errorMessage: '',
};
export default CertificateInformation;
