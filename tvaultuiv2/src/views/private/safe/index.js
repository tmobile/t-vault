/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withRouter } from 'react-router-dom';
import { SectionPreview } from '../../../styles/GlobalStyles';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import SafeDashboard from './components/SafeDashboard';
import { useStateValue } from '../../../contexts/globalState';

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
      <SectionPreview>
        <SafeDashboard {...props} />
      </SectionPreview>
    </ComponentError>
  );
};
SafePageLayout.propTypes = {};

SafePageLayout.defaultProps = {};
export default withRouter(SafePageLayout);
