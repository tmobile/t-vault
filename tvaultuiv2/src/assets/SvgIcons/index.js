/* eslint-disable react/jsx-props-no-spreading */
/* eslint-disable import/prefer-default-export */
import SvgIcon from '@material-ui/core/SvgIcon';
import React from 'react';

export const IconDeleteActive = (props) => (
  <SvgIcon {...props}>
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="20"
      height="20"
      fill="none"
      viewBox="0 0 20 20"
    >
      <path
        stroke="#fff"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M2.5 5h15M6.667 5V3.334c0-.442.175-.866.488-1.179.312-.312.736-.488 1.178-.488h3.334c.442 0 .866.176 1.178.488.313.313.488.737.488 1.179V5m2.5 0v11.667c0 .442-.175.866-.488 1.178-.312.313-.736.489-1.178.489H5.833c-.442 0-.866-.176-1.178-.488-.313-.313-.488-.737-.488-1.179V5h11.666z"
      />
    </svg>
  </SvgIcon>
);

export const FolderPlus = (props) => (
  <SvgIcon {...props}>
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        fill="url(#paint0_linear)"
        d="M22 19c0 .53-.21 1.04-.586 1.414C21.04 20.79 20.53 21 20 21H4c-.53 0-1.04-.21-1.414-.586C2.21 20.04 2 19.53 2 19V5c0-.53.21-1.04.586-1.414C2.96 3.21 3.47 3 4 3h5l2 3h9c.53 0 1.04.21 1.414.586C21.79 6.96 22 7.47 22 8v11z"
      />
      <path
        stroke="#E8E8E8"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M12 11v6M9 14h6"
      />
      <defs>
        <linearGradient
          id="paint0_linear"
          x1="12"
          x2="12"
          y1="1.017"
          y2="21.915"
          gradientUnits="userSpaceOnUse"
        >
          <stop stopColor="#FF61B2" />
          <stop offset=".509" stopColor="#E20074" />
          <stop offset="1" stopColor="#650038" />
        </linearGradient>
      </defs>
    </svg>
  </SvgIcon>
);

export const IconAddFolder = (props) => (
  <SvgIcon {...props}>
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="20"
      height="20"
      fill="none"
      viewBox="0 0 20 20"
    >
      <path
        stroke="#fff"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M18.667 15.833c0 .442-.176.866-.489 1.179-.312.312-.736.488-1.178.488H3.667c-.442 0-.866-.176-1.179-.488-.312-.313-.488-.737-.488-1.179V4.167c0-.442.176-.866.488-1.179.313-.312.737-.488 1.179-.488h4.166L9.5 5H17c.442 0 .866.176 1.178.488.313.313.489.737.489 1.179v9.166zM10.333 9.167v5M7.833 11.667h5"
      />
    </svg>
  </SvgIcon>
);

export const IconAddSecret = (props) => (
  <SvgIcon {...props}>
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="15"
      height="18"
      fill="none"
      viewBox="0 0 15 18"
    >
      <g clipPath="url(#clip0)">
        <path
          stroke="#fff"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2"
          d="M13.333 7.5H1.667C.747 7.5 0 8.314 0 9.318v6.364C0 16.686.746 17.5 1.667 17.5h11.666c.92 0 1.667-.814 1.667-1.818V9.318c0-1.004-.746-1.818-1.667-1.818zM7.5 10v5M5 12.5h5M3 7.5V4.167c0-1.105.439-2.165 1.22-2.947C5.002.44 6.062 0 7.167 0s2.165.439 2.946 1.22c.781.782 1.22 1.842 1.22 2.947V7.5"
        />
      </g>
      <defs>
        <clipPath id="clip0">
          <path fill="#fff" d="M0 0H15V17.5H0z" />
        </clipPath>
      </defs>
    </svg>
  </SvgIcon>
);

