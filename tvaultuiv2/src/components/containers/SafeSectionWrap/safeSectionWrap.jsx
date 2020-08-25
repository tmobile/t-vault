import React from "react";
import styles from "./safeSectionWrap.module.scss";
const SafeSectionWrap = () => {
  return (
    <main title="safe-section" className={styles.safeSectionContainer}>
      <section>left section</section>
      <section>right section</section>
    </main>
  );
};

export default SafeSectionWrap;
