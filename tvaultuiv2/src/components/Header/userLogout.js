/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Popper from '@material-ui/core/Popper';
import Fade from '@material-ui/core/Fade';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { useHistory } from 'react-router-dom';
import userIcon from '../../assets/icon-profile.svg';
import { revokeToken } from '../../views/public/HomePage/utils';

const UserWrap = styled.div`
  display: flex;
  align-items: center;
  cursor: pointer;
`;
const UserName = styled.span``;

const UserIcon = styled.img`
  margin: 0 0.5rem;
`;

const Logout = styled.div`
  cursor: pointer;
`;

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
  const [anchorEl, setAnchorEl] = React.useState(null);
  const history = useHistory();

  const handleClick = (event) => {
    setAnchorEl(anchorEl ? null : event.currentTarget);
  };

  const onLogoutClicked = () => {
    revokeToken();
    sessionStorage.clear();
    checkToken();
    history.push('/');
  };

  const open = Boolean(anchorEl);
  const id = open ? 'transitions-popper' : undefined;

  return (
    <>
      <UserWrap aria-describedby={id} onClick={handleClick}>
        <UserIcon src={userIcon} alt="usericon" />
        <UserName>{userName}</UserName>
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
