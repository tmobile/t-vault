/* eslint-disable react/prop-types */
import React, { createContext, useReducer, useContext } from 'react';

export const StateContext = createContext({});

export const StateProvider = ({ reducer, state, children }) => {
  return (
    <StateContext.Provider value={useReducer(reducer, state)}>
      {children}
    </StateContext.Provider>
  );
};

export const useStateValue = () => useContext(StateContext);
