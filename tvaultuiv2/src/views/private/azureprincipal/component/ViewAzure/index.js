/* eslint-disable react/jsx-wrap-multilines */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Modal from '@material-ui/core/Modal';
import { Backdrop, Typography } from '@material-ui/core';
import Fade from '@material-ui/core/Fade';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import PropTypes from 'prop-types';
import ReactHtmlParser from 'react-html-parser';
import azureIcon from '../../../../../assets/azure-icon.svg';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ConfirmationModal from '../../../../../components/ConfirmationModal';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../../../breakpoints';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import apiService from '../../apiService';
import { GlobalModalWrapper } from '../../../../../styles/GlobalStyles';
import Strings from '../../../../../resources';
import CollapsibleDropdown from '../../../../../components/CollapsibleDropdown';

const { small } = mediaBreakpoints;

const loaderStyle = css`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  color: red;
  z-index: 1;
`;

const IconDescriptionWrapper = styled.div`
  display: flex;
  position: relative;
  margin-top: 3.2rem;
`;

const CertIcon = styled.img`
  height: 5.7rem;
  width: 5rem;
  margin-right: 2rem;
`;

const CertDesc = styled.div``;

const HeaderWrapper = styled.div`
  display: flex;
  align-items: center;
  ${small} {
    margin-top: 1rem;
  }
`;

const Description = styled.p`
  color: #c4c4c4;
  font-size: 1.4rem;
  margin-top: 0;
`;

const InfoLine = styled('p')`
  color: ${(props) => props.theme.customColor.collapse.color};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
  strong {
    margin-right: 0.5rem;
  }
  a {
    color: ${(props) => props.theme.customColor.magenta};
  }
`;
const Span = styled.span`
  color: ${(props) => props.theme.customColor.collapse.title};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
  ${(props) => props.extraStyles}
`;
const CollapsibleContainer = styled.div``;
const ViewMoreStyles = css`
  display: flex;
  align-items: center;
  font-weight: 600;
  cursor: pointer;
  margin-left: 6.5rem;
`;

const Label = styled.p`
  font-size: 1.3rem;
  color: ${(props) => props.theme.customColor.label.color};
  margin-bottom: 0.9rem;
`;

const Value = styled.p`
  font-size: 1.8rem;
`;

const EachDetail = styled.div`
  margin-bottom: 3rem;
  p {
    margin: 0;
  }
`;

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

