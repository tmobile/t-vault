import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { IconDeleteActive, IconEdit } from '../../../../../assets/SvgIcons';
import PopperElement from '../../../../../components/Popper';
import { customColor } from '../../../../../theme';

const FolderIconWrap = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
    :hover {
      background: ${(props) =>
        props.theme.customColor.hoverColor.list || '#151820'};
      border-radius: 50%;
    }
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
    background: ${customColor.magenta};
  }
`;

const EditDeletePopper = (props) => {
  const { onDeleteClicked, onEditClicked, setAnchorEl, anchorEl } = props;
  return (
    <div>
      <FolderIconWrap>
        <PopperElement
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          anchorEl={anchorEl}
          setAnchorEl={setAnchorEl}
        >
          <PopperItem onClick={onEditClicked}>
            <IconEdit />
            <span>Edit</span>
          </PopperItem>
          <PopperItem onClick={onDeleteClicked}>
            <IconDeleteActive />
            <span> Delete</span>
          </PopperItem>
        </PopperElement>
      </FolderIconWrap>
    </div>
  );
};

EditDeletePopper.propTypes = {
  onEditClicked: PropTypes.func.isRequired,
  onDeleteClicked: PropTypes.func.isRequired,
  anchorEl: PropTypes.func.isRequired,
  setAnchorEl: PropTypes.func.isRequired,
};
EditDeletePopper.defaultProps = {};

export default EditDeletePopper;
