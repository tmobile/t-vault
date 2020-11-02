/* eslint-disable react/jsx-curly-newline */
import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { Link } from 'react-router-dom';
import mediaBreakpoints from '../../../../../../../breakpoints';
import certIcon from '../../../../../../../assets/cert-icon.svg';
import { TitleFour } from '../../../../../../../styles/GlobalStyles';
import CertificateListItem from '../../../CertificateListItem';
import EditAndDeletePopup from '../../../../../../../components/EditAndDeletePopup';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import EditDeletePopper from '../../../../../service-accounts/components/EditDeletePopper';

const PopperWrap = styled.div`
  position: absolute;
  right: 4%;
  z-index: 1;
  max-width: 18rem;
  display: none;
`;

const CertificateStatus = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;

const ListFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  text-decoration: none;
  align-items: center;
  padding: 1.2rem 1.8rem 1.2rem 3.4rem;
  cursor: pointer;
  justify-content: space-between;
  background-image: ${(props) =>
    props.active === 'true' ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active === 'true' ? '#fff' : '#4a4a4a')};
  ${mediaBreakpoints.belowLarge} {
    padding: 2rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
    color: #fff;
    ${PopperWrap} {
      display: block;
    }
    ${CertificateStatus} {
      display: none;
    }
  }
`;
const StatusActionWrapper = styled.div`
  display: flex;
  align-items: center;
`;
const EditDeletePopperWrap = styled.div``;

const BorderLine = styled.div`
  border-bottom: 0.1rem solid #1d212c;
  width: 90%;
  position: absolute;
  bottom: 0;
`;

const StatusIcon = styled.span`
  width: 1.2rem;
  height: 1.2rem;
  border-radius: 50%;
  margin-top: 0.4rem;
  margin-left: 0.6rem;
  background-color: ${(props) =>
    // eslint-disable-next-line no-nested-ternary
    props.status === 'Active'
      ? props.theme.customColor.status.active
      : props.status === 'Revoked'
      ? props.theme.customColor.status.revoked
      : props.theme.customColor.status.pending};
`;

const extraCss = css`
  color: ${(props) => props.theme.customColor.secondary.color};
`;

const LeftColumn = (props) => {
  const {
    certificateList,
    onLinkClicked,
    isMobileScreen,
    onDeleteCertificateClicked,
    onTransferOwnerClicked,
    onEditListItemClicked,
    history,
  } = props;

  /**
   * @function onActionClicked
   * @description function to prevent default click.
   * @param {object} e event
   */
  const onActionClicked = (e) => {
    e.stopPropagation();
    e.preventDefault();
  };
  return (
    <ComponentError>
      <>
        {certificateList.map((certificate) => (
          <ListFolderWrap
            key={certificate.certificateName}
            to={{
              pathname: `/certificates/${certificate.certificateName}`,
              state: { data: certificate },
            }}
            onClick={() => onLinkClicked(certificate)}
            active={
              history.location.pathname ===
              `/certificates/${certificate.certificateName}`
                ? 'true'
                : 'false'
            }
          >
            <CertificateListItem
              title={certificate.certificateName}
              certType={certificate.certType}
              createDate={
                certificate.createDate
                  ? new Date(certificate.createDate).toLocaleDateString()
                  : ''
              }
              icon={certIcon}
              showActions={false}
            />
            <BorderLine />
            <StatusActionWrapper>
              {certificate.certificateStatus && (
                <CertificateStatus>
                  <TitleFour extraCss={extraCss}>
                    {certificate.certificateStatus}
                  </TitleFour>
                  <StatusIcon status={certificate.certificateStatus} />
                </CertificateStatus>
              )}
              {!certificate.certificateStatus && certificate.requestStatus && (
                <CertificateStatus>
                  <TitleFour extraCss={extraCss}>
                    {certificate.requestStatus}
                  </TitleFour>
                  <StatusIcon status={certificate.requestStatus} />
                </CertificateStatus>
              )}
              {certificate.applicationName && !isMobileScreen ? (
                <PopperWrap onClick={(e) => onActionClicked(e)}>
                  <EditAndDeletePopup
                    onDeletListItemClicked={() =>
                      onDeleteCertificateClicked(certificate)
                    }
                    onEditListItemClicked={() =>
                      onEditListItemClicked(certificate)
                    }
                    admin
                    isTransferOwner
                    onTransferOwnerClicked={() =>
                      onTransferOwnerClicked(certificate)
                    }
                  />
                </PopperWrap>
              ) : null}
              {isMobileScreen && certificate.applicationName && (
                <EditDeletePopperWrap onClick={(e) => onActionClicked(e)}>
                  <EditDeletePopper
                    onDeleteClicked={() =>
                      onDeleteCertificateClicked(certificate)
                    }
                    onEditClicked={() => onEditListItemClicked(certificate)}
                    admin
                    onTransferOwnerClicked={() =>
                      onTransferOwnerClicked(certificate)
                    }
                  />
                </EditDeletePopperWrap>
              )}
            </StatusActionWrapper>
          </ListFolderWrap>
        ))}
      </>
    </ComponentError>
  );
};

LeftColumn.propTypes = {
  certificateList: PropTypes.arrayOf(PropTypes.any).isRequired,
  onTransferOwnerClicked: PropTypes.func.isRequired,
  onDeleteCertificateClicked: PropTypes.func.isRequired,
  onEditListItemClicked: PropTypes.func.isRequired,
  onLinkClicked: PropTypes.func.isRequired,
  isMobileScreen: PropTypes.bool.isRequired,
  history: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default LeftColumn;
