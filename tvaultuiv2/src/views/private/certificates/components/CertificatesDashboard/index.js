/* eslint-disable no-param-reassign */
/* eslint-disable no-nested-ternary */
/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import { Route, Switch, useHistory, Redirect } from 'react-router-dom';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import { debounce } from 'lodash';
import sectionHeaderBg from '../../../../../assets/certificate-banner.svg';
import sectionMobHeaderBg from '../../../../../assets/mob-certbg.png';
import sectionTabHeaderBg from '../../../../../assets/tab-certbg.png';
import mediaBreakpoints from '../../../../../breakpoints';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import noCertificateIcon from '../../../../../assets/nocertificate.svg';
import FloatingActionButtonComponent from '../../../../../components/FormFields/FloatingActionButton';
import Error from '../../../../../components/Error';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import CertificatesReviewDetails from '../CertificatesReviewDetails';
import CertificateItemDetail from '../CertificateItemDetail';
import apiService from '../../apiService';
import EditCertificate from '../EditCertificate';
import TransferCertificate from '../TransferCertificateOwner';
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
import DeletionConfirmationModal from './components/DeletionConfirmationModal';
import SuccessAndErrorModal from '../../../../../components/SuccessAndErrorModal';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import SearchboxWithDropdown from '../../../../../components/FormFields/SearchboxWithDropdown';

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

const customStyle = css`
  justify-content: center;
`;

const ScaledLoaderContainer = styled.div`
  height: 5rem;
  display: flex;
  align-items: center;
`;

const scaledLoaderFirstChild = css`
  width: 1.5rem;
  height: 1.5rem;
`;

const scaledLoaderLastChild = css`
  width: 3rem;
  height: 3rem;
  left: -1.7rem;
  top: -0.3rem;
`;

