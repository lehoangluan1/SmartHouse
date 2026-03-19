import ErrorPageLayout from "./ErrorPageLayout";

function InternalServerErrorPage() {
  return (
    <ErrorPageLayout
      code="500"
      title="Internal Server Error"
      description="The system encountered an internal error. Please try again later."
    />
  );
}

export default InternalServerErrorPage;