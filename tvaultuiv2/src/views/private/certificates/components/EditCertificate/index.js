/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import certIcon from '../../../../../assets/cert-icon.svg';
import leftArrowIcon from '../../../../../assets/left-arrow.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import PreviewCertificate from '../../CreateCertificates/preview';

const { small, belowLarge } = mediaBreakpoints;

const ModalWrapper = styled.section`
  background-color: ${(props) => props.theme.palette.background.modal};
  padding: 5.5rem 6rem 6rem 6rem;
  border: none;
  outline: none;
  width: 69.6rem;
  margin: auto 0;
  display: flex;
  flex-direction: column;
  position: relative;
  ${belowLarge} {
    padding: 2.7rem 5rem 3.2rem 5rem;
    width: 57.2rem;
  }
  ${small} {
    width: 100%;
    padding: 2rem;
    margin: 0;
    height: fit-content;
  }
`;

const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  ${small} {
    margin-top: 1rem;
  }
`;

const LeftIcon = styled.img`
  display: none;
  ${small} {
    display: block;
    margin-right: 1.4rem;
    margin-top: 0.3rem;
  }
`;
const IconDescriptionWrapper = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
  position: relative;
  margin-top: 3.2rem;
`;

const SafeIcon = styled.img`
  height: 5.7rem;
  width: 5rem;
  margin-right: 2rem;
`;

const PreviewWrap = styled.div``;

const ContainerOwnerWrap = styled.div`
  font-size: 1.4rem;
`;

const Container = styled.div``;
const Owner = styled.div``;
const SideLabel = styled.span`
  color: #8b8ea6;
  margin-right: 0.3rem;
`;

const SideValue = styled.span``;

const CancelSaveWrapper = styled.div`
  display: ${(props) => (props.showPreview ? 'none' : 'flex')};
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
const loaderStyle = css`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  color: red;
  z-index: 1;
`;

const Label = styled.p`
  font-size: 1.3rem;
  color: #8b8ea6;
  margin-bottom: 0.9rem;
`;

const Value = styled.p`
  font-size: 1.8rem;
  text-transform: capitalize;
`;

const EachDetail = styled.div`
  margin-bottom: 3rem;
  p {
    margin: 0;
  }
`;

const useStyles = makeStyles((theme) => ({
  select: {
    '&.MuiFilledInput-root.Mui-focused': {
      backgroundColor: '#fff',
    },
  },
  dropdownStyle: {
    backgroundColor: '#fff',
  },
  modal: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflowY: 'auto',
    padding: '10rem 0',
    [theme.breakpoints.down('xs')]: {
      alignItems: 'unset',
      justifyContent: 'unset',
      padding: '0',
      height: '100%',
    },
  },
}));

const EditCertificate = (props) => {
  const { open, ListItemDetails } = props;
  const classes = useStyles();
  const [responseType] = useState(null);
  const isMobileScreen = useMediaQuery(small);

  return (
    <ComponentError>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={open}
        onClose={() => {}}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <Fade in={open}>
          <ModalWrapper>
            {responseType === 0 && <LoaderSpinner customStyle={loaderStyle} />}
            <HeaderWrapper>
              <LeftIcon src={leftArrowIcon} alt="go-back" onClick={() => {}} />
              <Typography variant="h5">Edit Certificate</Typography>
            </HeaderWrapper>
            <IconDescriptionWrapper>
              <SafeIcon src={certIcon} alt="cert-icon" />
              <ContainerOwnerWrap>
                <Container>
                  <SideLabel>Container:</SideLabel>
                  <SideValue>{ListItemDetails.containerName}</SideValue>
                </Container>
                <Owner>
                  <SideLabel>Owner Email:</SideLabel>
                  <SideValue>{ListItemDetails.certOwnerEmailId}</SideValue>
                </Owner>
              </ContainerOwnerWrap>
            </IconDescriptionWrapper>
            <PreviewWrap>
              <PreviewCertificate
                dns={ListItemDetails.dnsNames}
                certificateType={ListItemDetails.certType}
                applicationName={ListItemDetails.applicationName}
                certName={ListItemDetails.certificateName}
                isEditCertificate
              />
              <EachDetail>
                <Label>Status:</Label>
                <Value>{ListItemDetails.certificateStatus}</Value>
              </EachDetail>
              <EachDetail>
                <Label>Create Data:</Label>
                <Value>{ListItemDetails.createDate}</Value>
              </EachDetail>
              <EachDetail>
                <Label>Expiry Date:</Label>
                <Value>{ListItemDetails.expiryDate}</Value>
              </EachDetail>
              <EachDetail>
                <Label>Signature Algorithm:</Label>
                <Value>SHA256-RSA</Value>
              </EachDetail>
              <EachDetail>
                <Label>Key Usage:</Label>
                <Value>digitalSignature, keyEncipherment</Value>
              </EachDetail>
              <EachDetail>
                <Label>Extended Key Usage:</Label>
                <Value>serverAuth</Value>
              </EachDetail>
              <EachDetail>
                <Label>Enroll Service:</Label>
                <Value>T-Mobile Issuing CA 01 - SHA2</Value>
              </EachDetail>
            </PreviewWrap>
            <CancelSaveWrapper>
              <CancelButton>
                <ButtonComponent
                  label="Cancel"
                  color="primary"
                  onClick={() => {}}
                  width={isMobileScreen ? '100%' : ''}
                />
              </CancelButton>
              <CancelButton>
                <ButtonComponent
                  label="Revoke"
                  color="secondary"
                  onClick={() => {}}
                  width={isMobileScreen ? '100%' : ''}
                />
              </CancelButton>
              <ButtonComponent
                label="Renew"
                color="secondary"
                onClick={() => {}}
                width={isMobileScreen ? '100%' : ''}
              />
            </CancelSaveWrapper>
          </ModalWrapper>
        </Fade>
      </Modal>
    </ComponentError>
  );
};

EditCertificate.propTypes = {
  ListItemDetails: PropTypes.objectOf(PropTypes.any).isRequired,
  open: PropTypes.bool.isRequired,
};

export default EditCertificate;
