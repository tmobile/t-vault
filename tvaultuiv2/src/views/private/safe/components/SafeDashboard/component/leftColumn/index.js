/* eslint-disable react/no-array-index-key */
/* eslint-disable react/jsx-curly-newline */
import React from 'react';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import { Link } from 'react-router-dom';
import mediaBreakpoints from '../../../../../../../breakpoints';
import safeIcon from '../../../../../../../assets/icon_safes.svg';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import EditDeletePopper from '../../../EditDeletePopper';
import ListItem from '../../../ListItem';
import PsudoPopper from '../../../PsudoPopper';

const PopperWrap = styled.div`
  position: absolute;
  top: 50%;
  right: 0%;
  z-index: 1;
  width: 5.5rem;
  transform: translate(-50%, -50%);
  display: none;
`;

const SafeFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  align-items: center;
  text-decoration: none;
  justify-content: space-between;
  padding: 1.2rem 1.8rem 1.2rem 3.8rem;
  background-image: ${(props) =>
    props.active === 'true' ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active === 'true' ? '#fff' : '#4a4a4a')};
  ${mediaBreakpoints.belowLarge} {
    padding: 2rem 1.1rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
    color: #fff;
    ${PopperWrap} {
      display: block;
    }
  }
`;

const BorderLine = styled.div`
  border-bottom: 0.1rem solid #1d212c;
  width: 90%;
  position: absolute;
  bottom: 0;
`;

const EditDeletePopperWrap = styled.div``;

const iconStyles = makeStyles(() => ({
  root: {
    width: '3.4rem',
    height: '3.9rem',
  },
}));

const LeftColumn = (props) => {
  const {
    safeList,
    onLinkClicked,
    history,
    isAdmin,
    ownerOfSafes,
    onEditSafeClicked,
    onDeleteSafeClicked,
    onActionClicked,
    searchSelectClicked,
  } = props;
  const listIconStyles = iconStyles();
  const isTabAndMobScreen = useMediaQuery(mediaBreakpoints.smallAndMedium);

  return (
    <ComponentError>
      <>
        {safeList?.map((safe, index) => {
          return (
            <SafeFolderWrap
              key={index}
              to={{
                pathname: `/safes/${safe.name}`,
                state: { safe },
              }}
              onClick={() => onLinkClicked()}
              active={
                history.location.pathname === `/safes/${safe.name}`
                  ? 'true'
                  : 'false'
              }
            >
              <ListItem
                title={safe.name}
                subTitle={safe.safeType}
                flag={safe.type}
                icon={safeIcon}
                manage={safe.manage}
                listIconStyles={listIconStyles}
              />
              <BorderLine />
              {(safe.manage ||
                isAdmin ||
                (ownerOfSafes && searchSelectClicked)) &&
              !isTabAndMobScreen ? (
                <PopperWrap onClick={(e) => onActionClicked(e)}>
                  <PsudoPopper
                    onDeleteSafeClicked={(e) =>
                      onDeleteSafeClicked(e, safe.path)
                    }
                    safe={safe}
                    path="/safes/edit-safe"
                  />
                </PopperWrap>
              ) : null}
              {isTabAndMobScreen &&
                (safe.manage ||
                  isAdmin ||
                  (ownerOfSafes && searchSelectClicked)) && (
                  <EditDeletePopperWrap onClick={(e) => onActionClicked(e)}>
                    <EditDeletePopper
                      onDeleteClicked={(e) => onDeleteSafeClicked(e, safe.path)}
                      onEditClicked={() => onEditSafeClicked(safe)}
                    />
                  </EditDeletePopperWrap>
                )}
            </SafeFolderWrap>
          );
        })}
      </>
    </ComponentError>
  );
};

LeftColumn.propTypes = {
  safeList: PropTypes.arrayOf(PropTypes.any).isRequired,
  onEditSafeClicked: PropTypes.func.isRequired,
  onDeleteSafeClicked: PropTypes.func.isRequired,
  onLinkClicked: PropTypes.func.isRequired,
  history: PropTypes.objectOf(PropTypes.any).isRequired,
  isAdmin: PropTypes.bool.isRequired,
  onActionClicked: PropTypes.func.isRequired,
  ownerOfSafes: PropTypes.bool.isRequired,
  searchSelectClicked: PropTypes.bool.isRequired,
};

LeftColumn.defaultProps = {};

export default LeftColumn;
