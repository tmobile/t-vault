import React from "react";
import { render } from "./node_modules/@testing-library/react";
import SafePageLayout from "./safePageLayout";

test("Loads home page with welcome text", () => {
  const { getByTitle } = render(<SafePageLayout></SafePageLayout>);

  expect(getByTitle("id")).toHaveTextContent("this is safe page container");
});
