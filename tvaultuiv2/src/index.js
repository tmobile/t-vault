import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import { BrowserRouter } from 'react-router-dom';
import { MatomoProvider, createInstance } from '@datapunt/matomo-tracker-react';
import App from './App';
import * as serviceWorker from './serviceWorker';
import { StateProvider } from './contexts/globalState';
import mainReducer from './stateManagement/reducer';
import initialState from './stateManagement';

const instance = createInstance({
  urlBase: 'https://analytics.pacbot.t-mobile.com/',
  siteId: 8,
  userId: sessionStorage.getItem('owner') || 'User',
  linkTracking: false, // optional, default value: true
});

ReactDOM.render(
  <React.StrictMode>
    <MatomoProvider value={instance}>
      <BrowserRouter>
        <StateProvider reducer={mainReducer} state={initialState}>
          <App />
        </StateProvider>
      </BrowserRouter>
    </MatomoProvider>
  </React.StrictMode>,
  document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
