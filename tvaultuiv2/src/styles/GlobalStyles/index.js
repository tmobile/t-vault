import styled from 'styled-components';

export const TitleOne = styled('div')`
  font-size: 1.8rem;
  color: ${(props) => props.color || '#fff'};
  line-height: ${(props) => props.lineHeight || 'normal'};
  font-family: ${(props) => props.fontfamily || ''};
  ${(props) => (props.extraCss ? props.extraCss : '')}
`;
export const TitleTwo = styled('div')`
  font-size: 1.6rem;
  color: ${(props) => props.color || '#fff'};
  line-height: ${(props) => props.lineHeight || 'normal'};
  font-family: ${(props) => props.fontfamily || ''};
  ${(props) => (props.extraCss ? props.extraCss : '')}
`;
export const TitleThree = styled('div')`
  font-size: 1.4rem;
  color: ${(props) => props.color || '#fff'};
  line-height: ${(props) => props.lineHeight || 'normal'};
  font-family: ${(props) => props.fontfamily || ''};
  text-overflow: ellipsis;
  overflow: hidden;
  ${(props) => (props.extraCss ? props.extraCss : '')}
`;
export const TitleFour = styled('div')`
  font-size: 1.3rem;
  color: ${(props) => props.color || '#fff'};
  line-height: ${(props) => props.lineHeight || 'normal'};
  font-family: ${(props) => props.fontfamily || ''};
  ${(props) => (props.extraCss ? props.extraCss : '')}
`;

export const BackgroundColor = {
  listBg: '#1c1f29',
  secretBg: '#2a2e3e',
  secretHoverBg: '#252937',
};
