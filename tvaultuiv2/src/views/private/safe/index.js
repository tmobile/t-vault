/* eslint-disable react/jsx-props-no-spreading */
import React, { lazy } from 'react';
import { Route, Switch, withRouter } from 'react-router-dom';
import styled from 'styled-components';
import mediaBreakpoints from '../../../breakpoints';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import SafeDashboard from './components/SafeDashboard';
// import { UserContext } from '../../../contexts';
import { useStateValue } from '../../../contexts/globalState';

const CreateSafe = lazy(() => import('./CreateSafe'));
const SafeSectionPreview = styled('section')`
  margin: 3em auto;
  height: 77vh;
  ${mediaBreakpoints.semiLarge} {
    margin: 3rem 3.5rem 0 3.5rem;
  }
  ${mediaBreakpoints.small} {
    margin: 0;
    height: 89vh;
  }
`;

const SafePageLayout = (props) => {
  /** -- statrt of sample code */
  // Sample to use context
  // const contextObj = useContext(UserContext);

  // Sample code to use global state - below function provides with the global state and a dispatcher to handle the reducer
  const [state, dispatch] = useStateValue();
  // eslint-disable-next-line no-console

  setTimeout(() => {
    if (state.dataOne?.value !== 'OLA') {
      dispatch({ type: 'R1_DUMMY_ONE', data: 'OLA' });
    }
  }, 5000);

  /** End of Sample code */
  return (
    <ComponentError>
      <main title="safe-layout">
        <SafeSectionPreview>
          <SafeDashboard {...props} />
        </SafeSectionPreview>
        <Switch>
          <Route
            exact
            path="/safe/create-safe"
            render={(routeProps) => (
              <CreateSafe routeProps={{ ...routeProps }} />
            )}
          />
          <Route
            exact
            path="/safe/edit-safe"
            render={(routeProps) => (
              <CreateSafe routeProps={{ ...routeProps }} />
            )}
          />
        </Switch>
      </main>
    </ComponentError>
  );
};
SafePageLayout.propTypes = {};

SafePageLayout.defaultProps = {};
export default withRouter(SafePageLayout);
