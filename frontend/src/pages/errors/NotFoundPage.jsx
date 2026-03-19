import ErrorPageLayout from "./ErrorPageLayout";

function NotFoundPage() {
  return (
    <ErrorPageLayout
      code="404"
      title="Page Not Found"
      description="The page you are looking for does not exist or has been moved."
    />
  );
}

export default NotFoundPage;