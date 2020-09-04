import { createMuiTheme } from '@material-ui/core/styles';

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
    fontFamily: '"Exo 2", sans-serif',
    h1: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 200,
      fontSize: '6rem',
      lineHeight: 1.17,
      letterSpacing: '-0.09375em',
      color: '#fff',
    },
    h2: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 200,
      fontSize: '3.75rem',
      lineHeight: 1.2,
      letterSpacing: '-0.03125rem',
      color: '#fff',
    },
    h3: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 'normal',
      fontSize: '3rem',
      lineHeight: 1.17,
      letterSpacing: 'normal',
      color: '#fff',
    },
    h4: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 'normal',
      fontSize: '2.125rem',
      lineHeight: 1.06,
      letterSpacing: 'normal',
      color: '#fff',
    },
    h5: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 'normal',
      fontSize: '1.5rem',
      lineHeight: 1,
      letterSpacing: '0.01125rem',
      color: '#fff',
    },
    h6: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 500,
      fontSize: '1.25rem',
      lineHeight: 1.2,
      letterSpacing: '0.009375rem',
      color: '#fff',
    },
    subtitle1: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 'normal',
      fontSize: '1rem',
      lineHeight: 1.5,
      letterSpacing: '0.009375rem',
      color: '#fff',
    },
    subtitle2: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 500,
      fontSize: '0.875rem',
      lineHeight: 1.71,
      letterSpacing: '0.00625rem',
      color: '#fff',
    },
    body1: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 'normal',
      fontSize: '1rem',
      lineHeight: 1.5,
      letterSpacing: '0.03125rem',
      color: 'red',
      background: '#fff',
    },
    body2: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 'normal',
      fontSize: '0.875rem',
      lineHeight: 1.43,
      letterSpacing: '0.015625rem',
      backgroundImage: 'linear-gradient(to top, #2f3545, #1d212c)',
      backgroundRepeat: 'no-repeat',
      minHeight: '100vh',
    },
    button: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 500,
      fontSize: '0.875rem',
      lineHeight: 1.14,
      letterSpacing: '0.078125rem',
      color: 'rgba(0, 0, 0, 0.87)',
    },
    caption: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
      fontWeight: 'normal',
      fontSize: '0.75rem',
      lineHeight: 1.33,
      letterSpacing: '0.025rem',
      color: '#fff',
    },
    overline: {
      fontFamily: '"Exo 2", "Helvetica", "Arial", sans-serif',
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
      main: '#e20074',
      contrastText: '#fff',
    },
    secondary: {
      main: '#31394d',
      dark: '#9e0051',
      contrastText: '#fff',
    },
    error: {
      main: '#b00020',
    },
    success: {
      main: '#6dba15',
    },
    text: {
      primary: '#fff',
      secondary: '#56d1ee',
      light: '#fff',
      main: '#e20074',
      disabled: 'rgba(0, 0, 0, 0.38)',
      hint: '#31394d',
      icon: '#31394d',
    },
    action: {
      active: '#e20074',
      hover: 'rgba(226, 0, 116, 0.08)',
      hoverOpacity: 0.08,
      selected: '#e20074',
      selectedOpacity: 0.16,
      disabled: 'rgba(0, 0, 0, 0.38)',
      disabledBackground: 'rgba(255, 255, 255, 0.12)',
      disabledOpacity: 0.38,
      focus: '#590074',
      focusOpacity: 1,
      activatedOpaciy: 0.24,
    },
    background: {
      paper: '#fff',
      default: '#fafafa',
      white: '#333',
      level1: '#212121',
      headerParent: '#e20074',
      headerChild: '#31394d',
    },
  },
  overrides: {
    MuiButton: {
      root: {
        padding: '0.75rem 2.125rem',
        borderRadius: '1.5rem',
      },
      outlined: {
        padding: '0.75rem 2.125rem',
        borderRadius: '1.5rem',
      },
      label: {
        fontSize: 'inherit',
        fontFamily: 'inherit',
      },
      outlinedPrimary: {
        borderWidth: '0.0625rem',
        '&:hover': {
          borderWidth: '0.0625rem',
        },
      },
      outlinedSecondary: {
        borderWidth: '0.0625rem',
        '&:hover': {
          borderWidth: '0.0625rem',
        },
      },
    },
    MuiFab: {
      extended: {
        height: '3rem',
        minWidth: '3rem',
        borderRadius: '1.5rem',
        padding: '0 1rem',
      },
    },
    MuiIconButton: {
      root: {
        padding: '0.75rem',
      },
    },
    MuiSvgIcon: {
      root: {
        width: '2rem',
        height: '2rem',
      },
    },
    MuiPickersCalendarHeader: {
      transitionContainer: {
        height: '1.4375rem',
      },
      dayLabel: {
        width: '2.25rem',
        margin: '0 0.125rem',
      },
    },
    MuiPickersToolbar: {
      toolbar: {
        height: '6.25rem',
      },
    },
    MuiPickerDTToolbar: {
      toolbar: {
        paddingLeft: '1rem',
        paddingRight: '1rem',
      },
    },
    MuiTabs: {
      root: {
        minHeight: '3rem',
      },
    },
    MuiToolbar: {
      gutters: {
        paddingLeft: '1rem',
        paddingRight: '1rem',
        '@media (min-width: 768px)': {
          paddingLeft: '1.5rem',
          paddingRight: '1.5rem',
        },
      },
    },
    MuiPickersDay: {
      day: {
        width: '2.25rem',
        height: '2.25rem',
        margin: '0 0.125rem',
      },
    },
    MuiPickersBasePicker: {
      pickerView: {
        // maxWidth: '20.3125rem',
        minWidth: '19.375rem',
        minHeight: '19.0625rem',
      },
    },
    MuiPickersCalendar: {
      transitionContainer: {
        margiTop: '0.75rem',
        minHeight: '13.5rem',
      },
    },
    MuiListItemText: {
      marginTop: '0.25rem',
      marginBottom: '0.25rem',
    },
    MuiListItem: {
      root: {
        paddingTop: '0.25rem',
        paddingBottom: '0.25rem',
      },
      gutters: {
        paddingLeft: '1rem',
        paddingRight: '1rem',
      },
    },
    // MuiPickersClock: {
    //   container: {
    //     margin: '1rem 0 0.5rem',
    //   },
    //   clock: {
    //     width: '16.25rem',
    //     height: '16.25rem',
    //   },
    // },
    MuiInput: {
      root: {
        fontFamily: 'inherit',
      },
    },
    MuiInputLabel: {
      outlined: {
        transform: 'translate(0.875rem, 1.25rem) scale(1)',
      },
      shrink: {
        transform: 'translate(0.875rem, -0.375rem) scale(0.75) !important',
      },
    },
    PrivateNotchedOutline: {
      root: {
        paddingLeft: '0.5rem',
      },
      legendLabelled: {
        fontSize: '0.75rem',
      },
    },
    MuiOutlinedInput: {
      root: {
        borderRadius: '0.25rem',
      },
      notchedOutline: {
        borderWidth: '0.0625rem',
      },
      input: {
        padding: '1.15625rem 0.875rem',
      },
      adornedEnd: {
        paddingRight: 0,
      },
    },
    MuiFormLabel: {
      root: {
        fontFamily: 'inherit',
      },
    },
    MuiSelect: {
      icon: {
        top: 'calc(50% - 0.75rem)',
      },
    },
    MuiPaper: {
      rounded: {
        borderRadius: '0.25rem',
      },
    },
    MuiAutocomplete: {
      inputRoot: {
        '&.MuiOutlinedInput-root': {
          padding: '0.625rem',
        },
      },
      input: {
        padding: '0.593rem 0.25rem !important',
      },
      root: {
        '& [class*="MuiInputBase-root"]': {
          paddingRight: '4.0625rem !important',
        },
      },
      popupIndicator: {
        padding: '0.125rem',
        marginRight: '-0.125rem',
      },
      endAdornment: {
        top: 'calc(50% - 0.875rem)',
        right: '0.5625rem !important',
      },
      clearIndicator: {
        padding: '0.25rem',
        marginRight: '0.125rem',
      },
    },
    // MuiInputBase: {
    //   input: {
    //     Mui: {
    //       disabled: {
    //         color: 'black',
    //       },
    //     },
    //   },
    // },
    MuiAvatar: {
      root: {
        border: '0.0625rem 	#696969 solid',
        width: '100%',
        height: '100%',
        maxWidth: '8rem',
        maxHeight: '8rem',
      },
      img: {
        textIndent: '62.5rem',
      },
    },
  },
  baseFontSize: {
    // ,fontSize: '1.125vw',
    // '@media only all and (max-width: 1279px)': {
    fontSize: '10px',
    // },
  },
  // boxShadow: '0 2px 4px 0 rgba(0, 0, 0, 0.5)',
  colorPalette: {
    color1: '#730f3a',
    color2: '#e20074',
    color3: '#558dca', // blue
    color4: '#f5a623',
    color5: '#6dba15', // green
    color6: '#d8d8d8', // grey
    statusInProgress: '#6dba15', // green
    statusNotStarted: '#d8d8d8', // grey
    statusCompleted: '#558dca', // blue
    statusNotTracking: '#000000', // black
    statusStarted: '#6dba15', // green
    active: '#e20074',
    activeSecondary: '#fff',
    disabled: 'rgba(0, 0, 0, 0.38)',
    disabledSecondary: 'rgba(255, 255, 255, 0.38)',
    hoverSelected: '#9e0051', // dark pink
    selected: '#e20074',
    focus: '#590074',
    icons: '#80868b',
    bgDefault: '#fafafa',
    bgPrimary: '#fff',
  },
  text: {
    light: '#fff',
    dark: '#000',
    primary: '#e20074',
    secondary: '#31394d',
    active: '#e20074',
    activeSecondary: '#fff',
    disabled: 'rgba(0, 0, 0, 0.38)',
    disabledSecondary: 'rgba(255, 255, 255, 0.38)',
    hoverSelected: '#9e0051', // dark pink
    selected: '#e20074',
    error: '#b00020',
  },
  borders: {
    border1: '#eee',
    border2: '#fafafa',
    border3: '#f6f7f9',
    border4: '#dddddd',
  },
});

export default customTheme;
