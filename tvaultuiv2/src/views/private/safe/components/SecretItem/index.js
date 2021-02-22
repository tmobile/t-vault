import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const Wrapper = styled('div')`
  position: relative;
  display: flex;
  width: 100%;
  align-items: center;
`;
const  LabelWrap = styled.div`
  display: flex;
`;
const Icon = styled('img')`
    width: 4rem;
    height: 4rem;
    margin-left: 0.8rem;
`;
const Titles = styled('div')`
  
`;
const Title = styled.div`
    font-size: 1.6rem;
    margin-left: 1.6rem;
`;

const SubTitle = styled.span`
    font-size: 1.2rem;
    margin-left: 1.6rem;
    color: #5e627c;
`;

const SecretItem = (props) => {
  const { title, subTitle, icon } = props;
  return (
      <Wrapper>
        <LabelWrap>
            {icon && <Icon alt="folder_icon" src={icon} />}
          <Titles>
            <Title >{title}</Title>
            <SubTitle>{subTitle}</SubTitle>
          </Titles>
        </LabelWrap>
      </Wrapper>
  );
};
SecretItem.propTypes = {
  subTitle: PropTypes.string,
  title: PropTypes.string,
  icon: PropTypes.string,
};
SecretItem.defaultProps = {
  subTitle: '',
  title: '',
  icon: '',
};
export default SecretItem;
