// import React, { Suspense } from 'react';
// import { Route, Switch, Redirect } from 'react-router-dom';

// import SafePageLayout from './SafePageLayout';

// const PrivateRoutes = () => {
//   return (
//     <Suspense fallback={<div>Loading...</div>}>
//       <Switch>
//         <Redirect exact from="/" to="/" />

//         <Route
//           path="/:name"
//           render={(routeProps) => <SafePageLayout routeProps={routeProps} />}
//         />

//         <Redirect exact to="/" />
//       </Switch>
//     </Suspense>
//   );
// };

// export default PrivateRoutes;
