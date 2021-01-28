/* eslint-disable react/jsx-props-no-spreading */
import React, { useState } from 'react';
import Popover from '@material-ui/core/Popover';
import styled from 'styled-components';
import PopupState, { bindTrigger, bindPopover } from 'material-ui-popup-state';
import { makeStyles } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import GetAppIcon from '@material-ui/icons/GetApp';
import { customColor } from '../../../../../../../theme';
import DownloadModal from '../DownloadModal';
import apiService from '../../../../apiService';

const FileDownload = require('js-file-download');

const PoperItemWrap = styled.div``;
const DownloadText = styled.div`
  color: #fff;
  cursor: pointer;
  font-size: 1.6rem;
  display: flex;
  align-items: center;
  svg {
    margin-left: 1rem;
    color: ${customColor.magenta};
  }
`;
const PopperItem = styled.div`
  padding: 0.5rem;
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
  cursor: pointer;
  span {
    margin-right: 0.75rem;
  }
  :hover {
    background: ${(props) => props.theme.gradients.list || 'none'};
  }
`;
const Download = (props) => {
  const { certificateMetaData, onDownloadChange } = props;
  const [typeOfDownload, setTypeOfDownload] = useState('');
  const [openDownloadModal, setOpenDownloadModal] = useState(false);

  const onPopperItemClicked = (val) => {
    setTypeOfDownload(val);
    setOpenDownloadModal(true);
  };

  const onCloseDownloadModal = () => {
    setOpenDownloadModal(false);
    setTypeOfDownload('');
  };

  const onPrivateDownloadClicked = (payload, type) => {
    onDownloadChange('loading', null);
    setOpenDownloadModal(false);
    apiService
      .onPrivateDownload(payload)
      .then((res) => {
        onDownloadChange('success', null);
        FileDownload(
          res.data,
          `${certificateMetaData.certificateName}.${type}`
        );
      })
      .catch(() => {
        onDownloadChange('success', -1);
      });
  };

  const onPemDerFormatClicked = (type) => {
    onDownloadChange('loading', null);
    setOpenDownloadModal(false);
    apiService
      .onDownloadCertificate(
        certificateMetaData.certificateName,
        type,
        `${certificateMetaData.certType}`
      )
      .then((res) => {
        onDownloadChange('success', null);
        FileDownload(
          res.data,
          `${certificateMetaData.certificateName}.${type}`
        );
      })
      .catch(() => {
        onDownloadChange('success', -1);
      });
  };

  const useStyles = makeStyles((theme) => ({
    paper: {
      padding: theme.spacing(1),
      color: theme.palette.common.white,
      backgroundColor: theme.customColor.hoverColor.list || '#151820',
    },
  }));

  const classes = useStyles();

  return (
    <div>
      <DownloadModal
        onCloseDownloadModal={onCloseDownloadModal}
        typeOfDownload={typeOfDownload}
        openDownloadModal={openDownloadModal}
        onPemDerFormatClicked={(type) => onPemDerFormatClicked(type)}
        certificateMetaData={certificateMetaData}
        onPrivateDownloadClicked={onPrivateDownloadClicked}
      />

      <PopupState variant="popover" popupId="demo-popup-popover">
        {(popupState) => (
          <div>
            <DownloadText {...bindTrigger(popupState)}>
              Download
              <GetAppIcon />
            </DownloadText>
            <Popover
              {...bindPopover(popupState)}
              anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'right',
              }}
              transformOrigin={{
                vertical: 'top',
                horizontal: 'right',
              }}
              classes={classes}
            >
              <PoperItemWrap>
                <PopperItem
                  onClick={() => {
                    onPopperItemClicked('private');
                    popupState.close();
                  }}
                >
                  <span>Download w/ Private Key</span>
                </PopperItem>
                <PopperItem
                  onClick={() => {
                    onPopperItemClicked('pem-der');
                    popupState.close();
                  }}
                >
                  <span>Download w/ PEM/DER Format</span>
                </PopperItem>
              </PoperItemWrap>
            </Popover>
          </div>
        )}
      </PopupState>
    </div>
  );
};

Download.propTypes = {
  certificateMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  onDownloadChange: PropTypes.func.isRequired,
};

export default Download;
