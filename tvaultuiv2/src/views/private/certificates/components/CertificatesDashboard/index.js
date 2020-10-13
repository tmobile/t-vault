/* eslint-disable react/jsx-wrap-multilines */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import { Link, Route, Switch, useHistory, Redirect } from 'react-router-dom';
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
import ListItem from '../../../../../components/ListItem';
import { certificates } from '../../__mock/certificates';
import CertificateItemDetail from '../CertificateItemDetail';
import { TitleFour } from '../../../../../styles/GlobalStyles';

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
    display: ${(props) => (props.isAccountDetailsOpen ? 'block' : 'none')};
  }
`;
const LeftColumnSection = styled(ColumnSection)`
  width: 40.77%;
  ${mediaBreakpoints.small} {
    display: ${(props) => (props.isAccountDetailsOpen ? 'none' : 'block')};
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
`;

const PopperWrap = styled.div`
  position: absolute;
  right: 4%;
  z-index: 1;
  width: 18rem;
  display: none;
`;

const ListFolderWrap = styled(Link)`
  position: relative;
  display: flex;
  text-decoration: none;
  align-items: center;
  padding: 1.2rem 1.8rem 1.2rem 3.4rem;
  cursor: pointer;
  background-image: ${(props) =>
    props.active ? props.theme.gradients.list : 'none'};
  color: ${(props) => (props.active ? '#fff' : '#4a4a4a')};
  ${mediaBreakpoints.belowLarge} {
    padding: 2rem 1.1rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
    color: #fff;
    ${PopperWrap} {
      display: block;
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

const CertificateStatus = styled.div`
  display: flex;
  align-items: center;
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

const useStyles = makeStyles(() => ({
  contained: { borderRadius: '0.4rem' },
  select: {
    backgroundColor: 'transparent',
    fontSize: '1.6rem',
    textTransform: 'uppercase',
    color: '#fff',
    fontWeight: 'bold',
    width: '22rem',
    marginRight: '2.5rem',
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
  const [certificateClicked, setCertificateClicked] = useState(false);
  const [ListItemDetails, setListItemDetails] = useState({});
  const classes = useStyles();
  const history = useHistory();
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  /**
   * @function fetchData
   * @description function call all certificates api.
   */
  const fetchData = useCallback(async () => {
    setCertificateList([...certificates]);
  }, []);

  /**
   * @description On component load call fetchData function.
   */
  useEffect(() => {
    fetchData().catch(() => {
      setResponse({ status: 'failed', message: 'failed' });
    });
  }, [fetchData]);

  /**
   * @function onSearchChange
   * @description function to search input
   */
  const onSearchChange = (value) => {
    setInputSearchValue(value);
  };

  /**
   * @function onLinkClicked
   * @description function to check if mobile screen the make safeClicked true
   * based on that value display left and right side.
   */
  const onLinkClicked = (item) => {
    setListItemDetails(item);
    if (isMobileScreen) {
      setCertificateClicked(true);
    }
  };

  /**
   * @function backToServiceAccounts
   * @description To get back to left side lists in case of mobile view
   * @param {bool} isMobileScreen boolian
   */
  const backToCertificates = () => {
    if (isMobileScreen) {
      setCertificateClicked(false);
    }
  };

  useEffect(() => {
    if (certificateList.length > 0) {
      setListItemDetails(certificateList[0]);
    }
  }, [certificateList]);

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
        }
      >
        <ListItem
          title={certificate.certificateName}
          subTitle2={certificate.certType}
          subTitle="26/10/10202"
          icon={certIcon}
          showActions={false}
        />
        <BorderLine />
        <CertificateStatus>
          <TitleFour extraCss={extraCss}>
            {certificate.certificateStatus}
          </TitleFour>
          <StatusIcon status={certificate.certificateStatus} />
        </CertificateStatus>
      </ListFolderWrap>
    ));
  };
  return (
    <ComponentError>
      <>
        <SectionPreview title="certificates-section">
          <LeftColumnSection>
            <ColumnHeader>
              <SelectComponent
                menu={menu}
                value={certificateType}
                color="secondary"
                classes={classes}
                fullWidth={false}
                onChange={(e) => setCertificateType(e.target.value)}
              />
              <SearchWrap>
                <TextFieldComponent
                  placeholder="Search"
                  icon="search"
                  fullWidth
                  onChange={(e) => onSearchChange(e.target.value)}
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
                <Error description="Error while fetching certificates!" />
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
            isAccountDetailsOpen={certificateClicked}
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
