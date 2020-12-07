/* eslint-disable no-param-reassign */
/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import {
  Route,
  Switch,
  useHistory,
  Redirect,
  useLocation,
} from 'react-router-dom';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import sectionHeaderBg from '../../../../../assets/certificate-banner.svg';
import sectionMobHeaderBg from '../../../../../assets/mob-certbg.png';
import sectionTabHeaderBg from '../../../../../assets/tab-certbg.png';
import mediaBreakpoints from '../../../../../breakpoints';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import noCertificateIcon from '../../../../../assets/nocertificate.svg';
import FloatingActionButtonComponent from '../../../../../components/FormFields/FloatingActionButton';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import Error from '../../../../../components/Error';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import CertificatesReviewDetails from '../CertificatesReviewDetails';
import CertificateItemDetail from '../CertificateItemDetail';
import apiService from '../../apiService';
import EditCertificate from '../EditCertificate';
import TransferCertificate from '../TransferCertificateOwner';
import DeletionConfirmationModal from './components/DeletionConfirmationModal';
import CreateCertificates from '../../CreateCertificates';
import LeftColumn from './components/LeftColumn';
import { useStateValue } from '../../../../../contexts/globalState';
import SelectWithCountComponent from '../../../../../components/FormFields/SelectWithCount';
import {
  ListContainer,
  ListContent,
} from '../../../../../styles/GlobalStyles/listingStyle';
import configData from '../../../../../config/config';
import CertificateRelease from '../CertificateRelease';
import SnackbarComponent from '../../../../../components/Snackbar';
import OnboardCertificates from '../OnboardCertificate';

const ColumnSection = styled('section')`
  position: relative;
  background: ${(props) => props.backgroundColor || '#151820'};
`;

const RightColumnSection = styled(ColumnSection)`
  width: 59.23%;
  padding: 0;
  background: linear-gradient(to top, #151820, #2c3040);
  ${mediaBreakpoints.small} {
    width: 100%;
    ${(props) => props.mobileViewStyles}
    display: ${(props) => (props.isDetailsOpen ? 'block' : 'none')};
  }
`;
const LeftColumnSection = styled(ColumnSection)`
  width: 40.77%;
  ${mediaBreakpoints.small} {
    display: ${(props) => (props.isDetailsOpen ? 'none' : 'block')};
    width: 100%;
  }
`;

const SectionPreview = styled('main')`
  display: flex;
  height: 100%;
`;
const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  padding: 0.5em;
  justify-content: space-between;
  border-bottom: 0.1rem solid #1d212c;
`;

const NoDataWrapper = styled.div`
  height: 61vh;
  display: flex;
  justify-content: center;
  align-items: center;
  color: ${(props) => props.theme.customColor.secondary.color};
  span {
    margin: 0 0.4rem;
    color: #fff;
    font-weight: bold;
    text-transform: uppercase;
  }
`;

const NoListWrap = styled.div`
  width: 35%;
`;

const FloatBtnWrapper = styled('div')`
  position: absolute;
  bottom: 1rem;
  right: 2.5rem;
  z-index: 1;
`;

const SearchWrap = styled.div`
  width: 100%;
`;

const MobileViewForListDetailPage = css`
  position: fixed;
  display: flex;
  right: 0;
  left: 0;
  overflow-y: auto;
  max-height: 100%;
  z-index: 20;
  bottom: 0;
  top: 0;
  overflow-y: auto;
`;
const EmptyContentBox = styled('div')`
  width: 100%;
  position: absolute;
  display: flex;
  justify-content: center;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
`;

const SearchFilterNotAvailable = styled.p`
  width: 80%;
  text-align: center;
  word-break: break-all;
