import React from "react";
import {
  BrowserRouter as Router,
  Route,
  Switch,
  Redirect,
} from "react-router-dom";

import Header from "components/navs/Header/header";
import HomePage from "views/public/HomePage/homePage";

import appStyles from "./App.module.scss";
import SafePageLayout from "views/private/SafePageLayout/safePageLayout";

function App(props) {
  return (
    <div className={appStyles.appContainer}>
      <Router>
        <Switch>
          <Header />
          <Route exact path="/" render={() => <HomePage />} />
          <Route exact path="/safe" render={() => <SafePageLayout />} />
          <Route exact path="/vault-approve" />
          <Route exact path="/service-account" />
          <Route exact path="/certificates" />
        </Switch>
       
      </Router>
    </div>
  );
}

export default App;
