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
import AccessDeniedLogo from '../../../../../assets/accessdenied-logo.svg';

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
  justify-content: center;
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
  width: 60%;
  span {
    margin: 0 0.3rem;
    color: #fff;
  }
`;

const Secrets = (props) => {
  const {
    secretsFolder,
    secretsStatus,
    setEnableAddFolder,
    userHavePermission,
    safeDetail,
    value,
    getSecretDetails,
  } = props;
  // resolution handlers
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  return (
    <ComponentError>
      <SecretsContainer>
        {userHavePermission.type !== '' &&
          userHavePermission.type !== 'deny' &&
          Object.keys(safeDetail).length > 0 && (
            <CountSpan color="#5e627c">
              {`${
                secretsFolder[0] ? secretsFolder[0]?.children?.length : 0
              } Secrets`}
            </CountSpan>
          )}
        {secretsStatus.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {secretsStatus.status === 'failed' &&
          !secretsFolder[0]?.children?.length && (
            <EmptySecretBox>
              <Error description="Sorry we were unable to retrieve those documents." />
            </EmptySecretBox>
          )}
        {secretsStatus.status === 'success' &&
          Object.keys(safeDetail).length > 0 && (
            <>
              {safeDetail.access !== 'deny' && safeDetail.access !== '' && (
                <>
                  {secretsFolder[0]?.children?.length > 0 ? (
                    <Tree
                      data={secretsFolder}
                      value={value}
                      getSecretDetails={getSecretDetails}
                      userHavePermission={userHavePermission}
                    />
                  ) : secretsFolder[0]?.children?.length === 0 ? (
                    // eslint-disable-next-line react/jsx-indent
                    <EmptySecretBox>
                      {safeDetail.access === 'write' ? (
                        <NoData
                          imageSrc={NoSecretsIcon}
                          description={Strings.Resources.noSafeSecretFound}
                          actionButton={
                            // eslint-disable-next-line react/jsx-wrap-multilines
                            <ButtonComponent
                              label="add"
                              icon="add"
                              color="secondary"
                              disabled={safeDetail.access !== 'write'}
                              width={isMobileScreen ? '100%' : '9.4rem'}
                              onClick={() => setEnableAddFolder(true)}
                            />
                          }
                          bgIconStyle={bgIconStyle}
                          customStyle={noDataStyle}
                        />
                      ) : (
                        <NoData
                          imageSrc={NoSecretsIcon}
                          description={
                            Strings.Resources.noSafeSecretFoundReadPerm
                          }
                          bgIconStyle={bgIconStyle}
                          customStyle={noDataStyle}
                        />
                      )}
                    </EmptySecretBox>
                  ) : (
                    <></>
                  )}
                </>
              )}
              {safeDetail.access === '' && (
                <AccessDeniedWrap>
                  <AccessDeniedIcon
                    src={AccessDeniedLogo}
                    alt="accessDeniedLogo"
                  />
                  <NoPermission>
                    You<span>do</span>not have access to this<span>Safe</span>
                    and cannot view it’s contents
                  </NoPermission>
                </AccessDeniedWrap>
              )}
            </>
          )}
        {secretsStatus.status === 'success' &&
          Object.keys(safeDetail).length === 0 && (
            <AccessDeniedWrap>
              <AccessDeniedIcon src={NoSecretsIcon} alt="noSecretAvailable" />
              <NoPermission>
                Once you add a <span>safe</span> you’ll be able to add{' '}
                <span>secret</span> to view them all here!
              </NoPermission>
            </AccessDeniedWrap>
          )}
      </SecretsContainer>
    </ComponentError>
  );
};
Secrets.propTypes = {
  secretsFolder: PropTypes.arrayOf(PropTypes.any),
  secretsStatus: PropTypes.objectOf(PropTypes.any),
  setEnableAddFolder: PropTypes.func,
  userHavePermission: PropTypes.objectOf(PropTypes.any).isRequired,
  safeDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  value: PropTypes.number.isRequired,
  getSecretDetails: PropTypes.func.isRequired,
};
Secrets.defaultProps = {
  secretsFolder: [],
  secretsStatus: {},
  setEnableAddFolder: () => {},
};

export default Secrets;
