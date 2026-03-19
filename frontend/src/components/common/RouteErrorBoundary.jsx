import React from "react";
import InternalServerErrorPage from "../../pages/errors/InternalServerErrorPage";

class RouteErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error("RouteErrorBoundary:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return <InternalServerErrorPage />;
    }

    return this.props.children;
  }
}

export default RouteErrorBoundary;