// import React from 'react';
// import { render } from '@testing-library/react';
// import App from './App';

// test('renders learn react link', () => {
//   const { getByText } = render(<App />);
//   const linkElement = getByText(/learn react/i);
//   expect(linkElement).toBeInTheDocument();
// });

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

import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';

import * as customStore from 'customStore';
import App from './App';
import { shallow } from 'enzyme';

it('renders without crashing', () => {
  const div = document.createElement('div');

  ReactDOM.render(
    <BrowserRouter>
      <App {...props} />
    </BrowserRouter>,
    div
  );

  ReactDOM.unmountComponentAtNode(div);

  const wrapper = shallow(<App {...props} store={store} />);
  wrapper.dive().instance().componentDidUpdate(props);
});
