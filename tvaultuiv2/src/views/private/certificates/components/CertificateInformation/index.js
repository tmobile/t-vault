/* eslint-disable react/jsx-curly-newline */
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import Loader from '../../../../../components/Loaders/LoaderSpinner';
import Download from './components/Download';
import SnackbarComponent from '../../../../../components/Snackbar';

const DetailsWrap = styled.div`
  padding: 0 4rem;
  overflow-y: auto;
  height: 100%;
  display: flex;
  flex-direction: column;
`;

const TypeDownloadWrap = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 3rem;
`;

const TypeWrap = styled.div`
  p {
    margin: 0;
  }
`;

const DownLoadWrap = styled.div`
  padding: 1rem 0;
  align-self: flex-end;
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
  text-transform: capitalize;
`;

const DnsName = styled.p`
  border-bottom: 1px solid #5e627c;
  padding: 1rem 0;
  font-size: 1.6rem;
`;

const customStyle = css`
  height: 100%;
`;

const ErrorWrap = styled.div`
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: ${(props) => props.theme.customColor.label.color};
`;

const CertificateInformation = (props) => {
  const { responseStatus, certificateMetaData, errorMessage } = props;
  const [response, setResponse] = useState({ status: 'loading' });
  const [dnsNames, setDnsNames] = useState([]);
  const [toastResponse, setToastResponse] = useState(null);

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

  const onDownloadChange = (status, val) => {
    setResponse({ status });
    setToastResponse(val);
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setToastResponse(null);
  };

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && <Loader customStyle={customStyle} />}
        {response.status === 'success' && (
          <DetailsWrap>
            <TypeDownloadWrap>
              <TypeWrap>
                <Label>Certificate Type:</Label>
                <Value>{certificateMetaData.certType || 'N/A'}</Value>
              </TypeWrap>
              {certificateMetaData.certificateName && (
                <DownLoadWrap>
                  <Download
                    certificateMetaData={certificateMetaData}
                    onDownloadChange={(status, val) =>
                      onDownloadChange(status, val)
                    }
                  />
                </DownLoadWrap>
              )}
            </TypeDownloadWrap>
            <EachDetail>
              <Label>Certificate Name:</Label>
              <Value>{certificateMetaData.certificateName || 'N/A'}</Value>
            </EachDetail>
            <EachDetail>
              <Label>Aplication Name:</Label>
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
        {toastResponse === -1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message="Unable to download certificate!"
          />
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
