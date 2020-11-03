/* eslint-disable no-console */
/* eslint-disable no-nested-ternary */
/* eslint-disable react/prop-types */
/* eslint-disable react/destructuring-assignment */
import React, { createContext, useState } from 'react';
// import apiService from './apiService';

export const UserContext = createContext({});

export const UserContextProvider = (props) => {
  const [contextState] = useState({});
  return (
    <UserContext.Provider value={contextState}>
      {props.children}
    </UserContext.Provider>
  );
};
