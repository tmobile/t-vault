/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import styled from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import { Link } from 'react-router-dom';
import Fab from '@material-ui/core/Fab';
import Icon from '@material-ui/core/Icon';
import Tooltip from '@material-ui/core/Tooltip';
import PropTypes from 'prop-types';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

// styled components
const FabWrap = styled.div``;

const setIcon = (props) => {
  const { icon } = props;
  return <Icon>{icon}</Icon>;
};
const useStylesBootstrap = makeStyles((theme) => ({
  arrow: {
    color: theme.palette.common.white,
  },
  tooltip: {
    backgroundColor: theme.palette.common.white,
    color: theme.palette.common.black,
    fontSize: theme.typography.subtitle1.fontSize,
  },
}));
const BootstrapTooltip = (options) => {
  const { title, placement, children } = options;
  const classes = useStylesBootstrap();

  // eslint-disable-next-line react/jsx-props-no-spreading
  return (
    <Tooltip arrow classes={classes} title={title} placement={placement}>
      {children}
    </Tooltip>
  );
};
const FloatingActionButtonComponent = (props) => {
  const {
    href,
    size,
    disabled,
    color,
    icon,
    tooltipPos,
    tooltipTitle,
    onClick,
  } = props;
  const linkProps = href
    ? {
        component: Link,
        to: href,
      }
    : '';

  return (
    <ComponentError>
      {tooltipTitle ? (
        <BootstrapTooltip title={tooltipTitle} placement={tooltipPos}>
          <FabWrap role="button" onClick={onClick}>
            <Fab
              color={color || 'default'}
              aria-label={icon}
              // href={href}
              size={size || 'small'}
              disabled={disabled || false}
              {...linkProps}
            >
              {setIcon({ ...props })}
            </Fab>
          </FabWrap>
        </BootstrapTooltip>
      ) : (
        <FabWrap onClick={onClick}>
          {' '}
          <Fab
            color={color || 'default'}
            aria-label={icon}
            // href={href}
            {...linkProps}
            size={size || 'small'}
            disabled={disabled || false}
          >
            {setIcon({ ...props })}
          </Fab>
        </FabWrap>
      )}
    </ComponentError>
  );
};

FloatingActionButtonComponent.propTypes = {
  href: PropTypes.string,
  size: PropTypes.string,
  disabled: PropTypes.bool,
  color: PropTypes.string,
  icon: PropTypes.string.isRequired,
  tooltipPos: PropTypes.string,
  tooltipTitle: PropTypes.string,
  onClick: PropTypes.func,
};

FloatingActionButtonComponent.defaultProps = {
  href: '',
  size: 'small',
  disabled: false,
  color: 'default',
  tooltipPos: 'bottom',
  tooltipTitle: '',
  onClick: () => {},
};

setIcon.propTypes = {
  icon: PropTypes.string.isRequired,
};

export default FloatingActionButtonComponent;
