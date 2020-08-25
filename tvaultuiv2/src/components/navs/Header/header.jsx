import React from "react";

import styles from "./header.module.scss";

const Header = () => {
  const navItems = ["safe", "applications", "service accounts", "certificates"];
  return (
    <header className={styles.vaultHeaderContainer}>
      <div className={styles.logoWrapper}>LOGO</div>
      <div className={styles.navWrapper}>
        {navItems.map((item, i) => (
          <div key={i} className={styles.navItem}>
            {item}
          </div>
        ))}
      </div>
      <div className={styles.profileWrapper}></div>
    </header>
  );
};

export default Header;
