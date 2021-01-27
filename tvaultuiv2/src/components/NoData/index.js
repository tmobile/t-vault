import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import CardMedia from '@material-ui/core/CardMedia';
import ReactHtmlParser from 'react-html-parser';
import mediaBreakpoints from '../../breakpoints';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';

const DialogeBoxWrapper = styled('div')`
  display: flex;
  align-items: center;
  flex-direction: column;
  height: 100%;
  width: 100%;
  ${(props) => props.customStyle};
`;
const BoxDescription = styled.p`
  text-align: center;
  font-size: 1.4rem;
  color: #5e627c;
  ${mediaBreakpoints.small} {
    width: 90%;
  }
`;

const BackgroundIcon = styled(CardMedia)`
  ${(props) => props.imgstyling}
`;
const NoData = (props) => {
  const {
    description,
    actionButton,
    imageSrc,
    bgIconStyle,
    customStyle,
  } = props;

  return (
    <ComponentError>
      <DialogeBoxWrapper customStyle={customStyle}>
        <BackgroundIcon
          image={imageSrc}
          title="no-data"
          imgstyling={bgIconStyle}
        />
        <BoxDescription>{ReactHtmlParser(description)}</BoxDescription>
        {actionButton}
      </DialogeBoxWrapper>
    </ComponentError>
  );
};
NoData.propTypes = {
  description: PropTypes.string,
  actionButton: PropTypes.node,
  imageSrc: PropTypes.node,
  bgIconStyle: PropTypes.objectOf(PropTypes.any),
  customStyle: PropTypes.arrayOf(PropTypes.any),
};
NoData.defaultProps = {
  description: '',
  actionButton: <div />,
  imageSrc: '',
  bgIconStyle: { width: '100%', height: '22rem' },
  customStyle: [],
};
export default NoData;
