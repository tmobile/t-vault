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
import RendetionError from './errorBoundaries/RendetionError/rendetion-error';

const Views = lazy(() => import('./views'));

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
const Preview = styled('section')`
  width: 100%;
  margin: auto;
`;

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
                <Header theme={customMuiTheme} />
                <Layout id="rootLayout">
                  <Preview>
                    <Suspense fallback={<div>Loading......</div>}>
                      <Switch>
                        <Route
                          path="/"
                          render={(routerProps) => <Views {...routerProps} />}
                        />
                      </Switch>
                    </Suspense>
                  </Preview>
                </Layout>
              </Container>
            </MuiPickersUtilsProvider>
          </ThemeProvider>
        </MuiThemeProvider>
      </RendetionError>
    </BrowserRouter>
  );
};

export default withRouter(App);
