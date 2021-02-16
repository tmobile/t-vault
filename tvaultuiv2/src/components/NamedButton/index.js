import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import { TitleTwo } from '../../styles/GlobalStyles';
import mediaBreakpoints from '../../breakpoints';

const ActionButton = styled('div')`
  display: flex;
  align-items: center;
  fontsize: 1.6rem;
  pointer-events: ${(props) => (props.disable ? 'none' : 'auto')};
  ${(props) => props.customBtnStyle};
  cursor: ${(props) => (props.disable ? 'not-allowed' : 'pointer')};
`;

const Icon = styled('img')`
  margin-left: 1rem;
  ${mediaBreakpoints.small} {
    margin-left: 0.5rem;
  }
`;

const styles = css`
  font-weight: bold;
  ${mediaBreakpoints.small} {
    font-weight: normal;
    font-size: 1.4rem;
  }
  ${(props) => props.labelStyle}
`;

const NamedButton = (props) => {
  const { label, iconSrc, customStyle, onClick, disable } = props;
  return (
    <ComponentError>
      <ActionButton
        customBtnStyle={customStyle}
        onClick={onClick}
        disable={disable}
      >
        <TitleTwo color={disable ? '#5e627c' : '#fff'} extraCss={styles}>
          {label}
        </TitleTwo>
        {iconSrc && <Icon alt="icon-plus" src={iconSrc} />}
      </ActionButton>
    </ComponentError>
  );
};
NamedButton.propTypes = {
  label: PropTypes.string,
  iconSrc: PropTypes.node,
  customStyle: PropTypes.arrayOf(PropTypes.any),
  onClick: PropTypes.func,
  disable: PropTypes.bool,
};
NamedButton.defaultProps = {
  label: '',
  iconSrc: '',
  customStyle: [],
  onClick: () => {},
  disable: false,
};
export default NamedButton;