const customLoaderStyle = css`
  position: absolute;
  right: 1.2rem;
  top: 1.6rem;
  color: red;
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
  const [certificateType, setCertificateType] = useState(
    'Internal Certificates'
  );
  const [menu, setMenu] = useState([]);
  const [response, setResponse] = useState({ status: 'success' });
  const [errorMsg, setErrorMsg] = useState('');
  const [allCertList, setAllCertList] = useState([]);
  const [certificateClicked, setCertificateClicked] = useState(false);
  const [listItemDetails, setListItemDetails] = useState({});
  const [certificateData, setCertificateData] = useState({});
  const [openTransferModal, setOpenTransferModal] = useState(false);
  const [openReleaseModal, setOpenReleaseModal] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [openOnboardModal, setOpenOnboardModal] = useState(false);
  const [openDeleteConfirmation, setOpenDeleteConfirmation] = useState(false);
  const [successErrorModal, setSuccessErrorModal] = useState(false);
  const [successErrorDetails, setSuccessErrorDetails] = useState({
    title: '',
    desc: '',
  });
  const classes = useStyles();
  const history = useHistory();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const isTabAndMobileScreen = useMediaQuery(mediaBreakpoints.smallAndMedium);
  const isTabScreen = useMediaQuery(mediaBreakpoints.medium);
  const [state] = useStateValue();
  const admin = Boolean(state?.isAdmin);
  const limit = 20;
  const [hasMore, setHasMore] = useState(false);
  const [offset, setOffset] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [allCertificates, setAllCertificates] = useState([]);
  const [dataCleared, setDataCleared] = useState(true);
  const [searchCertList, setSearchCertList] = useState([]);
  const [noResultFound, setNoResultFound] = useState('');
  const [searchLoader, setSearchLoader] = useState(false);
  const [searchSelected, setSearchSelected] = useState([]);

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

  const fetchAdminInternalData = useCallback(async () => {
    apiService
      .getInternalCertificates(limit, offset)
      .then((result) => {
        setOffset(offset + limit);
        const internalCertArray = [];
        if (result && result?.data?.keys) {
          if (result?.data?.next === '-1') {
            setHasMore(false);
          } else {
            setHasMore(true);
          }
          result.data.keys.map((item) => {
            if (item.certificateName) {
              return internalCertArray.push(item);
            }
            return null;
          });
        }
        const finalList = [...allCertList, ...internalCertArray];
        setCertificateList([...finalList]);
        setAllCertList([...finalList]);
        setResponse({ status: 'success' });
        setIsLoading(false);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMsg(err.response.data.errors[0]);
        }
        setResponse({ status: 'failed' });
      });
  }, [offset, allCertList]);

  const fetchAdminExternalData = useCallback(async () => {
    apiService
      .getAllAdminCertExternal(limit, offset)
      .then((result) => {
        setOffset(offset + limit);
        const externalCertArray = [];
        if (result?.data?.keys) {
          if (result?.data?.next === '-1') {
            setHasMore(false);
          } else {
            setHasMore(true);
          }
          result.data.keys.map((item) => {
            return externalCertArray.push(item);
          });
        }
        setCertificateList([...allCertList, ...externalCertArray]);
        setAllCertList([...allCertList, ...externalCertArray]);
        setResponse({ status: 'success' });
        setIsLoading(false);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMsg(err.response.data.errors[0]);
        }
        setResponse({ status: 'failed' });
      });
  }, [offset, allCertList]);

  const fetchNonAdminInternalData = useCallback(async () => {
    let allCertInternal = [];
    if (configData.AUTH_TYPE === 'oidc' && offset === 0) {
      allCertInternal = apiService.getAllNonAdminCertInternal();
    }
    const internalCertificates = apiService.getInternalCertificates(
      limit,
      offset
    );
    const allApiResponse = Promise.all([allCertInternal, internalCertificates]);
    allApiResponse
      .then((result) => {
        const allCertificateInternal = [];
        const internalCertArray = [];
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
            setAllCertificates([...allCertificateInternal]);
          }
        } else {
          const access = JSON.parse(sessionStorage.getItem('access'));
          if (Object.keys(access).length > 0) {
            Object.keys(access).forEach((item) => {
              if (item === 'cert' || item === 'internalcerts') {
                access[item].map((ele) => {
                  const val = Object.keys(ele);
                  if (item === 'cert') {
                    allCertificateInternal.push(val[0]);
                  }
                  return null;
                });
              }
            });
          }
          setAllCertificates([...allCertificateInternal]);
        }
        if (result && result[1]?.data?.keys) {
          if (result[1]?.data?.next === '-1') {
            setHasMore(false);
          } else {
            setHasMore(true);
          }
          result[1].data.keys.map((item) => {
            if (item.certificateName) {
              return internalCertArray.push(item);
            }
            return null;
          });
        }
        const finalList = [...allCertList, ...internalCertArray];
        if (result[1]?.data?.next === '-1') {
          if (offset === 0) {
            compareCertificates(finalList, allCertificateInternal, 'internal');
          } else {
            compareCertificates(finalList, allCertificates, 'internal');
          }
        }
        setCertificateList([...finalList]);
        setAllCertList([...finalList]);
        setResponse({ status: 'success' });
        setOffset(offset + limit);
        setIsLoading(false);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMsg(err.response.data.errors[0]);
        }
        setResponse({ status: 'failed' });
      });
    // eslint-disable-next-line
  }, [offset,allCertList,certificateList]);

  const fetchNonAdminExternalData = useCallback(async () => {
    let allCertExternal = [];
    if (configData.AUTH_TYPE === 'oidc' && offset === 0) {
      allCertExternal = apiService.getAllNonAdminCertExternal();
    }
    const externalCertificates = apiService.getExternalCertificates(
      limit,
      offset
    );
    const allApiResponse = Promise.all([allCertExternal, externalCertificates]);
    allApiResponse
      .then((result) => {
        const allCertificateExternal = [];
        const externalCertArray = [];
        if (configData.AUTH_TYPE === 'oidc') {
          if (result && result[0]?.data?.externalcerts) {
            result[0].data.externalcerts.map((item) =>
              Object.entries(item).map(
                ([key]) =>
                  item[key] !== 'deny' && allCertificateExternal.push(key)
              )
            );
            setAllCertificates([...allCertificateExternal]);
          }
        } else {
          const access = JSON.parse(sessionStorage.getItem('access'));
          if (Object.keys(access).length > 0) {
            Object.keys(access).forEach((item) => {
              if (item === 'cert' || item === 'externalcerts') {
                access[item].map((ele) => {
                  const val = Object.keys(ele);
                  if (item === 'externalcerts') {
                    allCertificateExternal.push(val[0]);
                  }
                  return null;
                });
              }
            });
          }
          setAllCertificates([...allCertificateExternal]);
        }
        if (result && result[1]?.data?.keys) {
          result[1].data.keys.map((item) => {
            if (item.certificateName) {
              return externalCertArray.push(item);
            }
            return null;
          });
        }
        const finalList = [...allCertList, ...externalCertArray];
        if (
          result[1]?.data?.next === '-1' ||
          result[1]?.data?.next === undefined
        ) {
          if (offset === 0) {
            compareCertificates(finalList, allCertificateExternal, 'external');
          } else {
            compareCertificates(finalList, allCertificates, 'external');
          }
        }
        setCertificateList([...finalList]);
        setAllCertList([...finalList]);
        setResponse({ status: 'success' });
        setOffset(offset + limit);
        setIsLoading(false);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMsg(err.response.data.errors[0]);
        }
        setResponse({ status: 'failed' });
      });
    // eslint-disable-next-line
  }, []);

  const fetchOnboardCertificates = useCallback(async () => {
    const oldCert = [...allCertList];
    apiService
      .getOnboardCertificates(limit, offset)
      .then((result) => {
        setOffset(offset + limit);
        const onboardCertArray = [];
        if (result?.data?.keys) {
          if (result.data.next === -1) {
            setHasMore(false);
          } else {
            setHasMore(true);
          }
          result.data.keys.map((ele) => {
            ele.isOnboardCert = true;
            return onboardCertArray.push(ele);
          });
        }
        setCertificateList([...oldCert, ...onboardCertArray]);
        setAllCertList([...oldCert, ...onboardCertArray]);
        setResponse({ status: 'success' });
        setIsLoading(false);
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMsg(err.response.data.errors[0]);
        }
        setResponse({ status: 'failed' });
      });
    // eslint-disable-next-line
  }, []);

  const fetchInternalCertificates = () => {
    if (offset === 0) {
      setResponse({ status: 'loading' });
    }
    if (admin) {
      fetchAdminInternalData();
    } else {
      fetchNonAdminInternalData();
    }
  };

  const fetchExternalCertificates = () => {
    if (offset === 0) {
      setResponse({ status: 'loading' });
    }
    if (admin) {
      fetchAdminExternalData();
    } else {
      fetchNonAdminExternalData();
    }
  };

  const loadTypeSpecificData = (type) => {
    setDataCleared(false);
    if (type === 'Internal Certificates') {
      fetchInternalCertificates();
    } else if (type === 'External Certificates') {
      fetchExternalCertificates();
    } else if (type === 'Onboard Certificates') {
      fetchOnboardCertificates();
    }
  };

  const clearDataAndLoad = () => {
    setOffset(0);
    setHasMore(false);
    setCertificateList([]);
    setAllCertList([]);
    setAllCertificates([]);
    setSearchSelected([]);
    setInputSearchValue('');
    setNoResultFound('');
    setResponse({ status: 'loading' });
    setDataCleared(true);
  };

  useEffect(() => {
    if (dataCleared === true) {
      loadTypeSpecificData(certificateType);
      setDataCleared(false);
    }
    // eslint-disable-next-line
  },[dataCleared])

  useEffect(() => {
    const url = history?.location?.pathname?.split('/');
    if (
      allCertList.length > 0 &&
      url[1] === 'certificates' &&
      searchSelected.length === 0
    ) {
      setListItemDetails(allCertList[0]);
      history.push(`/certificates/${allCertList[0]?.certificateName}`);
    } else {
      setListItemDetails({});
    }
    // eslint-disable-next-line
  }, [allCertList, history]);

  useEffect(() => {
    const array = [
      { name: 'Internal Certificates' },
      { name: 'External Certificates' },
    ];
    if (admin) {
      array.push({
        name: 'Onboard Certificates',
      });
    }
    setMenu([...array]);
  }, [certificateList, admin, allCertList]);

  /**
   * @function onLinkClicked
   * @description function to check if mobile screen the make certificateClicked true
   * based on that value display left and right side.
   */
  const onLinkClicked = (cert) => {
    setListItemDetails(cert);
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

  /**
   * @function onSelectChange
   * @description function to filter certificates.
   * @param {string} value selected filter value.
   */
  const onSelectChange = (value) => {
    setCertificateType(value);
    clearDataAndLoad();
  };

  const searchAllcertApi = useCallback(
    debounce((searchText) => {
      const allSearchCerts = [];
      apiService.searchAllCert(searchText).then((res) => {
        if (res && res?.data) {
          res.data.internal.map((item) =>
            allSearchCerts.push({
              name: item,
              type: 'internal',
            })
          );
          res.data.external.map((item) =>
            allSearchCerts.push({
              name: item,
              type: 'external',
            })
          );
        }

        if (allSearchCerts.length === 0) {
          setNoResultFound('No records found');
        } else {
          setNoResultFound('');
        }
        setSearchCertList([...allSearchCerts]);
        setSearchLoader(false);
      });
    }, 1000),
    []
  );

  /**
   * @function onSearchChange
   * @description function to search certificate.
   * @param {string} value searched input value.
   */
  const onSearchChange = (value) => {
    if (value?.length > 2) {
      setSearchLoader(true);
      setDataCleared(false);
      searchAllcertApi(value);
    } else {
      setSearchCertList([]);
      setNoResultFound('');
    }
    if (inputSearchValue === '' && !dataCleared) {
      clearDataAndLoad();
    }
  };

  useEffect(() => {
    onSearchChange(inputSearchValue);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inputSearchValue]);

  const fetchCertificateDetail = (certType, certName) => {
    const url = `/sslcert?certificateName=${certName}&certType=${certType}`;
    apiService
      .getCertificateDetail(url)
      .then((res) => {
        if (res?.data?.keys[0]) {
          setSearchSelected([res?.data?.keys[0]]);
        } else {
          setSearchSelected([{ certificateName: certName, certType }]);
        }
        setResponse({ status: 'success' });
      })
      .catch(() => {
        setSearchSelected([{ certificateName: certName, certType }]);
        setResponse({ status: 'success' });
      });
  };

  const onSearchItemSelected = (v) => {
    setResponse({ status: 'loading' });
    fetchCertificateDetail(v.type, v.name);
    if (v.type === 'internal') {
      setCertificateType('Internal Certificates');
    } else if (v.type === 'external') {
      setCertificateType('External Certificates');
    }
    setSearchCertList([]);
  };

  useEffect(() => {
    if (searchSelected.length === 1) {
      history.push(`/certificates/${searchSelected[0].certificateName}`);
      setListItemDetails(searchSelected[0]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchSelected]);

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
  const onCloseAllModal = async (actionPerform) => {
    setOpenTransferModal(false);
    setOpenReleaseModal(false);
    setOpenOnboardModal(false);
    setCertificateData({});
    if (actionPerform) {
      clearDataAndLoad();
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
      .then((res) => {
        if (res?.data?.messages && res?.data?.messages[0]) {
          setSuccessErrorDetails({
            title: 'Certificate Status!',
            desc: res.data.messages[0],
          });
        } else {
          setSuccessErrorDetails({
            title: 'Certificate Status!',
            desc: 'Certificate released successfully!',
          });
        }
        setSuccessErrorModal(true);
        onCloseAllModal(true);
      })
      .catch((e) => {
        setSuccessErrorModal(true);
        if (e?.response?.data?.errors && e?.response?.data?.errors[0]) {
          setSuccessErrorDetails({
            title: 'Certificate Release Failed!',
            desc: e.response.data.errors[0],
          });
        } else {
          setSuccessErrorDetails({
            title: 'Certificate Release Failed!',
            desc: 'Something went wrong!',
          });
        }
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

  /**
   * @function onDeleteCertificateClicked
   * @description function to open the delete modal.
   * @param {object} data .
   */
  const onDeleteCertificateClicked = (data) => {
    setCertificateData(data);
    setOpenDeleteConfirmation(true);
  };

  /**
   * @function handleDeleteConfirmationModalClose
   * @description function to close the delete modal.
   */
  const handleDeleteConfirmationModalClose = () => {
    setOpenDeleteConfirmation(false);
  };

  /**
   * @function onCertificateDeleteConfirm
   * @description function to perform the delete of certificate.
   */
  const onCertificateDeleteConfirm = () => {
    setResponse({ status: 'loading' });
    setOpenDeleteConfirmation(false);
    apiService
      .deleteCertificate(
        certificateData.certificateName,
        `${certificateData.certType}`
      )
      .then((res) => {
        if (res?.data?.messages && res?.data?.messages[0]) {
          setSuccessErrorDetails({
            title: 'Certificate Deletion Successful!',
            desc: res.data.messages[0],
          });
        } else {
          setSuccessErrorDetails({
            title: 'Certificate Deletion Successful!',
            desc: 'Certificate deleted successfully',
          });
        }
        setSuccessErrorModal(true);
        onCloseAllModal(true);
      })
      .catch((e) => {
        setSuccessErrorModal(true);
        if (e?.response?.data?.errors && e?.response?.data?.errors[0]) {
          setSuccessErrorDetails({
            title: 'Certificate Deletion Failed!',
            desc: e.response.data.errors[0],
          });
        } else {
          setSuccessErrorDetails({
            title: 'Certificate Deletion Failed!',
            desc: 'Something went wrong!',
          });
        }
        setResponse({ status: 'success' });
      });
  };

  const handleSuccessAndDeleteModalClose = () => {
    setSuccessErrorModal(false);
    setSuccessErrorDetails({ title: '', desc: '' });
  };

  const renderList = () => {
    return (
      <LeftColumn
        onLinkClicked={(cert) => onLinkClicked(cert)}
        onEditListItemClicked={(cert) => onEditListItemClicked(cert)}
        onTransferOwnerClicked={(cert) => onTransferOwnerClicked(cert)}
        onReleaseClicked={(cert) => onReleaseClicked(cert)}
        onOnboardClicked={(cert) => onOnboardClicked(cert)}
        onDeleteCertificateClicked={(cert) => onDeleteCertificateClicked(cert)}
        isTabAndMobileScreen={isTabAndMobileScreen}
        history={history}
        certificateList={
          searchSelected.length === 1 ? searchSelected : certificateList
        }
        isLoading={isLoading}
      />
    );
  };

  const loadMoreData = () => {
    setIsLoading(true);
    if (certificateType === 'Internal Certificates') {
      fetchInternalCertificates();
    }
    if (certificateType === 'External Certificates') {
      fetchExternalCertificates();
    }
  };

  const handleListScroll = () => {
    const element = document.getElementById('scrollList');
    if (
      element.scrollHeight - element.offsetHeight - 250 < element.scrollTop &&
      !isLoading &&
      hasMore
    ) {
      loadMoreData();
    }
  };

  return (
    <ComponentError>
      <>
        <SectionPreview>
          {openTransferModal && (
            <TransferCertificate
              certificateData={certificateData}
              open={openTransferModal}
              onCloseModal={(action) => onCloseAllModal(action)}
            />
          )}
          {openDeleteConfirmation && (
            <DeletionConfirmationModal
              certificateData={certificateData}
              openDeleteConfirmation={openDeleteConfirmation}
              handleDeleteConfirmationModalClose={
                handleDeleteConfirmationModalClose
              }
              onCertificateDeleteConfirm={onCertificateDeleteConfirm}
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
          {openOnboardModal && (
            <OnboardCertificates
              certificateData={certificateData}
              open={openOnboardModal}
              onCloseModal={(action) => onCloseAllModal(action)}
              onOboardCertClicked={(data) => onOboardCertClicked(data)}
            />
          )}
          {successErrorModal && (
            <SuccessAndErrorModal
              title={successErrorDetails.title}
              description={successErrorDetails.desc}
              handleSuccessAndDeleteModalClose={() =>
                handleSuccessAndDeleteModalClose()
              }
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
                <SearchboxWithDropdown
                  onSearchChange={(e) => setInputSearchValue(e?.target?.value)}
                  value={inputSearchValue || ''}
                  menu={searchCertList}
                  onChange={(value) => onSearchItemSelected(value)}
                  noResultFound={noResultFound}
                />
                {searchLoader && inputSearchValue?.length > 2 && (
                  <LoaderSpinner customStyle={customLoaderStyle} />
                )}
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
                    <ListContent
                      id="scrollList"
                      onScroll={() => {
                        handleListScroll();
                      }}
                    >
                      {renderList()}
                      {isLoading && searchSelected.length < 1 && (
                        <ScaledLoaderContainer>
                          <ScaledLoader
                            contentHeight="80%"
                            contentWidth="100%"
                            notAbsolute
                            scaledLoaderLastChild={scaledLoaderLastChild}
                            scaledLoaderFirstChild={scaledLoaderFirstChild}
                          />
                        </ScaledLoaderContainer>
                      )}
                    </ListContent>
                  </ListContainer>
                )}
                {certificateList?.length === 0 && (
                  <>
                    {inputSearchValue ? (
                      <NoDataWrapper>
                        <SearchFilterNotAvailable>
                          No certificate found with name
                          <span>{inputSearchValue}</span>
                        </SearchFilterNotAvailable>
                      </NoDataWrapper>
                    ) : (
                      <NoDataWrapper>
                        <NoListWrap>
                          <NoData
                            imageSrc={noCertificateIcon}
                            description="Create a certificate to get started!"
                            actionButton={
                              <FloatingActionButtonComponent
                                href="/certificates/create-ceritificate"
                                color="secondary"
                                icon="add"
                                tooltipTitle="Create New Certificate"
                                tooltipPos="bottom"
                              />
                            }
                            customStyle={customStyle}
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
                    pathname: `/certificates/${certificateList[0]?.certificateName.replace(
                      '*.',
                      ''
                    )}`,
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
                    name={listItemDetails.certificateName}
                    renderContent={
                      <CertificatesReviewDetails
                        certificateDetail={listItemDetails}
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
                    owner={listItemDetails.certOwnerEmailId}
                    container={listItemDetails.containerName}
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
                  refresh={() => {
                    clearDataAndLoad();
                  }}
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
            message={toastMessage || 'Successful!'}
          />
        )}
      </>
    </ComponentError>
  );
};

export default CertificatesDashboard;
