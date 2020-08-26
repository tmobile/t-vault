import React, { useState } from "react";

import Radio from "@material-ui/core/Radio";
import RadioGroup from "@material-ui/core/RadioGroup";
import TextField from "@material-ui/core/TextField";
import Autocomplete from "@material-ui/lab/Autocomplete";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import FormControl from "@material-ui/core/FormControl";
import ButtonComponent from "./buttonComponent";
import styled from "styled-components";

const PermissionWrapper = styled.div`
  padding: 2rem;
  width: 50%;
  border: 1.5px solid #000;
  display: flex;
  flex-direction: column;
`;

const InputRadioWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
`;

const InputWrapper = styled.div`
  width: 70%;
  .MuiFormControl-fullWidth {
    background-color: #eee;
    padding: 0.5rem;
  }
  .MuiInput-underline:before,
  .MuiInput-underline:after,
  .MuiInput-underline:hover:not(.Mui-disabled):before {
    border-bottom: none;
  }
  .MuiSvgIcon-root {
    display: none;
  }
`;
const Title = styled.p`
  margin-top: 0;
  margin-bottom: 1rem;
`;

const CancelSaveWrapper = styled.div`
  align-self: flex-end;
`;

const Permissions = (props) => {
	const [value, setValue] = useState("read");
	const [searchValue, setSearchValue] = useState("");

	const handleChange = (event) => {
		setValue(event.target.value);
	};
	const data = [{ title: "abc@tmobile.com" }, { title: "xyz@tmobile.com" }];
	console.log("searchValue :>> ", searchValue);
	return (
		<PermissionWrapper>
			<InputRadioWrapper>
				<InputWrapper>
					<Title>Add User</Title>
					<Autocomplete
						id="combo-box-demo"
						options={data}
						getOptionLabel={(option) => option.title}
						style={{ width: "90%" }}
						renderInput={(params) => (
							<TextField
								{...params}
								placeholder="Search"
								onChange={(e) => setSearchValue(e.target.value)}
							/>
						)}
					/>
				</InputWrapper>
				<div>
					<Title>Permission</Title>
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
			</InputRadioWrapper>
			<CancelSaveWrapper>
				<ButtonComponent label="CANCEL" color="default" type="contained" />
				<ButtonComponent label="SAVE" color="primary" type="contained" />
			</CancelSaveWrapper>
		</PermissionWrapper>
	);
};

export default Permissions;
