import ErrorPageLayout from "./ErrorPageLayout";

function BadRequestPage() {
  return (
    <ErrorPageLayout
      code="400"
      title="Bad Request"
      description="The request is invalid or the data submitted is not in the correct format."
    />
  );
}

export default BadRequestPage;