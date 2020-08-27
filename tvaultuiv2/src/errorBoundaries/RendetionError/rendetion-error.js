import React from 'react';
import { PropTypes } from 'prop-types';

class RendetionError extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null, errorInfo: null };
  }

  componentDidCatch(error, errorInfo) {
    // Catch errors in any components below and re-render with error message
    this.setState({
      error,
      errorInfo,
    });
    // You can also log error messages to an error reporting service here
  }

  render() {
    const { errorInfo, error } = this.state;
    const { children } = this.props;
    if (errorInfo) {
      // eslint-disable-next-line no-console
      console.error('rendetion', error, errorInfo);
      return (
        <div>
          <h2>Oops!! Something is nor right!!</h2>
        </div>
      );
    }
    // Normally, just render children
    return children;
  }
}

RendetionError.propTypes = { children: PropTypes.element.isRequired };

export default RendetionError;
