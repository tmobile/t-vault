/* eslint-disable import/no-unresolved */
/* eslint-disable react/require-default-props */
import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import { TitleTwo } from 'styles/GlobalStyles';
import mediaBreakpoints from 'breakpoints';

const ActionButton = styled('div')`
  display: flex;
  align-items: center;
  fontsize: 1.6rem ${(props) => props.customBtnStyle};
  cursor: pointer;
`;

const Icon = styled('img')`
  margin-left: 1rem;
  ${mediaBreakpoints.small} {
    margin-left: 0.5rem;
  }
`;

const styles = css`
  font-weight: bold;
`;

const NamedButton = (props) => {
  const { label, iconSrc, customStyle, onClick } = props;
  return (
    <ComponentError>
      <ActionButton customBtnStyle={customStyle} onClick={onClick}>
        <TitleTwo color="#fff" extraCss={styles}>
          {label}
        </TitleTwo>
        <Icon alt="icon-plus" src={iconSrc} />
      </ActionButton>
    </ComponentError>
  );
};
NamedButton.propTypes = {
  label: PropTypes.string,
  iconSrc: PropTypes.node,
  // eslint-disable-next-line react/forbid-prop-types
  customStyle: PropTypes.object,
  onClick: PropTypes.func,
};
NamedButton.defaultProps = {
  label: '',
  iconSrc: '',
  customStyle: {},
  onClick: () => {},
};
export default NamedButton;