const ViewAzure = (props) => {
  const { viewAzureData, open, onCloseViewAzureModal } = props;
  const classes = useStyles();
  const [modalDetail, setModalDetail] = useState({
    title: '',
    description: '',
  });
  const [openModal, setOpenModal] = useState({ status: 'confirm' });
  const [loading, setLoading] = useState(true);
  const [azureDetail, setAzureDetail] = useState({});
  const isMobileScreen = useMediaQuery(small);
  const [azureActivated, setAzureActivated] = useState(false);

  const clearData = () => {
    setModalDetail({
      title: '',
      description: '',
    });
  };

  useEffect(() => {
    if (Object.keys(viewAzureData).length > 0) {
      apiService.getAzureserviceDetails(viewAzureData.name).then((res) => {
        setOpenModal({ status: 'view' });
        setLoading(false);
        if (res?.data) {
          setAzureDetail(res.data);
        }
      });
    }
  }, [viewAzureData]);

  const onCloseModal = () => {
    if (!loading) {
      onCloseViewAzureModal(azureActivated);
    }
  };

  const onActivateClicked = () => {
    setOpenModal({ status: 'activate' });
    setModalDetail({
      title: 'Confirm Activation',
      description: Strings.Resources.azureActivateConfirmation,
    });
  };

  const onCancelActivation = () => {
    setOpenModal({ status: 'view' });
    clearData();
  };

  const onConfirmActivation = () => {
    setOpenModal({ status: 'confirm' });
    setLoading(true);
    clearData();
    apiService
      .activateAzureAccount(viewAzureData.name)
      .then(() => {
        setAzureActivated(true);
        setLoading(false);
        setModalDetail({
          title: 'Activation Successful',
          description:
            'Azure Service Principal has been activated. You may also want to assign permissions for other users or groups to view or modify this service account.',
        });
      })
      .catch((err) => {
        setAzureActivated(false);
        setLoading(false);
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setModalDetail({
            title: 'Error',
            description: err.response.data.errors[0],
          });
        } else {
          setModalDetail({
            title: 'Error',
            description: 'Something went wrong',
          });
        }
      });
  };
  return (
    <ComponentError>
      <>
        {openModal.status === 'confirm' && (
          <ConfirmationModal
            open={open}
            handleClose={() => onCloseModal()}
            title={modalDetail.title}
            description={modalDetail.description}
            confirmButton={
              !loading ? (
                <ButtonComponent
                  label="Close"
                  color="secondary"
                  onClick={() => onCloseModal()}
                  width={isMobileScreen ? '100%' : '45%'}
                />
              ) : (
                <LoaderSpinner customStyle={loaderStyle} />
              )
            }
          />
        )}
        {openModal.status === 'activate' && (
          <ConfirmationModal
            open={open}
            handleClose={() => onCancelActivation()}
            title={modalDetail.title}
            description={modalDetail.description}
            cancelButton={
              <ButtonComponent
                label="Close"
                color="primary"
                onClick={() => onCancelActivation()}
                width={isMobileScreen ? '100%' : '45%'}
              />
            }
            confirmButton={
              <ButtonComponent
                label="Activate"
                color="secondary"
                onClick={() => onConfirmActivation()}
                width={isMobileScreen ? '100%' : '45%'}
              />
            }
          />
        )}
        {openModal.status === 'view' && (
          <Modal
            aria-labelledby="transition-modal-title"
            aria-describedby="transition-modal-description"
            className={classes.modal}
            open={open}
            onClose={() => onCloseModal()}
            closeAfterTransition
            BackdropComponent={Backdrop}
            BackdropProps={{
              timeout: 500,
            }}
          >
            <Fade in={open}>
              <GlobalModalWrapper>
                <>
                  <HeaderWrapper>
                    <Typography variant="h5">View Azure Pincipal</Typography>
                  </HeaderWrapper>
                  <IconDescriptionWrapper>
                    <CertIcon src={azureIcon} alt="azure-icon" />
                    <CertDesc>
                      <Description>
                        {Strings.Resources.azurePrincipal}
                      </Description>
                    </CertDesc>
                  </IconDescriptionWrapper>
                  <CollapsibleDropdown
                    titleMore="View More"
                    titleLess="View Less"
                    collapseStyles="background:none"
                    titleCss={ViewMoreStyles}
                  >
                    <CollapsibleContainer>
                      <InfoLine>
                        <Span>
                          <strong>Step 1:</strong>
                        </Span>
                        {ReactHtmlParser(Strings.Resources.azureGuide1)}
                      </InfoLine>
                      <InfoLine>
                        <Span>
                          <strong>Step 2:</strong>
                        </Span>
                        {ReactHtmlParser(Strings.Resources.azureGuide2)}
                      </InfoLine>
                      <InfoLine>
                        <Span>
                          <strong>Step 3:</strong>
                        </Span>
                        {ReactHtmlParser(Strings.Resources.azureGuide3)}
                      </InfoLine>
                    </CollapsibleContainer>
                  </CollapsibleDropdown>
                </>
                <>
                  <EachDetail>
                    <Label>Azure Service Principal Name:</Label>
                    <Value>{azureDetail.servicePrincipalName}</Value>
                  </EachDetail>
                  <EachDetail>
                    <Label>Owner (managedBy):</Label>
                    <Value>{azureDetail.owner_ntid}</Value>
                  </EachDetail>
                  <EachDetail>
                    <Label>Owner Email:</Label>
                    <Value>{azureDetail.owner_email}</Value>
                  </EachDetail>
                  <EachDetail>
                    <Label>Tenant ID:</Label>
                    <Value>{azureDetail.tenantId}</Value>
                  </EachDetail>
                  <EachDetail>
                    <Label>Azure Service Principal Client ID:</Label>
                    <Value>{azureDetail.servicePrincipalClientId}</Value>
                  </EachDetail>
                  <EachDetail>
                    <Label>Azure Service Principal ID:</Label>
                    <Value>{azureDetail.servicePrincipalId}</Value>
                  </EachDetail>
                  <EachDetail>
                    <Label>Created Date:</Label>
                    <Value>{azureDetail.createdDate}</Value>
                  </EachDetail>
                  <EachDetail>
                    <Label>Application Name:</Label>
                    <Value>{azureDetail.application_name}</Value>
                  </EachDetail>
                </>
                <CancelSaveWrapper>
                  <CancelButton>
                    <ButtonComponent
                      label="Cancel"
                      color="primary"
                      onClick={() => onCloseModal()}
                    />
                  </CancelButton>
                  {!azureDetail.isActivated && (
                    <ButtonComponent
                      label="Activate"
                      color="secondary"
                      onClick={() => onActivateClicked()}
                    />
                  )}
                </CancelSaveWrapper>
              </GlobalModalWrapper>
            </Fade>
          </Modal>
        )}
      </>
    </ComponentError>
  );
};

ViewAzure.propTypes = {
  viewAzureData: PropTypes.objectOf(PropTypes.any).isRequired,
  onCloseViewAzureModal: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
};

export default ViewAzure;
