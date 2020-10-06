/* eslint-disable no-nested-ternary */
import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import Error from '../../../../../components/Error';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import Tree from '../Tree';
import NoSecretsIcon from '../../../../../assets/no-data-secrets.svg';
import mediaBreakpoints from '../../../../../breakpoints';

const SecretsContainer = styled('div')`
  height: 100%;
  overflow: auto;
`;
const EmptySecretBox = styled('div')`
  width: 100%;
  position: absolute;
  display: flex;
  justify-content: center;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
`;
const CountSpan = styled.span`
  margin-top: 1.5rem;
  color: #5e627c;
  font-size: 1.3rem;
`;
const bgIconStyle = {
  width: '16rem',
  height: '16rem',
};

const customStyle = css`
  height: calc(100% - 4rem);
`;

const noDataStyle = css`
  width: 45%;
  ${mediaBreakpoints.small} {
    width: 100%;
  }
`;

const Secrets = (props) => {
  const {
    secretsFolder,
    secretsStatus,
    safeDetail,
    getResponse,
    setEnableAddFolder,
  } = props;

  // resolution handlers
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  return (
    <ComponentError>
      <SecretsContainer>
        {
          <CountSpan color="#5e627c">
            {`${secretsFolder && secretsFolder.length} Secrets`}
          </CountSpan>
        }

        {secretsStatus.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {getResponse === -1 && !secretsFolder[0]?.children?.length && (
          <EmptySecretBox>
            <Error description="Error while fetching safes folders" />
          </EmptySecretBox>
        )}

        {secretsFolder[0]?.children?.length &&
        secretsStatus.status !== 'loading' ? (
          <Tree data={secretsFolder} />
        ) : secretsFolder[0]?.children?.length === 0 &&
          getResponse === 1 &&
          secretsStatus.status !== 'loading' ? (
          // eslint-disable-next-line react/jsx-indent
          <EmptySecretBox>
            <NoData
              imageSrc={NoSecretsIcon}
              description="add a <strong>Folder</strong> and then you will be able to add <strong>secrets</strong> to view them all here"
              actionButton={
                // eslint-disable-next-line react/jsx-wrap-multilines
                <ButtonComponent
                  label="add"
                  icon="add"
                  color="secondary"
                  disabled={
                    safeDetail?.access?.toLowerCase() === 'read' ||
                    safeDetail?.access === ''
                  }
                  width={isMobileScreen ? '100%' : '9.4rem'}
                  onClick={() => setEnableAddFolder(true)}
                />
              }
              bgIconStyle={bgIconStyle}
              customStyle={noDataStyle}
            />
          </EmptySecretBox>
        ) : (
          <></>
        )}
      </SecretsContainer>
    </ComponentError>
  );
};
Secrets.propTypes = {
  secretsFolder: PropTypes.arrayOf(PropTypes.any),
  secretsStatus: PropTypes.objectOf(PropTypes.any),
  safeDetail: PropTypes.objectOf(PropTypes.any),
  setEnableAddFolder: PropTypes.func,
  getResponse: PropTypes.number,
};
Secrets.defaultProps = {
  secretsFolder: [],
  secretsStatus: {},
  safeDetail: {},
  setEnableAddFolder: () => {},
  getResponse: null,
};

export default Secrets;
