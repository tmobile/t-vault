import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import Loader from '../../../../../components/Loaders/LoaderSpinner';

const DetailsWrap = styled.div`
  padding: 0 4rem;
  overflow-y: auto;
  height: 100%;
`;

const EachDetail = styled.div`
  margin-bottom: 4rem;
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

const customStyle = css`
  height: 100%;
`;

const ErrorWrap = styled.div`
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #8b8ea6;
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
          <DetailsWrap>
            <EachDetail>
              <Label>Certificate Type:</Label>
              <Value>{certificateMetaData.certType || 'N/A'}</Value>
            </EachDetail>
            <EachDetail>
              <Label>Certificate Name:</Label>
              <Value>{certificateMetaData.certificateName || 'N/A'}</Value>
            </EachDetail>
            <EachDetail>
              <Label>Aplication Name:</Label>
              <Value>{certificateMetaData.applicationName || 'N/A'}</Value>
            </EachDetail>
            <EachDetail>
              <Label>DNS:</Label>
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
        )}
        {response.status === 'error' && <ErrorWrap>{errorMessage}</ErrorWrap>}
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
