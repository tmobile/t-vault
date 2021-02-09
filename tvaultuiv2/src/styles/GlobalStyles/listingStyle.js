import styled from 'styled-components';
import InfiniteScroll from 'react-infinite-scroller';

export const StyledInfiniteScroll = styled(InfiniteScroll)`
  width: 100%;
  overflow: auto;
  height: 100%;
`;
export const ListContent = styled.div`
  width: 100%;
  overflow: auto;
  height: 100%;
`;
export const ListContainer = styled.div`
  height: calc(100% - 12rem);
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

export const NoResultFound = styled.div`
  color: #5e627c;
  height: 61vh;
  display: flex;
  justify-content: center;
  align-items: center;
  div {
    margin: 0 0.3rem;
    color: #fff;
    font-weight: bold;
    text-transform: uppercase;
  }
`;
