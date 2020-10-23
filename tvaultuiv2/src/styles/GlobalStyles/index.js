import styled from 'styled-components';
import mediaBreakpoints from '../../breakpoints';

export const SubHeading = styled('div')`
  font-size: 2.4rem;
  color: ${(props) => props.color || '#fff'};
  line-height: ${(props) => props.lineHeight || 'normal'};
  font-family: ${(props) => props.fontfamily || ''};
  ${(props) => (props.extraCss ? props.extraCss : '')}
`;

export const TitleOne = styled('div')`
  font-size: 1.8rem;
  color: ${(props) => props.color || '#fff'};
  line-height: ${(props) => props.lineHeight || 'normal'};
  font-family: ${(props) => props.fontfamily || ''};
  ${(props) => (props.extraCss ? props.extraCss : '')}
  ${mediaBreakpoints.small} {
    font-size: 1.6rem;
  }
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

export const Color = {
  secretColor: '#5a637a',
};

export const PopperItem = styled.div`
  padding: 0.5em;
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
  cursor: pointer;
  span {
    margin-right: 0.75rem;
  }
  img {
    width: 2rem;
    height: 2rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
  ${(props) => (props.extraCss ? props.extraCss : '')}
`;

export const TabWrapper = styled.div`
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  .MuiAppBar-colorPrimary {
    background-color: inherit;
  }
  .MuiPaper-elevation4 {
    box-shadow: none;
  }
  .MuiTabs-root {
    min-height: unset;
  }
  .MuiTab-textColorInherit.Mui-selected {
    background-color: ${(props) => props.theme.palette.secondary.main};
    color: ${(props) => props.theme.palette.secondary.contrastText};
  }
  .MuiTab-textColorInherit {
    color: #e8e8e8;
    background-color: #20232e;
    min-width: auto;
    padding: 8.5px 20px;
    margin-right: 0.5rem;
    font-size: 1.4rem;
    min-height: 3.65rem;
    ${mediaBreakpoints.belowLarge} {
      margin-right: 1.2rem;
    }
  }
  .PrivateTabIndicator-colorSecondary-15 {
    background-color: inherit;
  }
`;

export const SectionPreview = styled.section`
  margin: 11.2rem auto 4.2rem auto;
  height: calc(100vh - 15.6rem);
  ${mediaBreakpoints.small} {
    margin: 0;
    height: 100vh;
  }
`;
