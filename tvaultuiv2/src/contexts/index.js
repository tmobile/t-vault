/* eslint-disable no-console */
/* eslint-disable no-nested-ternary */
/* eslint-disable react/prop-types */
/* eslint-disable react/destructuring-assignment */
import React, { createContext, useState } from 'react';
// import apiService from './apiService';

export const UserContext = createContext({});

export const UserContextProvider = (props) => {
  const [contextState] = useState({});
  // useEffect(() => {
  //   (async () => {
  //     try {
  //       const userResponse = await apiService.getAuth();
  //       const policies = userResponse?.data?.policies;
  //       const isAdmin =
  //         policies &&
  //         Array.isArray(policies) &&
  //         policies.indexOf('safeadmin') >= 0;
  //       setContextState({
  //         isAdmin,
  //       });
  //     } catch (e) {
  //       console.error('--- Unable to fecth user type --- ', e);
  //       setContextState({
  //         isAdmin: false,
  //       });
  //     }
  //     return () => {};
  //   })();
  // }, []);
  return (
    <UserContext.Provider value={contextState}>
      {props.children}
    </UserContext.Provider>
  );
};
