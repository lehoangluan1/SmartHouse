import ErrorPageLayout from "./ErrorPageLayout";

function ForbiddenPage() {
  return (
    <ErrorPageLayout
      code="403"
      title="Forbidden"
      description="You do not have permission to access this feature or page."
    />
  );
}

export default ForbiddenPage;