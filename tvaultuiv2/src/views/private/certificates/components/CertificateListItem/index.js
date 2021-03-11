import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import Avatar from '@material-ui/core/Avatar';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import { TitleOne } from '../../../../../styles/GlobalStyles';
import ListItemIcon from '../../../../../assets/icon_safes.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import TooltipComponent from '../../../../../components/Tooltip';

const FolderWrap = styled('div')`
  position: relative;
  display: flex;
  text-decoration: none;
  align-items: center;
  justify-content: space-between;
`;
const ListItemDetailBox = styled('div')`
  padding-left: 1.7rem;
`;
const ListItemAvatarWrap = styled.div`
  .MuiAvatar-root {
    width: 3.4rem;
    height: 3.9rem;
    border-radius: 0;
  }
  .MuiAvatar-img {
    width: auto;
  }
  display: flex;
  align-items: center;
  opacity: ${(props) => (props.isOnboardCert ? '0.5' : '1')};
`;

const SubTitleWrap = styled.div`
  display: flex;
  align-items: center;
  opacity: ${(props) => (props.isOnboardCert ? '0.5' : '1')};
`;
const Flag = styled('span')`
  font-size: ${(props) => props.fontSize};
  font-style: ${(props) => (props.fontStyle ? props.fontStyle : '')};
  color: #a2a1a1;
`;

const CertType = styled('span')`
  color: #a2a1a1;
  font-size: 1.3rem;
  text-transform: Capitalize;
`;

const Dot = styled.span`
  width: 0.3rem;
  height: 0.3rem;
  background-color: ${(props) => props.theme.customColor.secondary.color};
  border-radius: 50%;
  margin: 0 5px;
  display: inline-block;
`;

const LabelWrap = styled.div`
  display: flex;
  align-items: center;
`;

const extraCss = css`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 1.6rem;
  width: 25rem;
  opacity: ${(props) => (props.isOnboardCert ? '0.5' : '1')};
  ${mediaBreakpoints.belowLarge} {
    width: 17rem;
  }
  ${mediaBreakpoints.medium} {
    width: 12rem;
  }
  ${mediaBreakpoints.small} {
    width: 15rem;
  }
`;

const CertificateListItem = (props) => {
  const { title, createDate, icon, certType, isOnboardCert } = props;

  const renderTitle =()=>{
    return (
      <TitleOne 
      isOnboardCert={isOnboardCert}
      color="#d0d0d0" 
      extraCss={extraCss}
      >
        {title}
      </TitleOne>
      )
  }
  return (
    <ComponentError>
      <FolderWrap>
        <LabelWrap>
          <ListItemAvatarWrap isOnboardCert={isOnboardCert}>
            <Avatar alt="ListItem_icon" src={icon} />
          </ListItemAvatarWrap>
          <ListItemDetailBox>
          <TooltipComponent 
          title={title} 
          certificate ={'top'} 
          renderContent={renderTitle()}
          >
            </TooltipComponent>
            <SubTitleWrap isOnboardCert={isOnboardCert}>
              <Flag fontSize="1.3rem">{createDate}</Flag>
              {createDate ? (
                <>
                  <Dot />
                  <CertType>{certType}</CertType>
                </>
              ) : (
                <CertType>{certType}</CertType>
              )}
            </SubTitleWrap>
          </ListItemDetailBox>
        </LabelWrap>
      </FolderWrap>
    </ComponentError>
  );
};
CertificateListItem.propTypes = {
  createDate: PropTypes.string,
  title: PropTypes.string,
  icon: PropTypes.string,
  certType: PropTypes.string,
  isOnboardCert: PropTypes.bool.isRequired,
};
CertificateListItem.defaultProps = {
  createDate: '',
  title: '',
  icon: ListItemIcon,
  certType: '',
};
export default CertificateListItem;
