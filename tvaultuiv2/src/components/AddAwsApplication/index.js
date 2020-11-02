/* eslint-disable no-nested-ternary */
import React, { useState, useEffect, useReducer } from 'react';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import { InputLabel } from '@material-ui/core';
import RadioGroup from '@material-ui/core/RadioGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Radio from '@material-ui/core/Radio';
import PropTypes from 'prop-types';
import { SubHeading } from '../../styles/GlobalStyles';
import TextFieldComponent from '../FormFields/TextField';
import ButtonComponent from '../FormFields/ActionButton';
import mediaBreakpoints from '../../breakpoints';
import { ColorBackArrow } from '../../assets/SvgIcons';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import RadioButtonComponent from '../FormFields/RadioButton';

const { small } = mediaBreakpoints;
const ContainerWrapper = styled.section``;

const InputAwsWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  margin: 2rem 0;
`;

const AuthWrapper = styled.div`
  margin: 2rem 0;
`;

const EachInputField = styled.div`
  width: 48%;
`;

const CancelSaveWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
  ${small} {
    margin-top: 5.3rem;
  }
  button {
    ${small} {
      height: 4.5rem;
    }
  }
`;

const CancelButton = styled.div`
  margin-right: 0.8rem;
  ${small} {
    margin-right: 1rem;
    width: 100%;
  }
`;

const BackButton = styled.span`
  display: none;
  ${small} {
    display: flex;
    align-items: center;
    margin-right: 1.4rem;
    margin-top: 0.5rem;
  }
`;

const extraCss = css`
  display: flex;
  ${small} {
    margin-bottom: 4rem;
  }
`;

const RadioWrapper = styled.div``;