`;

const useStyles = makeStyles((theme) => ({
  contained: { borderRadius: '0.4rem' },
  select: {
    backgroundColor: 'transparent',
    fontSize: '1.6rem',
    textTransform: 'uppercase',
    color: '#fff',
    fontWeight: 'bold',
    maxWidth: '28rem',
    marginRight: '2.5rem',
    [theme.breakpoints.down('sm')]: {
      maxWidth: '16rem',
    },
    '& .Mui-selected': {
      color: 'red',
    },
  },
}));

const CertificatesDashboard = () => {
  const [inputSearchValue, setInputSearchValue] = useState('');
  const [certificateList, setCertificateList] = useState([]);
  const [certificateType, setCertificateType] = useState('All Certificates');
  const [menu, setMenu] = useState([]);
  const [response, setResponse] = useState({ status: 'success' });
  const [errorMsg, setErrorMsg] = useState('');
  const [allCertList, setAllCertList] = useState([]);
  const [certificateClicked, setCertificateClicked] = useState(false);
  const [ListItemDetails, setListItemDetails] = useState({});
  const [certificateData, setCertificateData] = useState({});
  const [openTransferModal, setOpenTransferModal] = useState(false);
  const [openDeleteConfirmation, setOpenDeleteConfirmation] = useState(false);
  const [deleteResponse, setDeleteResponse] = useState(false);
  const [deleteError, setDeleteError] = useState(false);
  const [deleteConfirmClicked, setDeleteConfirmClicked] = useState(false);
  const [openReleaseModal, setOpenReleaseModal] = useState(false);
  const [deleteModalDetail, setDeleteModalDetail] = useState({
    title: '',
    description: '',
  });
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [openOnboardModal, setOpenOnboardModal] = useState(false);
  const classes = useStyles();
  const history = useHistory();
  const location = useLocation();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const isTabAndMobileScreen = useMediaQuery(mediaBreakpoints.smallAndMedium);
  const isTabScreen = useMediaQuery(mediaBreakpoints.medium);
  const [state] = useStateValue();
  const admin = Boolean(state?.isAdmin);

  const compareCertificates = (array1, array2, type) => {
    if (array2.length > 0) {
      array2.map((item) => {
        if (!array1.some((list) => list.certificateName === item)) {
          const obj = {
            certificateName: item,
            certType: type,
          };
          array1.push(obj);
        }
        return null;
      });
    }
  };

  /**
   * @function fetchData
   * @description function call all certificates api.
   */
  const fetchAdminData = useCallback(async () => {
    setResponse({ status: 'loading' });
    setAllCertList([]);
    setCertificateList([]);
    let allCertInternal = [];
    if (configData.AUTH_TYPE === 'oidc') {
      allCertInternal = await apiService.getAllAdminCertInternal();
    }
    const internalCertificates = await apiService.getInternalCertificates();
    const externalCertificates = await apiService.getExternalCertificates();
    const onboardCertificates = await apiService.getOnboardCertificates();
    const allApiResponse = Promise.all([
      allCertInternal,
      internalCertificates,
      externalCertificates,
      onboardCertificates,
    ]);
    allApiResponse
      .then((result) => {
        const allCertArray = [];
        const internalCertArray = [];
        const externalCertArray = [];
        const onboardCertArray = [];
        if (configData.AUTH_TYPE === 'oidc') {
          if (result && result[0]?.data?.data?.keys) {
            result[0].data.data.keys.map((item) => {
              return allCertArray.push(item);
            });
          }
        } else {
          const access = JSON.parse(localStorage.getItem('access'));
          if (Object.keys(access).length > 0) {
            Object.keys(access).forEach((item) => {
              if (item === 'cert' || item === 'externalcerts') {
                access[item].map((ele) => {
                  const val = Object.keys(ele);
                  return allCertArray.push(val[0]);
                });
              }
            });
          }
        }
        if (result && result[1]?.data?.keys) {
          result[1].data.keys.map((item) => {
            if (item.certificateName) {
              return internalCertArray.push(item);
            }
            return null;
          });
          compareCertificates(internalCertArray, allCertArray, 'internal');
        }
        if (result && result[2]?.data?.keys) {
          result[2].data.keys.map((item) => {
            if (item.certificateName) {
              return externalCertArray.push(item);
            }
            return null;
          });
        }
        if (result && result[3].data) {
          result[3].data.map((ele) => {
            ele.isOnboardCert = true;
            return onboardCertArray.push(ele);
          });
        }
        setCertificateList([
          ...internalCertArray,
          ...externalCertArray,
          ...onboardCertArray,
        ]);
        setAllCertList([
          ...internalCertArray,
          ...externalCertArray,
          ...onboardCertArray,
        ]);
        setResponse({ status: 'success' });
      })
      .catch(() => {
        setResponse({ status: 'failed' });
      });
  }, []);

  const fetchNonAdminData = useCallback(async () => {
    setResponse({ status: 'loading' });
    let allCertInternal = [];
    let allCertExternal = [];
    if (configData.AUTH_TYPE === 'oidc') {
      allCertInternal = await apiService.getAllNonAdminCertInternal();
      allCertExternal = await apiService.getAllNonAdminCertExternal();
    }
    const internalCertificates = await apiService.getInternalCertificates();
    const externalCertificates = await apiService.getExternalCertificates();
    const allApiResponse = Promise.all([
      allCertInternal,
      allCertExternal,
      internalCertificates,
      externalCertificates,
    ]);
    allApiResponse
      .then((result) => {
        const allCertificateInternal = [];
        const allCertificateExternal = [];
        const internalCertArray = [];
        const externalCertArray = [];
        if (configData.AUTH_TYPE === 'oidc') {
          if (result && result[0]?.data?.cert) {
            result[0].data.cert.map((item) => {
              return Object.entries(item).map(([key, value]) => {
                if (value.toLowerCase() !== 'deny') {
                  return allCertificateInternal.push(key);
                }
                return null;
              });
            });
          }
          if (result && result[1]?.data?.externalcerts) {
            result[1].data.externalcerts.map((item) => {
              return Object.entries(item).map(([key]) => {
                return allCertificateExternal.push(key);
              });
            });
          }
        } else {
          const access = JSON.parse(localStorage.getItem('access'));
          if (Object.keys(access).length > 0) {
            Object.keys(access).forEach((item) => {
              if (item === 'cert' || item === 'externalcerts') {
                access[item].map((ele) => {
                  const val = Object.keys(ele);
                  if (item === 'cert') {
                    allCertificateInternal.push(val[0]);
                  } else {
                    allCertificateExternal.push(val[0]);
                  }
                  return null;
                });
              }
            });
          }
        }
        if (result && result[2]?.data?.keys) {
          result[2].data.keys.map((item) => {
            if (item.certificateName) {
              return internalCertArray.push(item);
            }
            return null;
          });
        }
        compareCertificates(
          internalCertArray,
          allCertificateInternal,
          'internal'
        );
        if (result && result[3]?.data?.keys) {
          result[3].data.keys.map((item) => {
            if (item.certificateName) {
              return externalCertArray.push(item);
            }
            return null;
          });
        }
        compareCertificates(
          externalCertArray,
          allCertificateExternal,
          'external'
        );
        setCertificateList([...internalCertArray, ...externalCertArray]);
        setAllCertList([...internalCertArray, ...externalCertArray]);
        setResponse({ status: 'success' });
      })
      .catch(() => {
        setResponse({ status: 'failed' });
      });
  }, []);

  /**
   * @description On component load call fetchData function.
   */
  useEffect(() => {
    setResponse({ status: 'loading' });
    if (admin) {
      fetchAdminData().catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMsg(err.response.data.errors[0]);
        }
        setResponse({ status: 'failed' });
      });
    } else {
      fetchNonAdminData().catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMsg(err.response.data.errors[0]);
        }
        setResponse({ status: 'failed' });
      });
    }
  }, [fetchAdminData, fetchNonAdminData, admin]);

  useEffect(() => {
    const internalArray = allCertList?.filter(
      (item) => item?.certType === 'internal'
    );
    const externalArray = allCertList?.filter(
      (item) => item?.certType === 'external'
    );
    setMenu([
      { name: 'All Certificates', count: allCertList?.length || 0 },
      { name: 'Internal Certificates', count: internalArray?.length || 0 },
      { name: 'External Certificates', count: externalArray?.length || 0 },
    ]);
  }, [allCertList]);

  /**
   * @function onLinkClicked
   * @description function to check if mobile screen the make certificateClicked true
   * based on that value display left and right side.
   */
  const onLinkClicked = () => {
    if (isMobileScreen) {
      setCertificateClicked(true);
    }
  };

  /**
   * @function backToCertificates
   * @description To get back to left side lists in case of mobile view
   * @param {bool} isMobileScreen boolian
   */
  const backToCertificates = () => {
    if (isMobileScreen) {
      setCertificateClicked(false);
    }
  };

  useEffect(() => {
    if (allCertList.length > 0) {
      const val = location.pathname.split('/');
      const certName = val[val.length - 1];
      if (
        certName !== 'create-ceritificate' &&
        certName !== 'edit-certificate'
      ) {
        const obj = allCertList.find(
          (cert) => cert.certificateName === certName
        );
        if (obj) {
          setListItemDetails({ ...obj });
        } else {
          setListItemDetails(allCertList[0]);
          history.push(`/certificates/${allCertList[0].certificateName}`);
        }
      }
    }
  }, [allCertList, location, history]);

  /**
   * @function onSelectChange
   * @description function to filter certificates.
   * @param {string} value selected filter value.
   */
  const onSelectChange = (value) => {
    setCertificateType(value);
    if (value !== 'All Certificates') {
      const filterArray = allCertList.filter((cert) =>
        value.toLowerCase().includes(cert.certType)
      );
      setCertificateList([...filterArray]);
    } else {
      setCertificateList([...allCertList]);
    }
  };

  /**
   * @function onSearchChange
   * @description function to search certificate.
   * @param {string} value searched input value.
   */
  const onSearchChange = (value) => {
    if (value !== '') {
      const searchArray = allCertList.filter((item) =>
        item.certificateName.includes(value)
      );
      setCertificateList([...searchArray]);
    } else {
      setCertificateList([...allCertList]);
    }
  };

  // when both search and filter value is available.
  useEffect(() => {
    if (certificateType !== 'All Certificates' && inputSearchValue) {
      const array = certificateList.filter((cert) =>
        cert.certificateName.includes(inputSearchValue)
      );
      setCertificateList([...array]);
    } else if (certificateType === 'All Certificates' && inputSearchValue) {
      onSearchChange(inputSearchValue);
    } else if (inputSearchValue === '') {
      onSelectChange(certificateType);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inputSearchValue, certificateType]);

  /**
   * @function onEditListItemClicked
   * @description function to open the edit modal.
   * @param {object} certificate certificate which is clicked.
   */
  const onEditListItemClicked = (certificate) => {
    history.push({
      pathname: '/certificates/edit-certificate',
      state: { certificate },
    });
  };

  /**
   * @function onCloseAllModal
   * @description function to close all modal and make api call to fetch
   * certificate, when edit or transfer or delete certificate happen.
   * @param {bool} actionPerform true/false based on the success event of corresponding action.
   */
  const onCloseAllModal = (actionPerform) => {
    setOpenTransferModal(false);
    setOpenReleaseModal(false);
    setOpenOnboardModal(false);
    setCertificateData({});
    if (actionPerform) {
      setResponse({ status: 'loading' });
      if (admin) {
        fetchAdminData();
      } else {
        fetchNonAdminData();
      }
    }
  };

  /**
   * @function onTransferOwnerClicked
   * @description function to open the transfer owner
   * @param {object} data .
   */
  const onTransferOwnerClicked = (data) => {
    setOpenTransferModal(true);
    setCertificateData(data);
  };

  /**
   * @function onDeleteCertificateClicked
   * @description function to open the delete modal.
   * @param {object} data .
   */
  const onDeleteCertificateClicked = (data) => {
    setCertificateData(data);
    setOpenDeleteConfirmation(true);
    setDeleteModalDetail({
      title: 'Confirmation',
      description: 'Are you sure you want to delete this certificate?',
    });
  };

  /**
   * @function handleDeleteConfirmationModalClose
   * @description function to close the delete modal and if
   * deletion completed successfully the call the api to fetch all certificates.
   */
  const handleDeleteConfirmationModalClose = () => {
    setDeleteResponse(false);
    setOpenDeleteConfirmation(false);
    if (!deleteError && deleteConfirmClicked) {
      setDeleteConfirmClicked(false);
      onCloseAllModal(true);
    }
  };

  /**
   * @function onCertificateDeleteConfirm
   * @description function to perform the delete of certificate.
   */
  const onCertificateDeleteConfirm = () => {
    setResponse({ status: 'loading' });
    setOpenDeleteConfirmation(false);
    setDeleteConfirmClicked(true);
    apiService
      .deleteCertificate(
        certificateData.certificateName,
        `${certificateData.certType}`
      )
      .then((res) => {
        if (res?.data?.messages && res.data.messages[0]) {
          setDeleteModalDetail({
            title: 'Successfull',
            description: res.data.messages[0],
          });
        }
        setOpenDeleteConfirmation(true);
        setResponse({ status: 'success' });
        setDeleteError(false);
        setDeleteResponse(true);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setDeleteModalDetail({
            title: 'Error',
            description: err.response.data.errors[0],
          });
        }
        setDeleteError(true);
        setOpenDeleteConfirmation(true);
        setResponse({ status: 'success' });
        setDeleteResponse(true);
      });
  };

  /**
   * @function onReleaseClicked
   * @description function to open released modal when released certificate is clicked.
   */

  const onReleaseClicked = (data) => {
    setOpenReleaseModal(true);
    setCertificateData(data);
  };

  /**
   * @function onReleaseSubmitClicked
   * @description function to call an api when release submit is clicked
   */
  const onReleaseSubmitClicked = (data) => {
    setResponse({ status: 'loading' });
    setOpenReleaseModal(false);
    apiService
      .onReleasecertificate(data.name, data.type, data.reason)
      .then(() => {
        setResponseType(1);
        onCloseAllModal(true);
        setToastMessage('Certificate released successfully!');
      })
      .catch((e) => {
        if (e?.response?.data?.errors && e?.response?.data?.errors[0]) {
          setToastMessage(e.response.data.errors[0]);
        }
        setResponseType(-1);
        setResponse({ status: 'success' });
      });
  };

  /**
   * @function onOnboardClicked
   * @description function to open released modal when released certificate is clicked.
   */

  const onOnboardClicked = (data) => {
    setOpenOnboardModal(true);
    setCertificateData(data);
  };

  /**
   * @function onOboardCertClicked
   * @description function to call an api when onboard submit is clicked
   */
  const onOboardCertClicked = (data) => {
    setResponse({ status: 'loading' });
    setOpenOnboardModal(false);
    apiService
      .onOnboardcertificate(data)
      .then(() => {
        setResponseType(1);
        onCloseAllModal(true);
        setToastMessage('SSL certificate onboarded successfully!');
      })
      .catch((e) => {
        if (e?.response?.data?.errors && e?.response?.data?.errors[0]) {
          setToastMessage(e.response.data.errors[0]);
        }
        setResponseType(-1);
        setResponse({ status: 'success' });
      });
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  const renderList = () => {
    return (
      <LeftColumn
        onLinkClicked={(cert) => onLinkClicked(cert)}
        onEditListItemClicked={(cert) => onEditListItemClicked(cert)}
        onDeleteCertificateClicked={(cert) => onDeleteCertificateClicked(cert)}
        onTransferOwnerClicked={(cert) => onTransferOwnerClicked(cert)}
        onReleaseClicked={(cert) => onReleaseClicked(cert)}
        onOnboardClicked={(cert) => onOnboardClicked(cert)}
        isTabAndMobileScreen={isTabAndMobileScreen}
        history={history}
        certificateList={certificateList}
      />
    );
  };
  return (
    <ComponentError>
      <>
        <SectionPreview title="certificates-section">
          {openTransferModal && (
            <TransferCertificate
              certificateData={certificateData}
              open={openTransferModal}
              onCloseModal={(action) => onCloseAllModal(action)}
            />
          )}
          {openReleaseModal && (
            <CertificateRelease
              certificateData={certificateData}
              open={openReleaseModal}
              onCloseModal={(action) => onCloseAllModal(action)}
              onReleaseSubmitClicked={(data) => onReleaseSubmitClicked(data)}
            />
          )}
          {openDeleteConfirmation && (
            <DeletionConfirmationModal
              openDeleteConfirmation={openDeleteConfirmation}
              handleDeleteConfirmationModalClose={
                handleDeleteConfirmationModalClose
              }
              onCertificateDeleteConfirm={onCertificateDeleteConfirm}
              deleteResponse={deleteResponse}
              deleteModalDetail={deleteModalDetail}
            />
          )}
          {openOnboardModal && (
            <OnboardCertificates
              certificateData={certificateData}
              open={openOnboardModal}
              onCloseModal={(action) => onCloseAllModal(action)}
              onOboardCertClicked={(data) => onOboardCertClicked(data)}
            />
          )}
          <LeftColumnSection>
            <ColumnHeader>
              <SelectWithCountComponent
                menu={menu}
                value={certificateType}
                color="secondary"
                classes={classes}
                fullWidth={false}
                onChange={(e) => onSelectChange(e.target.value)}
              />
              <SearchWrap>
                <TextFieldComponent
                  placeholder="Search"
                  icon="search"
                  fullWidth
                  onChange={(e) => setInputSearchValue(e.target.value)}
                  value={inputSearchValue || ''}
                  color="secondary"
                />
              </SearchWrap>
            </ColumnHeader>
            {response.status === 'loading' && (
              <ScaledLoader contentHeight="80%" contentWidth="100%" />
            )}
            {response.status === 'failed' && (
              <EmptyContentBox>
                <Error
                  description={errorMsg || 'Error while fetching certificates!'}
                />
              </EmptyContentBox>
            )}
            {response.status === 'success' && (
              <>
                {certificateList?.length > 0 && (
                  <ListContainer>
                    <ListContent>{renderList()}</ListContent>
                  </ListContainer>
                )}
                {certificateList?.length === 0 && (
                  <>
                    {inputSearchValue ? (
                      <NoDataWrapper>
                        <SearchFilterNotAvailable>
                          No certificate found with name
                          <span>{inputSearchValue}</span>
                          {certificateType !== 'All Certificates' && (
                            <>
                              and filter by
                              <span>{certificateType}</span>
                            </>
                          )}
                          {' . '}
                        </SearchFilterNotAvailable>
                      </NoDataWrapper>
                    ) : (
                      <NoDataWrapper>
                        <NoListWrap>
                          <NoData
                            imageSrc={noCertificateIcon}
                            actionButton={
                              <FloatingActionButtonComponent
                                href="/certificates/create-ceritificate"
                                color="secondary"
                                icon="add"
                                tooltipTitle="Create New Certificate"
                                tooltipPos="bottom"
                              />
                            }
                          />
                        </NoListWrap>
                      </NoDataWrapper>
                    )}
                  </>
                )}
              </>
            )}
            {certificateList.length > 0 && (
              <FloatBtnWrapper>
                <FloatingActionButtonComponent
                  href="/certificates/create-ceritificate"
                  color="secondary"
                  icon="add"
                  tooltipTitle="Create New Certificate"
                  tooltipPos="left"
                />
              </FloatBtnWrapper>
            )}
          </LeftColumnSection>
          <RightColumnSection
            mobileViewStyles={isMobileScreen ? MobileViewForListDetailPage : ''}
            isDetailsOpen={certificateClicked}
          >
            <Switch>
              {certificateList[0]?.certificateName && (
                <Redirect
                  exact
                  from="/certificates"
                  to={{
                    pathname: `/certificates/${certificateList[0]?.certificateName}`,
                    state: { data: certificateList[0] },
                  }}
                />
              )}
              <Route
                path="/certificates/:certificateName"
                render={() => (
                  <CertificateItemDetail
                    backToLists={backToCertificates}
                    ListDetailHeaderBg={
                      isTabScreen
                        ? sectionTabHeaderBg
                        : isMobileScreen
                        ? sectionMobHeaderBg
                        : sectionHeaderBg
                    }
                    name={ListItemDetails.certificateName}
                    renderContent={
                      <CertificatesReviewDetails
                        certificateDetail={ListItemDetails}
                      />
                    }
                  />
                )}
              />
              <Route
                path="/certificates"
                render={() => (
                  <CertificateItemDetail
                    ListDetailHeaderBg={
                      isTabScreen
                        ? sectionTabHeaderBg
                        : isMobileScreen
                        ? sectionMobHeaderBg
                        : sectionHeaderBg
                    }
                    owner={ListItemDetails.certOwnerEmailId}
                    container={ListItemDetails.containerName}
                    renderContent={
                      <CertificatesReviewDetails
                        certificateList={certificateList}
                      />
                    }
                  />
                )}
              />
            </Switch>
          </RightColumnSection>
          <Switch>
            <Route
              exact
              path="/certificates/create-ceritificate"
              render={() => (
                <CreateCertificates
                  refresh={() =>
                    admin ? fetchAdminData() : fetchNonAdminData()
                  }
                />
              )}
            />
            <Route
              exact
              path="/certificates/edit-certificate"
              render={() => (
                <EditCertificate
                  refresh={(status) => onCloseAllModal(status)}
                />
              )}
            />
          </Switch>
        </SectionPreview>
        {responseType === -1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message={toastMessage || 'Something went wrong!'}
          />
        )}
        {responseType === 1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message={toastMessage}
          />
        )}
      </>
    </ComponentError>
  );
};

export default CertificatesDashboard;
