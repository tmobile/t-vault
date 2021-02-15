import customTheme from '../theme';

const mediaBreakpoints = {
  small: `${customTheme.breakpoints.down('xs')}`, // (max-width: 767.95px)
  medium: `${customTheme.breakpoints.between('sm', 'sm')}`, // (min-width: 768px) and (max-width: 991.95px)
  desktop: `${customTheme.breakpoints.up('md')}`, // (min-width:992px)
  mediumAndAbove: `${customTheme.breakpoints.up('sm')}`, // (min-width: 768px)
  smallAndMedium: `${customTheme.breakpoints.down('sm')}`, // (max-width:991px)
  semiMedium: `${customTheme.breakpoints.down('1024')}`, // (max-width:1023)
  mediumAndsemiLarge: `${customTheme.breakpoints.between('sm', 'md')}`, // (min-width: 992px) and (max-width: 1280px)
  semiLarge: `${customTheme.breakpoints.between('md', 'md')}`, // (min-width: 992px) and (max-width: 1280px)
  large: `${customTheme.breakpoints.between('lg', 'xl')}`, // (min-width: 1280px) and (max-width: 1920px)
  belowLarge: `${customTheme.breakpoints.down('md')}`, // (max-width: 1279.95px)
  xLarge: `${customTheme.breakpoints.up('xl')}`, // (min-width: 1920px)
  ipadPro: `only screen and (min-device-width: 1024px) and (max-device-width: 1366px) 
            and (-webkit-min-device-pixel-ratio: 2)  and (orientation: portrait)`, // ipad pro portrait
};

export default mediaBreakpoints;
