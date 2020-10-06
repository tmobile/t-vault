import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import VisibilityIcon from '@material-ui/icons/Visibility';
import VisibilityOffIcon from '@material-ui/icons/VisibilityOff';
import FileCopyIcon from '@material-ui/icons/FileCopy';
import IconRefreshCC from '../../../../../assets/refresh-ccw.svg';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../../apiService';
import lock from '../../../../../assets/icon_lock.svg';
import {
  PopperItem,
  BackgroundColor,
} from '../../../../../styles/GlobalStyles';
import PopperElement from '../../../../../components/Popper';

const UserList = styled.div`
  margin-top: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0;
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
`;

const Secret = styled.div`
  -webkit-text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  text-security: ${(props) => (props.viewSecret ? 'none' : 'disc')};
  font-size: 1.2rem;
  color: #5a637a;
`;

const customStyle = css`
  height: 100%;
`;

const Icon = styled.img`
  width: 2.2rem;
  height: 2.2rem;
  margin-right: 1.6rem;
  margin-left: 2rem;
`;

const IconDetailsWrap = styled.div`
  display: flex;
  align-items: center;
`;

const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  cursor: pointer;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
    :hover {
      background: ${(props) =>
        props.theme.customColor.hoverColor.list || '#151820'};
      border-radius: 50%;
    }
  }
`;

const ServiceAccountSecrets = (props) => {
  const { accountDetail } = props;
  const [response, setResponse] = useState({ status: 'loading' });
  const [secretsData, setSecretsData] = useState({});
  const [showSecret, setShowSecret] = useState(false);

  useEffect(() => {
    if (accountDetail && Object.keys(accountDetail).length > 0) {
      apiService
        .getServiceAccountPassword(accountDetail?.name)
        .then((res) => {
          setResponse({ status: 'success' });
          if (res?.data) {
            setSecretsData(res.data);
          }
        })
        .catch((e) => console.log('e.response', e.response));
    }
  }, [accountDetail]);

  const onViewSecretsCliked = () => {
    setShowSecret(!showSecret);
  };

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status === 'success' && secretsData && (
          <UserList>
            <IconDetailsWrap>
              <Icon src={lock} alt="lock" />
              <Secret type="password" viewSecret={showSecret}>
                {secretsData.current_password}
              </Secret>
              <FolderIconWrap>
                <PopperElement
                  anchorOrigin={{
                    vertical: 'bottom',
                    horizontal: 'right',
                  }}
                  transformOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                  }}
                >
                  <PopperItem onClick={() => onViewSecretsCliked()}>
                    {showSecret ? <VisibilityOffIcon /> : <VisibilityIcon />}
                    <span>{showSecret ? 'Hide Secret' : 'View Secret'}</span>
                  </PopperItem>
                  <PopperItem>
                    <img alt="refersh-ic" src={IconRefreshCC} />
                    <span>Rotate Secret</span>
                  </PopperItem>
                  <PopperItem>
                    <FileCopyIcon />
                    <span>Copy Secret</span>
                  </PopperItem>
                </PopperElement>
              </FolderIconWrap>
            </IconDetailsWrap>
          </UserList>
        )}
      </>
    </ComponentError>
  );
};

ServiceAccountSecrets.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default ServiceAccountSecrets;