export const IconEdit = (props) => (
  <SvgIcon {...props}>
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="20"
      height="20"
      fill="none"
      viewBox="0 0 20 20"
    >
      <path
        stroke="#fff"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M14.167 2.5c.219-.219.479-.393.764-.511.286-.118.593-.18.902-.18.31 0 .616.062.902.18.286.118.546.292.765.51.219.22.393.48.511.766.119.286.18.592.18.902 0 .31-.061.616-.18.902-.118.286-.292.545-.51.764L6.25 17.083l-4.584 1.25 1.25-4.583L14.167 2.5z"
      />
    </svg>
  </SvgIcon>
);

export const IconRelease = (props) => (
  <SvgIcon {...props}>
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="20"
      height="20"
      fill="none"
      viewBox="0 0 20 20"
    >
      <path
        stroke="#D0D0D0"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M7.5 17.5H4.167c-.442 0-.866-.176-1.179-.488-.312-.313-.488-.737-.488-1.179V4.167c0-.442.176-.866.488-1.179.313-.312.737-.488 1.179-.488H7.5M13.334 14.167L17.5 10l-4.166-4.167M17.5 10h-10"
      />
    </svg>
  </SvgIcon>
);

export const IconFolderActive = (props) => (
  <SvgIcon {...props}>
    {' '}
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="48"
      height="48"
      fill="none"
      viewBox="0 0 48 48"
    >
      <g filter="url(#filter0_d)">
        <rect width="40" height="40" x="4" fill="url(#paint0_linear)" rx="5" />
      </g>
      <path
        fill="url(#paint1_linear)"
        stroke="#fff"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M32.333 25.833c0 .442-.175.866-.488 1.179-.313.312-.736.488-1.178.488H17.332c-.442 0-.866-.176-1.178-.488-.313-.313-.489-.737-.489-1.179V14.167c0-.442.176-.866.489-1.179.312-.312.736-.488 1.178-.488H21.5l1.666 2.5h7.5c.442 0 .866.176 1.179.488.313.313.488.737.488 1.179v9.166z"
      />
      <defs>
        <linearGradient
          id="paint0_linear"
          x1="24"
          x2="24"
          y1="-4.407"
          y2="42.034"
          gradientUnits="userSpaceOnUse"
        >
          <stop stopColor="#FF61B2" />
          <stop offset=".509" stopColor="#E20074" />
          <stop offset="1" stopColor="#650038" />
        </linearGradient>
        <linearGradient
          id="paint1_linear"
          x1="24"
          x2="24"
          y1="10.848"
          y2="28.263"
          gradientUnits="userSpaceOnUse"
        >
          <stop stopColor="#FF61B2" />
          <stop offset=".509" stopColor="#E20074" />
          <stop offset="1" stopColor="#650038" />
        </linearGradient>
        <filter
          id="filter0_d"
          width="48"
          height="48"
          x="0"
          y="0"
          colorInterpolationFilters="sRGB"
          filterUnits="userSpaceOnUse"
        >
          <feFlood floodOpacity="0" result="BackgroundImageFix" />
          <feColorMatrix
            in="SourceAlpha"
            values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0"
          />
          <feOffset dy="4" />
          <feGaussianBlur stdDeviation="2" />
          <feColorMatrix values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.25 0" />
          <feBlend in2="BackgroundImageFix" result="effect1_dropShadow" />
          <feBlend in="SourceGraphic" in2="effect1_dropShadow" result="shape" />
        </filter>
      </defs>
    </svg>
  </SvgIcon>
);

export const IconLock = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="12"
    height="13"
    fill="none"
    viewBox="0 0 12 13"
  >
    <g clipPath="url(#clip0)">
      <path
        stroke="#E8E8E8"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.5"
        d="M10.667 5.85H1.333C.597 5.85 0 6.432 0 7.15v4.55c0 .718.597 1.3 1.333 1.3h9.334c.736 0 1.333-.582 1.333-1.3V7.15c0-.718-.597-1.3-1.333-1.3zM2.666 5.85v-2.6c0-.862.351-1.689.976-2.298C4.267.342 5.115 0 6 0c.884 0 1.732.342 2.357.952.625.61.977 1.436.977 2.298v2.6"
      />
    </g>
    <defs>
      <clipPath id="clip0">
        <path fill="#fff" d="M0 0H12V13H0z" />
      </clipPath>
    </defs>
  </svg>
);

