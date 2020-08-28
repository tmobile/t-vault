/* eslint-disable react/jsx-props-no-spreading */
import React, { Suspense, lazy } from 'react';
import MomentUtils from '@date-io/moment';
import CssBaseline from '@material-ui/core/CssBaseline';
import {
  ThemeProvider as MuiThemeProvider,
  withStyles,
} from '@material-ui/core/styles';
import styled, { ThemeProvider } from 'styled-components';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import './App.css';
import { BrowserRouter, Route, Switch, withRouter } from 'react-router-dom';
import customMuiTheme from './theme';
import Header from './components/Header';
import CreateModal from './components/modal';
import AddFolder from './components/add-folder';
import RendetionError from './errorBoundaries/RendetionError/rendetion-error';

const PublicRoutes = lazy(() => import('./views/public'));
const PrivateRoutes = lazy(() => import('./views/private'));

const GlobalCss = withStyles({
  // @global is handled by jss-plugin-global.
  '@global': {
    html:
      customMuiTheme && customMuiTheme.baseFontSize
        ? customMuiTheme.baseFontSize
        : '10px',
  },
})(() => null);

const Container = styled('div')``;
const Layout = styled('main')``;
const Preview = styled('section')``;

export const App = () => {
  return (
    <BrowserRouter>
      <RendetionError>
        <MuiThemeProvider theme={customMuiTheme}>
          <ThemeProvider theme={customMuiTheme}>
            <MuiPickersUtilsProvider utils={MomentUtils}>
              <CssBaseline />
              <GlobalCss />
              <Container>
                <Header />
                <Layout id="rootLayout">
                  <Preview>
                    <Suspense fallback={<div>Loading......</div>}>
                      <Switch>
                        {/* <Route exact path="/" render={() => <PublicRoutes />} /> */}
                        <Route
                          path="/private"
                          render={(routerProps) => (
                            <PrivateRoutes {...routerProps} />
                          )}
                        />
                        <Route
                          path="/"
                          render={(routerProps) => (
                            <PublicRoutes {...routerProps} />
                          )}
                        />
                      </Switch>
                    </Suspense>
                  </Preview>
                </Layout>
                <CreateModal />
                <AddFolder />
              </Container>
            </MuiPickersUtilsProvider>
          </ThemeProvider>
        </MuiThemeProvider>
      </RendetionError>
    </BrowserRouter>
  );
};

export default withRouter(App);
