/* eslint-disable react/jsx-props-no-spreading */
import React, { useEffect, useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Popper from '@material-ui/core/Popper';
import Fade from '@material-ui/core/Fade';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { useHistory } from 'react-router-dom';
import userIcon from '../../assets/icon-profile.svg';
import vectorIcon from '../../assets/vector.svg';
import { revokeToken } from '../../views/public/HomePage/utils';
import configData from '../../config/config';
import mediaBreakpoints from '../../breakpoints';

const UserWrap = styled.div`
  display: flex;
  align-items: center;
  cursor: pointer;
  ${mediaBreakpoints.smallAndMedium} {
    margin-top: 2rem;
  }
`;

const UserName = styled.span`
  text-transform: capitalize;
`;

const UserIcon = styled.img`
  margin: 0 0.5rem;
`;

const VectorIcon = styled.img`
  margin-left: 0.4rem;
`;

const Logout = styled.div`
  cursor: pointer;
`;

const AdminLabel = styled.div``;

const useStyles = makeStyles((theme) => ({
  root: {
    zIndex: '2000',
  },
  paper: {
    backgroundColor: theme.palette.background.paper,
    padding: theme.spacing(1),
  },
}));

const UserLogout = (props) => {
  const { userName, checkToken } = props;
  const classes = useStyles();
  const [isAdmin, setIsAdmin] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const history = useHistory();

  const handleClick = (event) => {
    setAnchorEl(anchorEl ? null : event.currentTarget);
  };

  const onLogoutClicked = async () => {
    if (configData.AUTH_TYPE === 'oidc') {
      await revokeToken();
    }
    localStorage.clear();
    checkToken();
    history.push('/');
  };

  const open = Boolean(anchorEl);
  const id = open ? 'transitions-popper' : undefined;
  useEffect(() => {
    const admin = localStorage.getItem('isAdmin');
    if (admin) {
      setIsAdmin(JSON.parse(admin));
    }
  }, []);
  return (
    <>
      <UserWrap aria-describedby={id} onClick={handleClick}>
        <UserIcon src={userIcon} alt="usericon" />
        <div>
          <UserName>{userName}</UserName>
          <VectorIcon src={vectorIcon} alt="vectoricon" />
          <div>{isAdmin ? <AdminLabel>Admin</AdminLabel> : <></>}</div>
        </div>
      </UserWrap>
      <Popper
        id={id}
        open={open}
        anchorEl={anchorEl}
        transition
        className={classes.root}
      >
        {({ TransitionProps }) => (
          <Fade {...TransitionProps} timeout={350}>
            <Logout className={classes.paper} onClick={() => onLogoutClicked()}>
              Logout
            </Logout>
          </Fade>
        )}
      </Popper>
    </>
  );
};

UserLogout.propTypes = {
  userName: PropTypes.string.isRequired,
  checkToken: PropTypes.func.isRequired,
};

export default UserLogout;
