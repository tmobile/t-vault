import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';

const styles = theme => ({
	contained: {
		backgroundColor: "#ccc",
		color: '#fff',
		marginRight: "1rem",
		padding: '0.5rem 1rem',
		'&:hover': {
			backgroundColor: '#ccc',
		},
		boxShadow: 'none',
	},
	containedSecondary: {
		color: '#fff',
		background: '#000',
		padding: '0.5rem 1rem',
		'&:hover': {
			backgroundColor: '#000',
		},
	},
});

const onButtonClick = (e) => {
};

const ButtonComponent = (props) => {
	const { classes, label, onClick, type, size, color, disabled, classApplied } = props;
	return (
		<Button classes={classes}
			variant={type || 'text'}
			size={size || 'small'}
			className={classes[classApplied]}
			color={color || 'default'}
			disabled={disabled || false}
			onClick={onClick ? onClick : (e) => onButtonClick(e)}>
			{label}
		</Button>
	);
};

export default withStyles(styles)(ButtonComponent);
