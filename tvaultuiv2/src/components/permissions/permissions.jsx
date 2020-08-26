import React, { useState } from "react";

import Radio from "@material-ui/core/Radio";
import { withStyles } from "@material-ui/core/styles";
import RadioGroup from "@material-ui/core/RadioGroup";
import Input from "@material-ui/core/Input";
import InputAdornment from "@material-ui/core/InputAdornment";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import FormControl from "@material-ui/core/FormControl";
import AccountCircle from "@material-ui/icons/Search";
import ButtonComponent from "../buttonComponent";

import "./permissions.scss";
const styles = (theme) => ({
	inputForm: {
		backgroundColor: "#eee",
		padding: "0.5rem",
		width: "90%",
	},
	inputField: {
		"&::after": {
			border: "none",
		},
		"&::before": {
			border: "none",
		},
	},
});
const Permissions = (props) => {
	const { classes } = props;
	const [value, setValue] = useState("read");
	const [, setSearchValue] = useState("");

	const handleChange = (event) => {
		setValue(event.target.value);
	};

	return (
		<section className="permissions-container">
			<div className="permissions-content">
				<div className="permissions-content__user">
					<p className="permissions-heading">Add User</p>
					<FormControl className={classes.inputForm}>
						<Input
							placeholder="Search"
							id="input-with-icon-adornment"
							className={classes.inputField}
							onChange={(e) => setSearchValue(e.target.value)}
							startAdornment={
								<InputAdornment position="start">
									<AccountCircle />
								</InputAdornment>
							}
						/>
					</FormControl>
				</div>
				<div className="permissions-content__permission">
					<p className="permissions-heading">Permission</p>
					<FormControl component="fieldset">
						<RadioGroup
							row
							aria-label="permissions"
							name="permissions1"
							value={value}
							onChange={handleChange}
						>
							<FormControlLabel
								value="read"
								control={<Radio color="default" />}
								label="Read"
							/>
							<FormControlLabel
								value="write"
								control={<Radio color="default" />}
								label="Write"
							/>
						</RadioGroup>
					</FormControl>
				</div>
			</div>
			<div className="cancel-save-button">
				<ButtonComponent label="CANCEL" color="default" type="contained" />
				<ButtonComponent
					label="SAVE"
					type="contained"
					classApplied="containedSecondary"
				/>
			</div>
		</section>
	);
};

export default withStyles(styles)(Permissions);