export const IconDeleteInactive = () => (
  <SvgIcon>
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="15"
      height="16"
      fill="none"
      viewBox="0 0 15 16"
    >
      <path
        stroke="#5A637A"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.5"
        d="M1 3.8h12.6M4.5 3.8V2.4c0-.371.147-.727.41-.99.263-.262.619-.41.99-.41h2.8c.371 0 .727.147.99.41.262.263.41.619.41.99v1.4m2.1 0v9.8c0 .371-.148.727-.41.99-.263.262-.619.41-.99.41h-7c-.371 0-.728-.148-.99-.41-.263-.263-.41-.619-.41-.99V3.8h9.8z"
      />
    </svg>
  </SvgIcon>
);

export const IconFolderInactive = (props) => (
  <SvgIcon {...props}>
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="40"
      height="40"
      fill="none"
      viewBox="0 0 40 40"
    >
      <rect width="40" height="40" fill="url(#paint0_linear)" rx="5" />
      <path
        fill="url(#paint1_linear)"
        stroke="#5A637A"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        d="M28.333 25.833c0 .442-.175.866-.488 1.179-.313.312-.736.488-1.178.488H13.332c-.442 0-.866-.176-1.178-.488-.313-.313-.489-.737-.489-1.179V14.167c0-.442.176-.866.489-1.179.312-.312.736-.488 1.178-.488H17.5l1.666 2.5h7.5c.442 0 .866.176 1.179.488.313.313.488.737.488 1.179v9.166z"
      />
      <defs>
        <linearGradient
          id="paint0_linear"
          x1="63"
          x2="61.681"
          y1="40"
          y2="-5.414"
          gradientUnits="userSpaceOnUse"
        >
          <stop stopColor="#151820" />
          <stop offset="1" stopColor="#454B61" />
        </linearGradient>
        <linearGradient
          id="paint1_linear"
          x1="17.5"
          x2="17.5"
          y1="14"
          y2="28"
          gradientUnits="userSpaceOnUse"
        >
          <stop stopColor="#151820" />
          <stop offset="1" stopColor="#454B61" />
        </linearGradient>
      </defs>
    </svg>
  </SvgIcon>
);

export const IconRefreshCC = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    fill="none"
    viewBox="0 0 24 24"
  >
    <g
      stroke="#fff"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="2"
      clipPath="url(#clip0)"
    >
      <path d="M.833 3.333v5h5M19.167 16.667v-5h-5" />
      <path d="M17.075 7.5c-.423-1.194-1.141-2.262-2.088-3.104-.947-.841-2.092-1.43-3.327-1.71-1.236-.279-2.522-.24-3.74.111C6.705 3.15 5.597 3.804 4.7 4.7L.833 8.333m18.334 3.334L15.3 15.3c-.896.896-2.004 1.55-3.22 1.903-1.218.352-2.504.39-3.74.11-1.236-.28-2.38-.867-3.327-1.71-.947-.84-1.665-1.909-2.088-3.103" />
    </g>
    <defs>
      <clipPath id="clip0">
        <path fill="#fff" d="M0 0H20V20H0z" />
      </clipPath>
    </defs>
  </svg>
);

export const BackArrow = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    fill="none"
    viewBox="0 0 24 24"
  >
    <path
      fill="#fff"
      d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"
    />
  </svg>
);

export const SearchIcon = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="26"
    height="25"
    fill="none"
    viewBox="0 0 26 25"
  >
    <path
      fill="#5E627C"
      d="M16.158 14.56h-.824l-.291-.28c1.021-1.186 1.636-2.725 1.636-4.4 0-3.733-3.033-6.76-6.776-6.76-3.742 0-6.776 3.027-6.776 6.76 0 3.734 3.034 6.76 6.776 6.76 1.679 0 3.221-.614 4.41-1.633l.281.292v.821l5.213 5.19 1.553-1.55-5.202-5.2zm-6.255 0c-2.596 0-4.691-2.09-4.691-4.68S7.307 5.2 9.903 5.2c2.596 0 4.691 2.09 4.691 4.68s-2.095 4.68-4.69 4.68z"
    />
  </svg>
);
