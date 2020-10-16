/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect, useCallback, useContext } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import {
  Link,
  Route,
  Switch,
  useHistory,
  Redirect,
  useLocation,
} from 'react-router-dom';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import sectionHeaderBg from '../../../../../assets/certificate-banner.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../components/NoData';
import certIcon from '../../../../../assets/cert-icon.svg';
import noCertificateIcon from '../../../../../assets/nocertificate.svg';
import FloatingActionButtonComponent from '../../../../../components/FormFields/FloatingActionButton';
import TextFieldComponent from '../../../../../components/FormFields/TextField';
import Error from '../../../../../components/Error';
import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';
import SelectComponent from '../../../../../components/FormFields/SelectFields';
import CertificatesReviewDetails from '../CertificatesReviewDetails';
import CertificateItemDetail from '../CertificateItemDetail';
import { TitleFour } from '../../../../../styles/GlobalStyles';
import { UserContext } from '../../../../../contexts';
import apiService from '../../apiService';
import CertificateListItem from '../CertificateListItem';
import EditAndDeletePopup from '../../../../../components/EditAndDeletePopup';
import EditCertificate from '../EditCertificate';

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
const ListContent = styled.div`
  width: 100%;
  max-height: 57vh;
  ${mediaBreakpoints.small} {
    max-height: 78vh;
  }
`;

const ListContainer = styled.div`
  overflow: auto;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const NoDataWrapper = styled.div`
  height: 61vh;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #5e627c;
  span {
    margin: 0 0.4rem;
    color: #fff;
    font-weight: bold;
    text-transform: uppercase;
  }
`;

const PopperWrap = styled.div`
  position: absolute;
  right: 4%;
  z-index: 1;
  max-width: 18rem;
  display: none;
`;

const CertificateStatus = styled.div`
  display: flex;
  align-items: center;
`;

const ListFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  text-decoration: none;
  align-items: center;
  padding: 1.2rem 1.8rem 1.2rem 3.4rem;
  cursor: pointer;
  background-image: ${(props) =>
    props.active === 'true' ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active === 'true' ? '#fff' : '#4a4a4a')};
  ${mediaBreakpoints.belowLarge} {
    padding: 2rem 1.1rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
    color: #fff;
    ${PopperWrap} {
      display: block;
    }
    ${CertificateStatus} {
      display: none;
    }
  }
`;

const NoListWrap = styled.div`
  width: 35%;
`;

const BorderLine = styled.div`
  border-bottom: 0.1rem solid #1d212c;
  width: 90%;
  position: absolute;
  bottom: 0;
`;
const FloatBtnWrapper = styled('div')`
  position: absolute;
  bottom: 2.8rem;
  right: 2.5rem;
`;

const SearchWrap = styled.div`
  width: 100%;
`;

const MobileViewForListDetailPage = css`
  position: fixed;
  display: flex;
  right: 0;
  left: 0;
  bottom: 0;
  top: 0;
  z-index: 1;
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

const StatusIcon = styled.span`
  width: 1.2rem;
  height: 1.2rem;
  border-radius: 50%;
  margin-top: 0.4rem;
  margin-left: 0.6rem;
  background-color: ${(props) =>
    // eslint-disable-next-line no-nested-ternary
    props.status === 'Active'
      ? '#347a37'
      : props.status === 'Revoked'
      ? '#9a8022'
      : '#939496'};
`;

const extraCss = css`
  color: #5e627c;
