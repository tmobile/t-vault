/* eslint-disable react/jsx-one-expression-per-line */
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
import Strings from '../../../../../resources';
import Tree from '../Tree';
import NoSecretsIcon from '../../../../../assets/no-data-secrets.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import accessDeniedLogo from '../../../../../assets/accessdenied-logo.svg';

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

const AccessDeniedWrap = styled.div`
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
`;

const AccessDeniedIcon = styled.img`
  width: 16rem;
  height: 16rem;
`;
const NoPermission = styled.div`
  display: inline-block;
  color: #5a637a;
  text-align: center;
  margin-top: 4rem;
  span {
    margin: 0 0.3rem;
    color: #fff;
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
            {`${
              secretsFolder[0] ? secretsFolder[0]?.children?.length : 0
            } Secrets`}
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
            {safeDetail?.access?.toLowerCase() === 'read' ||
            safeDetail?.access === '' ? (
              <AccessDeniedWrap>
                <AccessDeniedIcon
                  src={accessDeniedLogo}
                  alt="accessDeniedLogo"
                />
                <NoPermission>
                  You <span>do</span> not have acess to this <span>Safe</span>{' '}
                  and cannot view it’s contents
                </NoPermission>
              </AccessDeniedWrap>
            ) : (
              <NoData
                imageSrc={NoSecretsIcon}
                description={Strings.Resources.noSafeSecretFound}
                actionButton={
                  // eslint-disable-next-line react/jsx-wrap-multilines
                  <ButtonComponent
                    label="add"
                    icon="add"
                    color="secondary"
                    width={isMobileScreen ? '100%' : '9.4rem'}
                    onClick={() => setEnableAddFolder(true)}
                  />
                }
                bgIconStyle={bgIconStyle}
                customStyle={noDataStyle}
              />
            )}
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
