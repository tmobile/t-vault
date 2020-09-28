import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import CardMedia from '@material-ui/core/CardMedia';
import ReactHtmlParser from 'react-html-parser';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';

const DialogeBoxWrapper = styled('div')`
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  height: 100%;
  width: ${(props) => props.width};
`;
const BoxDescription = styled.p`
  text-align: center;
  font-size: 1.4rem;
  color: #5e627c;
`;

const BackgroundIcon = styled(CardMedia)`
  ${(props) => props.imgStyles}
`;
const NoData = (props) => {
  const { description, actionButton, imageSrc, bgIconStyle, width } = props;

  return (
    <ComponentError>
      <DialogeBoxWrapper width={width}>
        <BackgroundIcon
          image={imageSrc}
          title="no-data"
          imgStyles={bgIconStyle}
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
  bgIconStyle: PropTypes.objectOf(PropTypes.object),
  width: PropTypes.string,
};
NoData.defaultProps = {
  description: 'Nothing here, But me',
  actionButton: <div />,
  imageSrc: '',
  bgIconStyle: { width: '100%', height: '22rem' },
  width: '100%',
};
export default NoData;
