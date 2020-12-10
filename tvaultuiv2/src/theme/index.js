import { createMuiTheme } from '@material-ui/core/styles';

export const customColor = {
  secondary: {
    backgroundColor: '#20232e',
    color: '#5e627c',
  },
  primary: {
    backgroundColor: '#fff',
    color: '#000',
  },
  magenta: '#e20074',
  snackBarSuccess: 'rgba(14, 156, 77, 0.7)',
  hoverColor: {
    list: '#151820',
  },
  modal: {
    color: '#c4c4c4',
    title: '#fff',
    backgroundColor: '#2a2e3e',
  },
  collapse: {
    color: '#c4c4c4',
    title: '#8488a8',
    fontSize: '1.4rem;',
  },
  checkbox: {
    color: '#c4c4c4',
  },
  label: {
    color: '#8b8ea6',
  },
  status: {
    active: '#347a37',
    revoked: '#9a8022',
    pending: '#939496',
  },
};
const customTheme = createMuiTheme({
  breakpoints: {
    keys: ['xs', 'sm', 'md', 'lg', 'xl'],
    values: {
      xs: 0,
      sm: 768,
      md: 992,
      lg: 1280,
      xl: 1920,
    },
  },
  shadows: [
    'none',
    '0 1px 3px 0 rgba(0, 0, 0, 0.2), 0 2px 1px -1px rgba(0, 0, 0, 0.12), 0 1px 1px 0 rgba(0, 0, 0, 0.14)', // 1- app use
    '0 1px 5px 0 rgba(0, 0, 0, 0.2), 0 3px 1px -2px rgba(0, 0, 0, 0.12), 0 2px 2px 0 rgba(0, 0, 0, 0.14)', // 2 - app use
    '0 1px 8px 0 rgba(0, 0, 0, 0.2), 0 3px 3px -2px rgba(0, 0, 0, 0.12), 0 3px 4px 0 rgba(0, 0, 0, 0.14)', // 3 - app use
    '0 2px 4px -1px rgba(0, 0, 0, 0.2), 0 1px 10px 0 rgba(0, 0, 0, 0.12), 0 4px 5px 0 rgba(0, 0, 0, 0.14)', // 4 - app use
    '0px 3px 5px -1px rgba(0,0,0,0.2),0px 5px 8px 0px rgba(0,0,0,0.14),0px 1px 14px 0px rgba(0,0,0,0.12)',
    '0 3px 5px -1px rgba(0, 0, 0, 0.2), 0 1px 18px 0 rgba(0, 0, 0, 0.12), 0 6px 10px 0 rgba(0, 0, 0, 0.14)', // 6 - app use
    '0px 4px 5px -2px rgba(0,0,0,0.2),0px 7px 10px 1px rgba(0,0,0,0.14),0px 2px 16px 1px rgba(0,0,0,0.12)',
    '0 5px 5px -3px rgba(0, 0, 0, 0.2), 0 3px 14px 2px rgba(0, 0, 0, 0.12), 0 8px 10px 1px rgba(0, 0, 0, 0.14)', // 8 - app use
    '0 5px 6px -3px rgba(0, 0, 0, 0.2), 0 3px 16px 2px rgba(0, 0, 0, 0.12), 0 9px 12px 1px rgba(0, 0, 0, 0.14)', // 9 - app use
    '0px 6px 6px -3px rgba(0,0,0,0.2),0px 10px 14px 1px rgba(0,0,0,0.14),0px 4px 18px 3px rgba(0,0,0,0.12)',
    '0px 6px 7px -4px rgba(0,0,0,0.2),0px 11px 15px 1px rgba(0,0,0,0.14),0px 4px 20px 3px rgba(0,0,0,0.12)',
    '0 7px 8px -4px rgba(0, 0, 0, 0.2), 0 5px 22px 4px rgba(0, 0, 0, 0.12), 0 12px 17px 2px rgba(0, 0, 0, 0.14)', // 12 - app use
    '0px 7px 8px -4px rgba(0,0,0,0.2),0px 13px 19px 2px rgba(0,0,0,0.14),0px 5px 24px 4px rgba(0,0,0,0.12)',
    '0px 7px 9px -4px rgba(0,0,0,0.2),0px 14px 21px 2px rgba(0,0,0,0.14),0px 5px 26px 4px rgba(0,0,0,0.12)',
    '0px 8px 9px -5px rgba(0,0,0,0.2),0px 15px 22px 2px rgba(0,0,0,0.14),0px 6px 28px 5px rgba(0,0,0,0.12)',
    '0 8px 10px -5px rgba(0, 0, 0, 0.2), 0 6px 30px 5px rgba(0, 0, 0, 0.12), 0 16px 24px 2px rgba(0, 0, 0, 0.14)', // 16 - app use
    '0px 8px 11px -5px rgba(0,0,0,0.2),0px 17px 26px 2px rgba(0,0,0,0.14),0px 6px 32px 5px rgba(0,0,0,0.12)',
    '0px 9px 11px -5px rgba(0,0,0,0.2),0px 18px 28px 2px rgba(0,0,0,0.14),0px 7px 34px 6px rgba(0,0,0,0.12)',
    '0px 9px 12px -6px rgba(0,0,0,0.2),0px 19px 29px 2px rgba(0,0,0,0.14),0px 7px 36px 6px rgba(0,0,0,0.12)',
    '0px 10px 13px -6px rgba(0,0,0,0.2),0px 20px 31px 3px rgba(0,0,0,0.14),0px 8px 38px 7px rgba(0,0,0,0.12)',
    '0px 10px 13px -6px rgba(0,0,0,0.2),0px 21px 33px 3px rgba(0,0,0,0.14),0px 8px 40px 7px rgba(0,0,0,0.12)',
    '0px 10px 14px -6px rgba(0,0,0,0.2),0px 22px 35px 3px rgba(0,0,0,0.14),0px 8px 42px 7px rgba(0,0,0,0.12)',
    '0px 11px 14px -7px rgba(0,0,0,0.2),0px 23px 36px 3px rgba(0,0,0,0.14),0px 9px 44px 8px rgba(0,0,0,0.12)',
    '0 11px 15px -7px rgba(0, 0, 0, 0.2), 0 9px 46px 8px rgba(0, 0, 0, 0.12), 0 24px 38px 3px rgba(0, 0, 0, 0.14)', // 24 - app use
  ],
  typography: {
    fontFamily:
      'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
    h1: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 200,
      fontSize: '9.6rem',
      lineHeight: 1.17,
      letterSpacing: '-0.09375em',
      color: '#fff',
    },
    h2: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 200,
      fontSize: '6rem',
      lineHeight: 1.2,
      letterSpacing: '-0.03125rem',
      color: '#fff',
    },
    h3: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 'normal',
      fontSize: '4.8rem',
      lineHeight: 1.17,
      letterSpacing: 'normal',
      color: '#fff',
    },
    h4: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 'normal',
      fontSize: '3.4rem',
      lineHeight: 1.06,
      letterSpacing: 'normal',
      color: '#fff',
    },
    h5: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontSize: '2.4rem',
      fontWeight: 500,
      fontStretch: 'normal',
      fontStyle: 'normal',
      lineHeight: 'normal',
      letterSpacing: 'normal',
      color: '#fff',
    },
    h6: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 500,
      fontSize: '1.25rem',
      lineHeight: 1.2,
      letterSpacing: '0.009375rem',
      color: '#fff',
    },
    subtitle1: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 'normal',
      fontSize: '1.4rem',
      lineHeight: 1.5,
      letterSpacing: '0.009375rem',
      color: '#fff',
    },
    subtitle2: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 500,
      fontSize: '1.2rem',
      lineHeight: 1.71,
      letterSpacing: '0.00625rem',
      color: '#fff',
    },
    body1: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 'normal',
      fontSize: '1.6rem',
      lineHeight: 1.5,
      letterSpacing: '0.03125rem',
      color: '#5e627c',
      background: 'transparent',
    },
    body2: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 'normal',
      fontSize: '1.4rem',
      lineHeight: 1.43,
      letterSpacing: '0.015625rem',
      backgroundImage: 'linear-gradient(to top, #2f3545, #1d212c)',
      backgroundRepeat: 'no-repeat',
      // minHeight: '100vh',
      color: '#fff',
    },
    collapse: {
      color: '#c4c4c4',
    },
    button: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 500,
      fontSize: '1.4rem',
      lineHeight: 1.14,
      letterSpacing: '0.078125rem',
      color: 'rgba(0, 0, 0, 0.87)',
    },
    caption: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 'normal',
      fontSize: '0.75rem',
      lineHeight: 1.33,
      letterSpacing: '0.025rem',
      color: '#fff',
    },
    overline: {
      fontFamily:
        'BlinkMacSystemFont,-apple-system,"Segoe UI",Roboto,Oxygen,Ubuntu,"Helvetica Neue",Arial,sans-serif',
      fontWeight: 500,
      fontSize: '0.75rem',
      lineHeight: 1.6,
      letterSpacing: '0.09375rem',
      textTransform: 'uppercase',
      color: 'rgba(0, 0, 0, 0.87)',
    },
  },
  palette: {
    primary: {
      main: '#fff',
      contrastText: '#e20074',
    },
    secondary: {
      main: '#e20074',
      contrastText: '#fff',
    },
    error: {
      main: '#ee4e4e',
    },
    success: {
      main: '#6dba15',
    },
    text: {
      primary: '#000',
      secondary: '#000',
      light: '#fff',
      main: '#fff',
      disabled: 'rgba(255, 255, 255, 0.38)',
      hint: '#fff',
      icon: '#fff',
    },
    action: {
      active: '#fff',
      hover: 'rgba(266 , 0, 116, 0.3)',
      hoverOpacity: 0.08,
      selected: '#e20074',
      selectedOpacity: 0.16,
      disabled: '#fff',
      disabledBackground: '#454c5e',
      disabledOpacity: 0.38,
      focus: '#590074',
      focusOpacity: 1,
      activatedOpaciy: 0.24,
    },
    background: {
      paper: '#1f232e',
      default: '#fafafa',
      white: '#333',
      level1: '#212121',
      headerParent: '#e20074',
      headerChild: '#31394d',
      modal: '#2a2e3e',
    },
  },
  overrides: {
    MuiSvgIcon: {
      root: {
        width: '2rem',
        height: '2rem',
      },
    },
    MuiPaper: {
      rounded: {
        borderRadius: '0',
      },
      root: {
        color: customColor.collapse.color,
      },
    },
    MuiAppBar: { colorPrimary: { backgroundColor: 'transparent' } },
    MuiInput: {
      root: {
        padding: '0.5rem',
      },
    },
    MuiIcon: {
      root: {
        fontSize: '2.4rem',
      },
    },
    MuiButton: {
      root: {
        textTransform: 'capitalize',
        borderRadius: '0',
        fontSize: '1.2rem',
      },
    },
    MuiFab: {
      secondary: {
        backgroundImage:
          'linear-gradient(to bottom, #ff61b2, #e20074, #650038)',
        color: '#fff',
      },
    },
    MuiTooltip: {
      textColorSecondary: {
        color: '#000',
      },
    },
    MuiFormHelperText: {
      root: {
        color: customColor.label.color,
      },
    },
    MuiPopover: {
      text: {
        color: '#fff',
      },
    },
    MuiTab: {
      root: {
        textTransform: 'capitalize',
      },
      textColorPrimary: {
        color: '#5e627c',
      },
    },
    MuiFormLabel: {
      root: {
        marginBottom: '0.8rem',
        fontSize: '1.4rem',
        color: '#fff',
      },
    },
    MuiRadio: {
      root: {
        color: customColor.secondary.color,
        '&$checked': {
          color: customColor.magenta,
        },
      },
    },
    MuiFilledInput: {
      root: {
        padding: '1rem 1rem 1rem 1.5rem',
        borderTopLeftRadius: '0',
        borderTopRightRadius: '0',
        fontSize: '1.4rem',
        height: '5rem',
        backgroundColor: customColor.primary.backgroundColor,
        color: customColor.primary.color,
        '&:hover': {
          backgroundColor: '#fff',
        },
      },
      input: {
        padding: '0.3rem 0',
      },
      adornedStart: {
        paddingLeft: '1.5rem',
      },
      multiline: {
        padding: '1.5rem 1rem',
        height: 'auto',
      },
      colorSecondary: {
        backgroundColor: customColor.secondary.backgroundColor,
        color: customColor.secondary.color,
        '&:hover': {
          backgroundColor: customColor.secondary.backgroundColor,
        },
      },
      underline: {
        '&:after': {
          borderBottom: '0',
        },
        '&:before': {
          borderBottom: '0',
        },
        '&:hover&:before': {
          borderBottom: '0',
        },
      },
    },
    MuiSelect: {
      select: {
        '&:focus': {
          backgroundColor: 'transparent',
        },
      },
    },
    MuiAlert: {
      root: {
        minHeight: '5.1rem',
        alignItems: 'center',
      },
      filledSuccess: {
        background: customColor.snackBarSuccess,
        backgroundColor: 'none',
      },
      filledError: {
        backgroundImage: 'none',
      },
    },
    MuiSnackbar: {
      anchorOriginBottomRight: {
        '@media (min-width: 768px)': {
          right: '11rem',
          bottom: '5.3rem',
        },
      },
    },
    MuiAutocomplete: {
      inputRoot: {
        paddingBottom: '0',
        height: '5rem',
        '&[class*="MuiFilledInput-root"]': {
          paddingTop: '0',
        },
      },
      paper: {
        backgroundColor: customColor.primary.backgroundColor,
        color: customColor.primary.color,
      },
      option: {
        paddingLeft: '2rem',
        '&[data-focus="true"]': {
          backgroundColor: customColor.primary.backgroundColor,
          color: customColor.magenta,
        },
        '&[aria-selected="true"]': {
          backgroundColor: customColor.primary.backgroundColor,
          color: customColor.magenta,
        },
        '&:active': {
          backgroundColor: 'transparent',
        },
      },
    },
    MuiCollapse: {
      container: {
        color: customColor.collapse.color,
      },
    },
  },
  baseFontSize: {
    fontSize: '10px',
    '@media only all and (min-width: 1024px) and (max-width: 1299px)': {
      fontSize: '0.7692vw',
    },
  },
  gradients: {
    list: 'linear-gradient(to right, #72134b, #1d212c)',
    nav: 'linear-gradient(to top, #7b124e, rgba(123, 18, 78, 0))',
    sideBar: 'linear-gradient(to right, #1d212c, #72134b)',
  },
  customColor,
  zIndex: { zIndex: 1300 },
});

export default customTheme;
