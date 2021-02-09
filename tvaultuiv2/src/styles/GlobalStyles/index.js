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
  ${mediaBreakpoints.smallAndMedium} {
    margin: 0;
    margin-top: 7rem;
    height: calc(100vh - 7rem);
  }
`;

export const RequiredCircle = styled.span`
  width: 0.6rem;
  height: 0.6rem;
  background-color: #e20074;
  border-radius: 50%;
  margin-left: ${(props) => props.margin || '0'};
  display: inline-block;
  margin-bottom: 0.1rem;
  ${(props) => (props.extraCss ? props.extraCss : '')}
`;

export const RequiredText = styled.span`
  font-size: 1.6rem;
  color: #5e627c;
  margin-left: 0.5rem;
  ${mediaBreakpoints.small} {
    font-size: 1.4rem;
  }
`;

export const LabelRequired = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

export const RequiredWrap = styled.div`
  margin-bottom: 0.8rem;
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;

export const InstructionText = styled.p`
  margin-top: 1.4rem;
  color: #bbbbbb;
  font-size: 1.2rem;
  margin-bottom: 0rem;
  ${mediaBreakpoints.small} {
    font-size: 1.3rem;
    opacity: 0.4;
  }
`;

export const GlobalModalWrapper = styled.section`
  background-color: ${(props) => props?.theme?.palette?.background?.modal};
  padding: 5.5rem 6rem 6rem 6rem;
  border: none;
  outline: none;
  width: 69.6rem;
  margin: auto 0;
  display: flex;
  flex-direction: column;
  position: relative;
  ${mediaBreakpoints.belowLarge} {
    padding: 2.7rem 5rem 3.2rem 5rem;
    width: 57.2rem;
  }
  ${mediaBreakpoints.small} {
    width: 100%;
    padding: 2rem;
    margin: 0;
    height: fit-content;
  }
  ${(props) => (props.extraCss ? props.extraCss : '')}
`;

export const NoDataWrapper = styled.section`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
  margin-top: 1rem;
  p {
    ${mediaBreakpoints.small} {
      margin-top: 2rem;
      margin-bottom: 4rem;
      width: 75%;
    }
  }
`;
