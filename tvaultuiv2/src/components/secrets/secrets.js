import React, { useState } from 'react';
import "./secrets.css"
const Secrets = () => {
	const [secret, setSecret] = useState("");
	const [keyId, setKeyId] = useState("");
	return (
		<section className="secret-container">
			<div className="secret-content">
				<div className="secret-content__input">
					<label className="secret-input-label">Key ID*</label>
					<input type="text" value={keyId} onChange={(e) => setKeyId(e.target.value)} className="secret-input-field" />
					<p className="input-requirements">Please enter a minimum of 3 characters lowercase alphabets, number and underscores only</p>
				</div>
				<div className="secret-content__input">
					<label className="secret-input-label">Secret</label>
					<input type="text" value={secret} onChange={(e) => setSecret(e.target.value)} className="secret-input-field" />
				</div>
			</div>

		</section>
	)
}

export default Secrets;
