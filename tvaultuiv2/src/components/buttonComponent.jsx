import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Icon from '@material-ui/core/Icon';

const styles = theme => ({
	contained: {
		marginRight: "1rem",
		padding: '0.5rem 1rem',
		boxShadow: 'none',
		color: "#fff"
	},
	iconStyle: {
		fontSize: '12px',
		fontWeight: 'bold',
		marginRight: '5px',
	},
});
const setIcon = (props) => {
	const { classes, icon } = props;
	return <Icon className={classes.iconStyle}>{icon}</Icon>;
};

const onButtonClick = (e) => {
};

const ButtonComponent = (props) => {
	const { icon, classes, label, onClick, type, size, color, disabled, classApplied } = props;
	return (
		<Button classes={classes}
			variant={type || 'text'}
			size={size || 'small'}
			className={classes[classApplied]}
			color={color || 'default'}
			disabled={disabled || false}
			onClick={onClick ? onClick : (e) => onButtonClick(e)}>
			{icon && setIcon({ ...props })}
			{label}
		</Button>
	);
};

export default withStyles(styles)(ButtonComponent);
