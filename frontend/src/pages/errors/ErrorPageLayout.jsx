import { Link } from "react-router-dom";
import "./ErrorPage.css";

function ErrorPageLayout({
  code = "Error",
  title = "Something went wrong",
  description = "An unexpected error occurred.",
  primaryText = "Go to dashboard",
  primaryTo = "/dashboard",
  secondaryText = "Back to login",
  secondaryTo = "/login",
}) {
  return (
    <div className="error-page">
      <div className="error-page__card">
        <div className="error-page__code">{code}</div>
        <h1 className="error-page__title">{title}</h1>
        <p className="error-page__description">{description}</p>

        <div className="error-page__actions">
          <Link to={primaryTo} className="error-page__button error-page__button--primary">
            {primaryText}
          </Link>
          <Link to={secondaryTo} className="error-page__button error-page__button--ghost">
            {secondaryText}
          </Link>
        </div>
      </div>
    </div>
  );
}

export default ErrorPageLayout;