const AddAwsApplication = (props) => {
  const { handleSaveClick, handleCancelClick, isSvcAccount } = props;
  const [awsAuthenticationType, setAwsAuthenticationType] = useState('ec2');
  const [roleName, setRoleName] = useState('');
  const [iamPrincipalArn, setIamPrincipalArn] = useState('');
  const [radioValue, setRadioValue] = useState('read');
  const [isEC2, setIsEC2] = useState(true);
  const isMobileScreen = useMediaQuery(small);
  const [disabledSave, setDisabledSave] = useState(true);
  const [principalError, setPrincipalError] = useState(false);
  const [count, setCount] = useState(0);

  const initialState = {
    vpcId: '',
    iamRoleArn: '',
    accountId: '',
    subnetId: '',
    amiId: '',
    profileArn: '',
    region: '',
  };
  const reducer = (state, { field, value }) => {
    return {
      ...state,
      [field]: value,
    };
  };
  const [state, dispatch] = useReducer(reducer, initialState);

  const onChange = (e) => {
    dispatch({ field: e.target.name, value: e.target.value });
  };

  const {
    vpcId,
    accountId,
    iamRoleArn,
    region,
    subnetId,
    profileArn,
    amiId,
  } = state;

  useEffect(() => {
    let val = 0;
    Object.values(state).map((value) => {
      if (value === null || value === '') {
        val += 1;
        return setCount(val);
      }
      return null;
    });
  }, [state]);

  const checkValidArn = (text) => {
    if (text) {
      const res = /^arn:aws:\S+::\d{12}/;
      return res.test(text);
    }
    return null;
  };

  useEffect(() => {
    if (!isEC2) {
      if (roleName?.length < 3 || principalError) {
        setDisabledSave(true);
      } else {
        setDisabledSave(false);
      }
    } else if (roleName?.length < 3 || count > 5) {
      setDisabledSave(true);
    } else {
      setDisabledSave(false);
    }
  }, [roleName, isEC2, iamPrincipalArn, principalError, count]);

  const handleAwsRadioChange = (event) => {
    setAwsAuthenticationType(event.target.value);
    if (event.target.value === 'ec2') {
      setIsEC2(true);
    } else {
      setIsEC2(false);
    }
  };

  const onCreateClicked = () => {
    let data = {};
    if (isEC2) {
      data = {
        auth_type: awsAuthenticationType,
        bound_ami_id: amiId,
        bound_iam_instance_profile_arn: profileArn,
        bound_iam_principal_arn: '',
        bound_iam_role_arn: iamRoleArn,
        bound_region: region,
        bound_subnet_id: subnetId,
        bound_vpc_id: vpcId,
        policies: '',
        resolve_aws_unique_ids: 'false',
        role: roleName,
      };
    } else {
      data = {
        auth_type: awsAuthenticationType,
        bound_account_id: '',
        bound_ami_id: '',
        bound_iam_instance_profile_arn: '',
        bound_iam_principal_arn: [iamPrincipalArn],
        bound_iam_role_arn: '',
        bound_region: '',
        bound_subnet_id: '',
        bound_vpc_id: '',
        policies: [],
        resolve_aws_unique_ids: 'false',
        role: roleName,
      };
    }
    handleSaveClick(data, radioValue);
  };

  const onIamPrincipalChange = (text) => {
    setIamPrincipalArn(text);
    if (!checkValidArn(text)) {
      setPrincipalError(true);
    } else {
      setPrincipalError(false);
    }
  };

  return (
    <ComponentError>
      <ContainerWrapper>
        <SubHeading extraCss={extraCss}>
          {isMobileScreen && (
            <BackButton onClick={() => handleCancelClick()}>
              <ColorBackArrow />
            </BackButton>
          )}
          Create AWS Configuration
        </SubHeading>
        <AuthWrapper>
          <InputLabel required>AWS Authentication Type</InputLabel>
          <RadioGroup
            row
            aria-label="awsauthentication"
            name="awsauthentication"
            value={awsAuthenticationType}
            onChange={handleAwsRadioChange}
          >
            <FormControlLabel
              value="ec2"
              control={<Radio color="default" />}
              label="EC2"
            />
            <FormControlLabel
              value="iam"
              control={<Radio color="default" />}
              label="IAM"
            />
          </RadioGroup>
        </AuthWrapper>
        <InputLabel required>Role Name</InputLabel>
        <TextFieldComponent
          value={roleName}
          placeholder="Role name- Enter min 3 charactes"
          fullWidth
          name="roleName"
          onChange={(e) => setRoleName(e.target.value)}
        />
        {isEC2 && <p>**Please fill atleast one of the followings.</p>}
        <InputAwsWrapper>
          <EachInputField>
            <InputLabel required>Account ID*</InputLabel>
            <TextFieldComponent
              value={accountId}
              placeholder="Account ID"
              fullWidth
              type="number"
              readOnly={!isEC2}
              name="accountId"
              onChange={(e) => onChange(e)}
            />
          </EachInputField>
          <EachInputField>
            <InputLabel required>Region*</InputLabel>
            <TextFieldComponent
              value={region}
              placeholder="Region"
              fullWidth
              readOnly={!isEC2}
              name="region"
              onChange={(e) => onChange(e)}
            />
          </EachInputField>
        </InputAwsWrapper>
        <InputAwsWrapper>
          <EachInputField>
            <InputLabel required>VPC ID*</InputLabel>
            <TextFieldComponent
              value={vpcId}
              placeholder="VPC ID"
              fullWidth
              readOnly={!isEC2}
              name="vpcId"
              onChange={(e) => onChange(e)}
            />
          </EachInputField>
          <EachInputField>
            <InputLabel required>Subnet ID*</InputLabel>
            <TextFieldComponent
              value={subnetId}
              placeholder="Subnet ID"
              fullWidth
              readOnly={!isEC2}
              name="subnetId"
              onChange={(e) => onChange(e)}
            />
          </EachInputField>
        </InputAwsWrapper>
        <InputAwsWrapper>
          <EachInputField>
            <InputLabel required>AMI ID*</InputLabel>
            <TextFieldComponent
              value={amiId}
              placeholder="AMI ID"
              fullWidth
              readOnly={!isEC2}
              name="amiId"
              onChange={(e) => onChange(e)}
            />
          </EachInputField>
          <EachInputField>
            <InputLabel required>Instance Profile ARN*</InputLabel>
            <TextFieldComponent
              value={profileArn}
              placeholder="Instance Profile ARN"
              fullWidth
              readOnly={!isEC2}
              name="profileArn"
              onChange={(e) => onChange(e)}
            />
          </EachInputField>
        </InputAwsWrapper>
        <InputAwsWrapper>
          <EachInputField>
            <InputLabel required>IAM Role ARN*</InputLabel>
            <TextFieldComponent
              value={iamRoleArn}
              placeholder="IAM Role ARN"
              fullWidth
              readOnly={!isEC2}
              name="iamRoleArn"
              onChange={(e) => onChange(e)}
            />
          </EachInputField>
          <EachInputField>
            <InputLabel required>IAM Principal ARN</InputLabel>
            <TextFieldComponent
              value={iamPrincipalArn}
              placeholder="IAM Principal ARN"
              fullWidth
              readOnly={isEC2}
              name="iamPrincipalArn"
              onChange={(e) => onIamPrincipalChange(e.target.value)}
              error={principalError}
              helperText={principalError ? 'Please enter valid arn!' : ''}
            />
          </EachInputField>
        </InputAwsWrapper>
        <RadioWrapper>
          <InputLabel required>Permission</InputLabel>
          <RadioButtonComponent
            menu={
              isSvcAccount
                ? ['read', 'reset', 'deny']
                : ['read', 'write', 'deny']
            }
            handleChange={(e) => setRadioValue(e.target.value)}
            value={radioValue}
          />
        </RadioWrapper>
        <CancelSaveWrapper>
          <CancelButton>
            <ButtonComponent
              label="Cancel"
              color="primary"
              onClick={() => handleCancelClick()}
              width={isMobileScreen ? '100%' : ''}
            />
          </CancelButton>
          <ButtonComponent
            label="Create"
            color="secondary"
            icon="add"
            disabled={disabledSave}
            onClick={() => onCreateClicked()}
            width={isMobileScreen ? '100%' : ''}
          />
        </CancelSaveWrapper>
      </ContainerWrapper>
    </ComponentError>
  );
};
AddAwsApplication.propTypes = {
  handleCancelClick: PropTypes.func.isRequired,
  handleSaveClick: PropTypes.func.isRequired,
  isSvcAccount: PropTypes.bool,
};

AddAwsApplication.defaultProps = {
  isSvcAccount: false,
};
export default AddAwsApplication;
