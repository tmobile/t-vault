/* Copyright 2019 T-Mobile USA, Inc.

All information contained herein is, and remains the property of
T-Mobile USA, Inc. and its suppliers, if any, and may be covered
by U.S. and Foreign Patents, patents in process, and are protected
by trade secret or copyright law. You may view more information at

    https://www.t-mobile.com/responsibility/legal

Dissemination of this information or reproduction of this material
is strictly forbidden unless prior written permission is obtained
from T-Mobile USA, Inc.
----------------------------------------------------------------------------- */

import React from "./node_modules/react";
import ReactDOM from "./node_modules/react-dom";
import { BrowserRouter } from "./node_modules/react-router-dom";
import PostLogin from "./PostLogin";
import { shallow } from "./node_modules/enzyme";

const props = {};

it("renders without crashing", () => {
  const div = document.createElement("div");

  ReactDOM.render(
    <BrowserRouter>
      <SafeSectionWrap {...props} />
    </BrowserRouter>,
    div
  );

  ReactDOM.unmountComponentAtNode(div);

  const wrapper = shallow(<SafeSectionWrap {...props} />);
  wrapper.dive().instance().componentDidUpdate(props);
});