`;

const useStyles = makeStyles((theme) => ({
  contained: { borderRadius: '0.4rem' },
  select: {
    backgroundColor: 'transparent',
    fontSize: '1.6rem',
    textTransform: 'uppercase',
    color: '#fff',
    fontWeight: 'bold',
    maxWidth: '22rem',
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
  const [menu] = useState([
    'All Certificates',
    'External Certificates',
    'Internal Certificates',
  ]);
  const [response, setResponse] = useState({ status: 'success' });
  const [errorMsg, setErrorMsg] = useState('');
  const [allCertList, setAllCertList] = useState([]);
  const [certificateClicked, setCertificateClicked] = useState(false);
  const [ListItemDetails, setListItemDetails] = useState({});
  const [openEditModal, setOpenEditModal] = useState(false);
  const [certificateData, setCertificateData] = useState({});
  const classes = useStyles();
  const history = useHistory();
  const location = useLocation();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  const contextObj = useContext(UserContext);

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
    setAllCertList([]);
    setCertificateList([]);
    const allCertInternal = await apiService.getAllAdminCertInternal();
    const internalCertificates = await apiService.getInternalCertificates();
    const externalCertificates = await apiService.getExternalCertificates();
    const allApiResponse = Promise.all([
      allCertInternal,
      internalCertificates,
      externalCertificates,
    ]);
    allApiResponse
      .then((result) => {
        const allCertArray = [];
        const internalCertArray = [];
        const externalCertArray = [];
        if (result && result[0]?.data?.data?.keys) {
          result[0].data.data.keys.map((item) => {
            return allCertArray.push(item);
          });
        }
        if (result && result[1]?.data?.keys) {
          result[1].data.keys.map((item) => {
            return internalCertArray.push(item);
          });
          compareCertificates(internalCertArray, allCertArray, 'internal');
        }
        if (result && result[2]?.data?.keys) {
          result[2].data.keys.map((item) => {
            return externalCertArray.push(item);
          });
        }
        setCertificateList([...internalCertArray, ...externalCertArray]);
        setAllCertList([...internalCertArray, ...externalCertArray]);
        setResponse({ status: 'success' });
      })
      .catch(() => {
        setResponse({ status: 'failed' });
      });
  }, []);

  const fetchNonAdminData = useCallback(async () => {
    const allCertInternal = await apiService.getAllNonAdminCertInternal();
    const allCertExternal = await apiService.getAllNonAdminCertExternal();
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
        if (result && result[0]?.data?.cert) {
          result[0].data.cert.map((item) => {
            return Object.entries(item).map(([key]) => {
              return allCertificateInternal.push(key);
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
        if (result && result[2]?.data?.keys) {
          result[2].data.keys.map((item) => {
            return internalCertArray.push(item);
          });
        }
        compareCertificates(
          internalCertArray,
          allCertificateInternal,
          'internal'
        );
        if (result && result[3]?.data?.keys) {
          result[3].data.keys.map((item) => {
            return externalCertArray.push(item);
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
    if (contextObj && Object.keys(contextObj).length > 0) {
      if (contextObj.isAdmin) {
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
    }
  }, [fetchAdminData, contextObj, fetchNonAdminData]);

  /**
   * @function onLinkClicked
   * @description function to check if mobile screen the make certificateClicked true
   * based on that value display left and right side.
   */
  const onLinkClicked = (item) => {
    setListItemDetails(item);
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
      const obj = allCertList.find((cert) => cert.certificateName === certName);
      if (obj) {
        setListItemDetails({ ...obj });
      } else {
        setListItemDetails(allCertList[0]);
      }
    }
  }, [allCertList, location]);

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
   * @function onActionClicked
   * @description function to prevent default click.
   * @param {object} e event
   */
  const onActionClicked = (e) => {
    e.stopPropagation();
    e.preventDefault();
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

  const onEditListItemClicked = (item) => {
    setOpenEditModal(true);
    setCertificateData({ ...item });
  };

  const onCloseEditModal = (editActionPerform) => {
    setOpenEditModal(false);
    setCertificateData({});
    if (editActionPerform) {
      setResponse({ status: 'loading' });
      if (contextObj.isAdmin) {
        fetchAdminData();
      } else {
        fetchNonAdminData();
      }
    }
  };

  const renderList = () => {
    return certificateList.map((certificate) => (
      <ListFolderWrap
        key={certificate.certificateName}
        to={{
          pathname: `/certificates/${certificate.certificateName}`,
          state: { data: certificate },
        }}
        onClick={() => onLinkClicked(certificate)}
        active={
          history.location.pathname ===
          `/certificates/${certificate.certificateName}`
            ? 'true'
            : 'false'
        }
      >
        <CertificateListItem
          title={certificate.certificateName}
          certType={certificate.certType}
          createDate={
            certificate.createDate
              ? new Date(certificate.createDate).toLocaleDateString()
              : ''
          }
          icon={certIcon}
          showActions={false}
        />
        <BorderLine />
        {certificate.certificateStatus && (
          <CertificateStatus>
            <TitleFour extraCss={extraCss}>
              {certificate.certificateStatus}
            </TitleFour>
            <StatusIcon status={certificate.certificateStatus} />
          </CertificateStatus>
        )}
        {certificate.applicationName && !isMobileScreen ? (
          <PopperWrap onClick={(e) => onActionClicked(e)}>
            <EditAndDeletePopup
              onDeletListItemClicked={() => {}}
              onEditListItemClicked={() => onEditListItemClicked(certificate)}
              admin={contextObj.isAdmin}
            />
          </PopperWrap>
        ) : null}
      </ListFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        <SectionPreview title="certificates-section">
          {openEditModal && (
            <EditCertificate
              certificateData={certificateData}
              open={openEditModal}
              onCloseModal={(action) => onCloseEditModal(action)}
            />
          )}
          <LeftColumnSection>
            <ColumnHeader>
              <SelectComponent
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
                        No certificate found with name
                        <span>{inputSearchValue}</span>
                        {certificateType !== 'All Certificates' && (
                          <>
                            and filter by
                            <span>{certificateType}</span>
                          </>
                        )}
                        {' . '}
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
                    ListDetailHeaderBg={sectionHeaderBg}
                    owner={ListItemDetails.certOwnerEmailId}
                    container={ListItemDetails.containerName}
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
                    ListDetailHeaderBg={sectionHeaderBg}
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
        </SectionPreview>
      </>
    </ComponentError>
  );
};
CertificatesDashboard.propTypes = {};
CertificatesDashboard.defaultProps = {};

export default CertificatesDashboard;
